#!/usr/bin/env python3
"""Advance dev after creating release/MAJOR.MINOR (Python 3.9+)."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import stat
import sys
import tempfile


NUMBER = r"(?:0|[1-9][0-9]*)"
RELEASE_BRANCH = re.compile(rf"release/({NUMBER})\.({NUMBER})")
VERSION_NAME = re.compile(rf"({NUMBER})\.({NUMBER})\.({NUMBER})")
PROPERTY = re.compile(
    r"(?P<prefix>[ \t]*(?P<key>versionName|versionCode)[ \t]*=[ \t]*)"
    r"(?P<value>[^\s]+)(?P<suffix>[ \t]*)(?P<newline>\r?\n)?"
)
MAX_VERSION_CODE = 2_100_000_000
DEFAULT_FILE = Path(__file__).resolve().parents[1] / "version.properties"


def atomic_write(path: Path, content: str) -> None:
    mode = stat.S_IMODE(path.stat().st_mode)
    temporary_path = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", newline="", dir=path.parent,
            prefix=f".{path.name}.", delete=False,
        ) as temporary:
            temporary_path = Path(temporary.name)
            temporary.write(content)
        temporary_path.chmod(mode)
        os.replace(temporary_path, path)
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def bump_version(path: Path, release_branch: str) -> tuple[str, int] | None:
    release = RELEASE_BRANCH.fullmatch(release_branch)
    if release is None:
        raise ValueError("Release branch must be release/MAJOR.MINOR with no leading zeros.")
    release_major, release_minor = map(int, release.groups())

    with path.open(encoding="utf-8", newline="") as version_file:
        lines = version_file.readlines()
    properties = {}
    for index, line in enumerate(lines):
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        match = PROPERTY.fullmatch(line)
        if match is None:
            raise ValueError(f"Invalid version property on line {index + 1}.")
        key = match.group("key")
        if key in properties:
            raise ValueError(f"Duplicate {key} property.")
        properties[key] = (index, match)
    if set(properties) != {"versionName", "versionCode"}:
        raise ValueError("version.properties must contain versionName and versionCode.")

    version = VERSION_NAME.fullmatch(properties["versionName"][1].group("value"))
    if version is None:
        raise ValueError("versionName must be MAJOR.MINOR.PATCH with no leading zeros.")
    major, minor, _ = map(int, version.groups())
    code_text = properties["versionCode"][1].group("value")
    if re.fullmatch(r"[1-9][0-9]*", code_text) is None:
        raise ValueError("versionCode must be a positive integer with no leading zeros.")
    code = int(code_text)
    if code > MAX_VERSION_CODE:
        raise ValueError(f"versionCode cannot exceed {MAX_VERSION_CODE}.")

    if major != release_major:
        raise ValueError("Development and release major versions differ; no automatic bump is safe.")
    if minor < release_minor:
        raise ValueError("Development version is behind the release branch; no automatic bump is safe.")
    if minor > release_minor:
        return None
    if code == MAX_VERSION_CODE:
        raise ValueError(f"Cannot increment versionCode beyond {MAX_VERSION_CODE}.")

    new_version = f"{major}.{minor + 1}.0"
    new_code = code + 1
    for key, value in {"versionName": new_version, "versionCode": str(new_code)}.items():
        index, match = properties[key]
        lines[index] = (
            match.group("prefix") + value + match.group("suffix") + (match.group("newline") or "")
        )
    atomic_write(path, "".join(lines))
    return new_version, new_code


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("release_branch", help="new release branch, for example release/1.0")
    parser.add_argument("--file", type=Path, default=DEFAULT_FILE, help="version.properties path")
    args = parser.parse_args()
    try:
        result = bump_version(args.file.expanduser().resolve(), args.release_branch)
    except (OSError, UnicodeError, ValueError) as error:
        print(f"Version bump failed: {error}", file=sys.stderr)
        return 1
    if result is None:
        print(f"Development version already advanced beyond {args.release_branch}; no change.")
    else:
        version, code = result
        print(f"Updated development version to {version} (code {code}) for {args.release_branch}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
