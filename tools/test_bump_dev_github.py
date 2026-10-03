"""Check release validation and GitHub API mutation boundaries without network access."""

import base64
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap
import unittest
from unittest.mock import call, patch

import bump_dev_github


ENDPOINT = "repos/fixture/project/contents/version.properties"


def document(content="versionName=1.0.2\nversionCode=4\n", sha="original-blob-sha"):
    return {
        "type": "file",
        "encoding": "base64",
        "content": base64.b64encode(content.encode()).decode(),
        "sha": sha,
    }


class BumpDevelopmentGitHubTest(unittest.TestCase):
    def test_only_dev_version_file_is_updated_using_its_original_sha(self):
        with patch.object(bump_dev_github, "github_api", side_effect=[document(), document(), {}]) as api:
            result = bump_dev_github.advance_development_version("fixture/project", "release/1.0")
        self.assertEqual(("1.1.0", 5), result)
        self.assertEqual(call(f"{ENDPOINT}?ref=release%2F1.0"), api.call_args_list[0])
        self.assertEqual(call(f"{ENDPOINT}?ref=dev"), api.call_args_list[1])
        self.assertEqual(3, api.call_count)
        endpoint, payload = api.call_args_list[2].args
        self.assertEqual(ENDPOINT, endpoint)
        self.assertEqual("dev", payload["branch"])
        self.assertEqual("original-blob-sha", payload["sha"])
        self.assertEqual(b"versionName=1.1.0\nversionCode=5\n", base64.b64decode(payload["content"]))
        self.assertEqual("chore: advance dev version after release/1.0", payload["message"])

    def test_reruns_and_newer_dev_versions_never_write(self):
        for version in ("1.1.0", "1.3.7"):
            with self.subTest(version=version):
                current = document(f"versionName={version}\nversionCode=5\n")
                with patch.object(bump_dev_github, "github_api", side_effect=[document(), current]) as api:
                    self.assertIsNone(bump_dev_github.advance_development_version("fixture/project", "release/1.0"))
                self.assertEqual(2, api.call_count)
                self.assertTrue(all(len(request.args) == 1 for request in api.call_args_list))

    def test_invalid_branch_is_rejected_before_any_api_request(self):
        for branch in ("release/1", "release/01.0", "release/1.0.0", "release/1.0;echo injected"):
            with self.subTest(branch=branch), patch.object(bump_dev_github, "github_api") as api:
                with self.assertRaisesRegex(ValueError, "Release branch must"):
                    bump_dev_github.advance_development_version("fixture/project", branch)
                api.assert_not_called()

    def test_mismatched_or_malformed_release_metadata_never_reads_or_writes_dev(self):
        for content in (
            "versionName=2.0.0\nversionCode=4\n",
            "versionName=1.1.0\nversionCode=4\n",
            "versionName=1.0.0-beta1\nversionCode=4\n",
            "versionName=1.0.0\nversionName=1.0.1\nversionCode=4\n",
            "versionCode=4\n",
        ):
            with self.subTest(content=content):
                with patch.object(bump_dev_github, "github_api", return_value=document(content)) as api:
                    with self.assertRaises(ValueError):
                        bump_dev_github.advance_development_version("fixture/project", "release/1.0")
                api.assert_called_once_with(f"{ENDPOINT}?ref=release%2F1.0")

    def test_invalid_or_behind_development_version_never_writes(self):
        for content, branch, release in (
            ("versionName=1.0.2\nversionCode=0\n", "release/1.0", "1.0.0"),
            ("versionName=1.0.2\nversionCode=2100000000\n", "release/1.0", "1.0.0"),
            ("versionName=2.0.2\nversionCode=4\n", "release/1.0", "1.0.0"),
            ("versionName=1.0.2\nversionCode=4\n", "release/1.1", "1.1.0"),
        ):
            with self.subTest(content=content, branch=branch):
                responses = [document(f"versionName={release}\nversionCode=4\n"), document(content)]
                with patch.object(bump_dev_github, "github_api", side_effect=responses) as api:
                    with self.assertRaises(ValueError):
                        bump_dev_github.advance_development_version("fixture/project", branch)
                self.assertEqual(2, api.call_count)

    def test_conflict_is_not_retried_with_a_new_sha(self):
        with patch.object(
            bump_dev_github, "github_api", side_effect=[document(), document(), RuntimeError("409 conflict")],
        ) as api:
            with self.assertRaisesRegex(RuntimeError, "409 conflict"):
                bump_dev_github.advance_development_version("fixture/project", "release/1.0")
        self.assertEqual(3, api.call_count)

    def test_api_errors_explain_permissions_or_safe_retry(self):
        for status, guidance in ((403, "Allow Actions contents:write"), (409, "fetch the latest dev version")):
            result = subprocess.CompletedProcess([], 1, "", f"gh: simulated failure (HTTP {status})")
            with self.subTest(status=status), patch.object(bump_dev_github.subprocess, "run", return_value=result):
                with self.assertRaisesRegex(RuntimeError, guidance):
                    bump_dev_github.github_api(ENDPOINT, {"sha": "original"})

    def test_api_payload_is_sent_on_stdin_and_token_stays_out_of_arguments(self):
        result = subprocess.CompletedProcess([], 0, "{}", "")
        with patch.dict(os.environ, GH_TOKEN="fixture-private-token"):
            with patch.object(bump_dev_github.subprocess, "run", return_value=result) as run:
                bump_dev_github.github_api(ENDPOINT, {"sha": "original"})
        self.assertEqual(["gh", "api", "--method", "PUT", ENDPOINT, "--input", "-"], run.call_args.args[0])
        self.assertEqual({"sha": "original"}, json.loads(run.call_args.kwargs["input"]))
        self.assertNotIn("fixture-private-token", str(run.call_args))

    def test_invalid_api_file_response_is_rejected(self):
        for response in (
            dict(document(), type="dir"),
            dict(document(), encoding="none"),
            dict(document(), content="not base64!"),
        ):
            with self.subTest(response=response), patch.object(bump_dev_github, "github_api", return_value=response) as api:
                with self.assertRaises(ValueError):
                    bump_dev_github.advance_development_version("fixture/project", "release/1.0")
                self.assertEqual(1, api.call_count)

    def test_cli_runs_from_another_working_directory(self):
        with tempfile.TemporaryDirectory(prefix="glyph-github-cli-test-") as directory:
            root = Path(directory)
            fake_gh = root / "gh"
            fake_gh.write_text("#!" + sys.executable + "\n" + textwrap.dedent("""\
                import base64, json, os, sys
                method = sys.argv[sys.argv.index('--method') + 1]
                if method == 'PUT':
                    with open(os.environ['MOCK_REQUEST'], 'w') as output:
                        json.dump(json.load(sys.stdin), output)
                    print('{}')
                else:
                    print(json.dumps({
                        'type': 'file', 'encoding': 'base64', 'sha': 'cli-original-sha',
                        'content': base64.b64encode(b'versionName=1.0.2\\nversionCode=4\\n').decode(),
                    }))
                """), encoding="utf-8")
            fake_gh.chmod(0o755)
            request = root / "request.json"
            environment = dict(
                os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                PYTHONDONTWRITEBYTECODE="1", GH_TOKEN="offline-fixture-token", GH_REPO="fixture/project",
                RELEASE_BRANCH="release/1.0", MOCK_REQUEST=str(request),
            )
            result = subprocess.run(
                [sys.executable, "-B", str(Path(bump_dev_github.__file__).resolve())],
                cwd=root, env=environment, capture_output=True, text=True,
            )
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn("Committed development version 1.1.0 (code 5)", result.stdout)
            payload = json.loads(request.read_text())
            self.assertEqual("cli-original-sha", payload["sha"])
            self.assertEqual("dev", payload["branch"])
            self.assertNotIn("offline-fixture-token", request.read_text() + result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
