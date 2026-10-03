#!/usr/bin/env python3
"""Align, sign, verify, and checksum a release APK (Python 3.9+)."""

import argparse
import base64
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile


SECRETS = (
    "ANDROID_KEYSTORE_BASE64",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
)
VERSION = re.compile(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)")
BUILD_TOOLS_VERSION = "36.0.0"


class SigningError(Exception):
    """An error message safe to display without signing credentials."""


def run_tool(arguments, label, environment, secrets, show_output=False):
    try:
        result = subprocess.run(arguments, env=environment, capture_output=True, text=True)
    except OSError:
        raise SigningError(f"Could not start {label}.") from None
    if result.returncode:
        # Native tools may echo an alias or arguments on failure. Do not relay them.
        raise SigningError(f"{label} failed (exit {result.returncode}).")
    if show_output:
        output = result.stdout
        for value in secrets.values():
            output = output.replace(value, "[redacted]")
        print(output, end="" if output.endswith("\n") else "\n")


def sign_apk(unsigned_apk: Path, version: str, output_dir: Path) -> Path:
    if VERSION.fullmatch(version) is None:
        raise SigningError("Version must be MAJOR.MINOR.PATCH with no leading zeros.")
    missing = [name for name in SECRETS if not os.environ.get(name)]
    if missing:
        raise SigningError("Missing repository Actions secrets: " + ", ".join(missing))
    if not os.environ.get("ANDROID_HOME"):
        raise SigningError("ANDROID_HOME must point to the Android SDK.")
    secrets = {name: os.environ[name] for name in SECRETS}
    tools = Path(os.environ["ANDROID_HOME"]) / "build-tools" / BUILD_TOOLS_VERSION
    zipalign, apksigner = tools / "zipalign", tools / "apksigner"
    if any(not tool.is_file() or not os.access(tool, os.X_OK) for tool in (zipalign, apksigner)):
        raise SigningError(f"Install Android SDK Build-Tools {BUILD_TOOLS_VERSION} before signing.")
    if not unsigned_apk.is_file():
        raise SigningError("The unsigned APK does not exist or is not a file.")
    try:
        encoded = "".join(secrets["ANDROID_KEYSTORE_BASE64"].split())
        key_bytes = base64.b64decode(encoded, validate=True)
    except ValueError:
        raise SigningError("ANDROID_KEYSTORE_BASE64 is not valid Base64.") from None
    if not key_bytes:
        raise SigningError("ANDROID_KEYSTORE_BASE64 decodes to an empty keystore.")

    filename = f"glyph-squish-{version}.apk"
    destinations = (output_dir / filename, output_dir / f"{filename}.sha256")
    if any(path.exists() or path.is_symlink() for path in destinations):
        raise SigningError("Release output already exists; use a fresh output directory.")
    output_dir.mkdir(parents=True, exist_ok=True)
    environment = {name: value for name, value in os.environ.items() if name not in SECRETS}
    signing_environment = dict(environment, **{
        name: secrets[name] for name in ("ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_PASSWORD")
    })
    with tempfile.TemporaryDirectory(
        prefix="glyph-signing-key-", dir=os.environ.get("RUNNER_TEMP") or None,
    ) as key_directory, tempfile.TemporaryDirectory(prefix=".glyph-signing-", dir=output_dir) as stage_directory:
        key = Path(key_directory) / "release.jks"
        with os.fdopen(os.open(key, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "wb") as key_file:
            key_file.write(key_bytes)
        stage = Path(stage_directory)
        aligned, signed = stage / "aligned.apk", stage / filename
        run_tool(
            [str(zipalign), "-P", "16", "-f", "4", str(unsigned_apk), str(aligned)],
            "APK alignment", environment, secrets,
        )
        run_tool(
            [str(apksigner), "sign", "--ks", str(key), "--ks-key-alias", secrets["ANDROID_KEY_ALIAS"],
             "--ks-pass", "env:ANDROID_KEYSTORE_PASSWORD", "--key-pass", "env:ANDROID_KEY_PASSWORD",
             "--v4-signing-enabled", "false", "--out", str(signed), str(aligned)],
            "APK signing", signing_environment, secrets,
        )
        run_tool(
            [str(apksigner), "verify", "--verbose", "--print-certs", str(signed)],
            "APK signature verification", environment, secrets, show_output=True,
        )
        run_tool(
            [str(zipalign), "-c", "-P", "16", "4", str(signed)],
            "Signed APK alignment verification", environment, secrets,
        )
        digest = hashlib.sha256()
        with signed.open("rb") as apk:
            for chunk in iter(lambda: apk.read(1024 * 1024), b""):
                digest.update(chunk)
        checksum = stage / f"{filename}.sha256"
        checksum.write_text(f"{digest.hexdigest()}  {filename}\n", encoding="utf-8")
        published = []
        try:
            # Hard links publish only fully verified files and never replace existing outputs.
            for source, destination in zip((signed, checksum), destinations):
                os.link(source, destination)
                published.append(destination)
        except OSError:
            for destination in published:
                destination.unlink(missing_ok=True)
            raise SigningError("Could not publish the verified APK and checksum; existing outputs were preserved.") from None
    return destinations[0]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("unsigned_apk", type=Path, help="unsigned release APK to sign")
    parser.add_argument("--version", required=True, help="release version, for example 1.0.0")
    parser.add_argument("--output-dir", type=Path, default=Path("dist"))
    args = parser.parse_args()
    try:
        result = sign_apk(args.unsigned_apk.expanduser().resolve(), args.version, args.output_dir.expanduser().resolve())
    except SigningError as error:
        print(f"Signing failed: {error}", file=sys.stderr)
        return 1
    except (OSError, UnicodeError):
        print("Signing failed: could not read or write the signing files.", file=sys.stderr)
        return 1
    print(f"Verified signed APK and checksum: {result.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
