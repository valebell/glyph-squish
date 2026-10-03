"""Regression tests for release-branch development version bumps."""

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import bump_dev_version


class BumpDevelopmentVersionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="glyph-version-test-")
        self.addCleanup(self.temporary.cleanup)
        self.path = Path(self.temporary.name) / "version.properties"
        self.path.write_text("versionName=1.0.0\nversionCode=1\n", encoding="utf-8")

    def assert_rejected_without_changes(self, branch="release/1.0"):
        before = self.path.read_bytes()
        with self.assertRaises(ValueError):
            bump_dev_version.bump_version(self.path, branch)
        self.assertEqual(before, self.path.read_bytes())
        self.assertEqual([self.path], list(self.path.parent.iterdir()))

    def test_first_release_advances_minor_and_code(self):
        self.assertEqual(("1.1.0", 2), bump_dev_version.bump_version(self.path, "release/1.0"))
        self.assertEqual(b"versionName=1.1.0\nversionCode=2\n", self.path.read_bytes())

    def test_patch_release_advances_minor_and_resets_patch(self):
        self.path.write_text("versionName=1.0.9\nversionCode=42\n", encoding="utf-8")
        self.assertEqual(("1.1.0", 43), bump_dev_version.bump_version(self.path, "release/1.0"))

    def test_zero_major_and_multi_digit_minor_are_supported(self):
        self.path.write_text("versionName=0.19.4\nversionCode=9\n", encoding="utf-8")
        self.assertEqual(("0.20.0", 10), bump_dev_version.bump_version(self.path, "release/0.19"))

    def test_rerun_does_not_increment_again_or_replace_file(self):
        bump_dev_version.bump_version(self.path, "release/1.0")
        before = self.path.read_bytes()
        original_stat = self.path.stat()
        self.assertIsNone(bump_dev_version.bump_version(self.path, "release/1.0"))
        self.assertEqual(before, self.path.read_bytes())
        self.assertEqual(original_stat.st_ino, self.path.stat().st_ino)
        self.assertEqual(original_stat.st_mtime_ns, self.path.stat().st_mtime_ns)

    def test_later_development_minor_is_unchanged(self):
        self.path.write_text("versionName=1.3.7\nversionCode=2100000000\n", encoding="utf-8")
        before = self.path.read_bytes()
        self.assertIsNone(bump_dev_version.bump_version(self.path, "release/1.0"))
        self.assertEqual(before, self.path.read_bytes())

    def test_mismatched_major_or_future_release_minor_is_rejected(self):
        for branch in ("release/0.9", "release/2.0", "release/1.1"):
            with self.subTest(branch=branch):
                self.assert_rejected_without_changes(branch)

    def test_malformed_release_branches_are_rejected(self):
        for branch in (
            "1.0", "release/1", "release/1.0.0", "release/01.0", "release/1.00",
            "release/-1.0", "release/1.-1", "release/1.0\n", "release/1.0/extra", "release/+1.0",
        ):
            with self.subTest(branch=branch):
                self.assert_rejected_without_changes(branch)

    def test_malformed_versions_are_rejected(self):
        for version in ("1.0", "1.0.0.0", "01.0.0", "1.00.0", "1.0.01", "1.0.-1", "1.0.0-rc1"):
            with self.subTest(version=version):
                self.path.write_text(f"versionName={version}\nversionCode=1\n", encoding="utf-8")
                self.assert_rejected_without_changes()

    def test_invalid_codes_and_overflow_are_rejected(self):
        for code in ("0", "-1", "+1", "01", "1.0", "2147483647", "2100000001", "2100000000"):
            with self.subTest(code=code):
                self.path.write_text(f"versionName=1.0.0\nversionCode={code}\n", encoding="utf-8")
                self.assert_rejected_without_changes()

    def test_maximum_code_can_be_reached(self):
        self.path.write_text("versionName=1.0.0\nversionCode=2099999999\n", encoding="utf-8")
        self.assertEqual(("1.1.0", 2100000000), bump_dev_version.bump_version(self.path, "release/1.0"))

    def test_invalid_properties_are_rejected(self):
        for content in (
            "versionName=1.0.0\n",
            "versionCode=1\n",
            "versionName=1.0.0\nversionCode=1\nversionCode=2\n",
            "versionName=1.0.0\nversionCode=1\nversionName=1.1.0\n",
            "versionName:1.0.0\nversionCode=1\n",
            "versionName=\nversionCode=1\n",
            "versionName=1.0.0\nversionCode=1\nunknown=value\n",
        ):
            with self.subTest(content=content):
                self.path.write_text(content, encoding="utf-8")
                self.assert_rejected_without_changes()

    def test_comments_spacing_line_endings_and_permissions_are_preserved(self):
        self.path.write_bytes(b"# Release versions\r\n\r\n versionName = 1.0.3 \r\n! Code\r\nversionCode=7")
        self.path.chmod(0o640)
        bump_dev_version.bump_version(self.path, "release/1.0")
        self.assertEqual(
            b"# Release versions\r\n\r\n versionName = 1.1.0 \r\n! Code\r\nversionCode=8",
            self.path.read_bytes(),
        )
        self.assertEqual(0o640, self.path.stat().st_mode & 0o777)

    def test_failed_atomic_replace_preserves_original_and_cleans_temporary_file(self):
        before = self.path.read_bytes()
        with patch.object(bump_dev_version.os, "replace", side_effect=OSError("fixture write failure")):
            with self.assertRaises(OSError):
                bump_dev_version.bump_version(self.path, "release/1.0")
        self.assertEqual(before, self.path.read_bytes())
        self.assertEqual([self.path], list(self.path.parent.iterdir()))

    def test_cli_success_noop_and_failure_from_another_directory(self):
        command = [
            sys.executable, str(Path(bump_dev_version.__file__).resolve()),
            "release/1.0", "--file", str(self.path),
        ]
        environment = dict(os.environ, PYTHONDONTWRITEBYTECODE="1")
        success = subprocess.run(command, cwd=self.path.parent, env=environment, capture_output=True, text=True)
        self.assertEqual(0, success.returncode, success.stderr)
        self.assertIn("1.1.0 (code 2)", success.stdout)
        noop = subprocess.run(command, cwd=self.path.parent, env=environment, capture_output=True, text=True)
        self.assertEqual(0, noop.returncode, noop.stderr)
        self.assertIn("no change", noop.stdout)
        command[2] = "release/2.0"
        before = self.path.read_bytes()
        failure = subprocess.run(command, cwd=self.path.parent, env=environment, capture_output=True, text=True)
        self.assertEqual(1, failure.returncode)
        self.assertIn("Version bump failed:", failure.stderr)
        self.assertEqual(before, self.path.read_bytes())


if __name__ == "__main__":
    unittest.main()
