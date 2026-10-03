#!/usr/bin/env python3
"""Check that a universal APK belongs to its release/MAJOR.MINOR branch."""

import argparse
import json
import os
from pathlib import Path
import re


NUMBER = r"(0|[1-9][0-9]*)"


def check_version(branch: str, metadata: dict) -> str:
    release = re.fullmatch(rf"release/{NUMBER}\.{NUMBER}", branch)
    if release is None:
        raise ValueError("Release branch must be release/MAJOR.MINOR, without leading zeros.")
    if metadata.get("applicationId") != "com.valebell.glyphsquish":
        raise ValueError("Unexpected application ID in release APK metadata.")
    elements = metadata.get("elements", [])
    if len(elements) != 1 or elements[0].get("filters"):
        raise ValueError("Expected one universal release APK.")
    apk = elements[0]
    name = apk.get("versionName", "")
    version = re.fullmatch(rf"{NUMBER}\.{NUMBER}\.{NUMBER}", name)
    if version is None or version.groups()[:2] != release.groups():
        raise ValueError("APK version must match its release branch, for example 1.0.0 on release/1.0.")
    code = apk.get("versionCode")
    if type(code) is not int or not 1 <= code <= 2_100_000_000:
        raise ValueError("Invalid Android version code.")
    return name


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("branch")
    parser.add_argument("--metadata", type=Path, default=Path("app/build/outputs/apk/release/output-metadata.json"))
    args = parser.parse_args()
    try:
        version = check_version(args.branch, json.loads(args.metadata.read_text()))
    except (OSError, ValueError, TypeError, AttributeError) as error:
        raise SystemExit(f"Release version check failed: {error}") from error
    if output := os.environ.get("GITHUB_OUTPUT"):
        with open(output, "a") as output_file:
            output_file.write(f"version={version}\n")
    print(f"Verified release version {version} on {args.branch}.")


if __name__ == "__main__":
    main()
