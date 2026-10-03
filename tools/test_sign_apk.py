"""Signing failure, credential handling, and artifact publication regressions."""

import base64
import contextlib
import hashlib
import io
import os
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import sign_apk


class SignApkTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="glyph-signing-test-")
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.runner = self.root / "runner"
        self.runner.mkdir()
        self.output = self.root / "dist"
        self.unsigned = self.root / "unsigned.apk"
        self.unsigned.write_bytes(b"unsigned fixture")
        self.tools = self.root / "sdk" / "build-tools" / sign_apk.BUILD_TOOLS_VERSION
        self.tools.mkdir(parents=True)
        for name in ("zipalign", "apksigner"):
            tool = self.tools / name
            tool.write_text("fixture executable", encoding="utf-8")
            tool.chmod(0o700)
        self.environment = {
            "ANDROID_HOME": str(self.root / "sdk"),
            "RUNNER_TEMP": str(self.runner),
            "ANDROID_KEYSTORE_BASE64": base64.b64encode(b"fake keystore").decode("ascii"),
            "ANDROID_KEYSTORE_PASSWORD": "fixture-store-password",
            "ANDROID_KEY_ALIAS": "fixture-alias",
            "ANDROID_KEY_PASSWORD": "fixture-key-password",
        }
        environment_patch = patch.dict(os.environ, self.environment, clear=True)
        environment_patch.start()
        self.addCleanup(environment_patch.stop)
        self.key_paths = []
        self.failure = None

    def run_native(self, arguments, *, env, capture_output, text):
        self.assertTrue(capture_output)
        self.assertTrue(text)
        for name in ("ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_PASSWORD"):
            self.assertNotIn(self.environment[name], arguments)
        self.assertNotIn("ANDROID_KEYSTORE_BASE64", env)
        self.assertNotIn("ANDROID_KEY_ALIAS", env)
        operation = arguments[1]
        if operation == "sign":
            key = Path(arguments[arguments.index("--ks") + 1])
            self.key_paths.append(key)
            self.assertEqual(b"fake keystore", key.read_bytes())
            self.assertEqual(0o600, stat.S_IMODE(key.stat().st_mode))
            self.assertEqual(self.environment["ANDROID_KEY_PASSWORD"], env["ANDROID_KEY_PASSWORD"])
            Path(arguments[arguments.index("--out") + 1]).write_bytes(b"signed fixture")
        else:
            self.assertNotIn("ANDROID_KEYSTORE_PASSWORD", env)
            self.assertNotIn("ANDROID_KEY_PASSWORD", env)
            if operation == "-P":
                Path(arguments[-1]).write_bytes(self.unsigned.read_bytes())
        output = "Certificate: fixture-alias\n" if operation == "verify" else ""
        if operation == self.failure:
            output = " ".join(self.environment[name] for name in sign_apk.SECRETS)
        return subprocess.CompletedProcess(arguments, 1 if operation == self.failure else 0, output, output)

    def sign(self):
        return sign_apk.sign_apk(self.unsigned, "1.0.0", self.output)

    def assert_temporaries_removed(self):
        self.assertEqual([], list(self.runner.iterdir()))
        self.assertFalse(any(path.exists() for path in self.key_paths))
        self.assertFalse(list(self.output.glob(".glyph-signing-*")))

    def test_verified_apk_and_standard_checksum_are_published_without_secret_output(self):
        output = io.StringIO()
        with patch.object(sign_apk.subprocess, "run", side_effect=self.run_native), contextlib.redirect_stdout(output):
            apk = self.sign()
        self.assertEqual(b"signed fixture", apk.read_bytes())
        checksum = self.output / "glyph-squish-1.0.0.apk.sha256"
        expected = hashlib.sha256(apk.read_bytes()).hexdigest()
        self.assertEqual(f"{expected}  {apk.name}\n", checksum.read_text())
        self.assertEqual(b"unsigned fixture", self.unsigned.read_bytes())
        for name in sign_apk.SECRETS:
            self.assertNotIn(self.environment[name], output.getvalue())
        self.assert_temporaries_removed()

    def test_signing_and_verification_failures_remove_key_and_publish_nothing(self):
        for operation in ("sign", "verify", "-c"):
            with self.subTest(operation=operation):
                self.failure = operation
                output = io.StringIO()
                with patch.object(sign_apk.subprocess, "run", side_effect=self.run_native), contextlib.redirect_stdout(output):
                    with self.assertRaises(sign_apk.SigningError) as error:
                        self.sign()
                self.assertEqual([], list(self.output.iterdir()))
                for name in sign_apk.SECRETS:
                    self.assertNotIn(self.environment[name], str(error.exception) + output.getvalue())
                self.assert_temporaries_removed()

    def test_existing_outputs_are_never_replaced(self):
        self.output.mkdir()
        existing = self.output / "glyph-squish-1.0.0.apk.sha256"
        existing.write_bytes(b"existing checksum")
        with patch.object(sign_apk.subprocess, "run") as native:
            with self.assertRaises(sign_apk.SigningError):
                self.sign()
        native.assert_not_called()
        self.assertEqual(b"existing checksum", existing.read_bytes())
        self.assertEqual([existing], list(self.output.iterdir()))

    def test_failed_second_publication_rolls_back_first_artifact(self):
        real_link = os.link

        def link(source, destination):
            if str(destination).endswith(".sha256"):
                raise OSError("fixture publication failure")
            real_link(source, destination)

        with patch.object(sign_apk.subprocess, "run", side_effect=self.run_native), \
                patch.object(sign_apk.os, "link", side_effect=link), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(sign_apk.SigningError):
                self.sign()
        self.assertEqual([], list(self.output.iterdir()))
        self.assert_temporaries_removed()

    def test_invalid_version_or_credentials_do_not_start_tools(self):
        with patch.object(sign_apk.subprocess, "run") as native:
            for version in ("1.0", "01.0.0", "../1.0.0", "1.0.0-rc1"):
                with self.subTest(version=version), self.assertRaises(sign_apk.SigningError):
                    sign_apk.sign_apk(self.unsigned, version, self.output)
            with patch.dict(os.environ, {"ANDROID_KEY_PASSWORD": ""}):
                with self.assertRaisesRegex(sign_apk.SigningError, "ANDROID_KEY_PASSWORD"):
                    self.sign()
            for encoded in ("invalid!", "   "):
                with patch.dict(os.environ, {"ANDROID_KEYSTORE_BASE64": encoded}):
                    with self.assertRaises(sign_apk.SigningError):
                        self.sign()
        native.assert_not_called()
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
