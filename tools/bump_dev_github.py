#!/usr/bin/env python3
"""Advance dev through GitHub's Contents API after creating a release branch."""

from __future__ import annotations

import base64
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
from urllib.parse import quote

from bump_dev_version import PROPERTY, RELEASE_BRANCH, VERSION_NAME, bump_version


def github_api(resource: str, payload: dict | None = None) -> dict:
    command = ["gh", "api", "--method", "PUT" if payload is not None else "GET", resource]
    if payload is not None:
        command += ["--input", "-"]
    result = subprocess.run(
        command,
        input=json.dumps(payload) if payload is not None else None,
        capture_output=True,
        text=True,
    )
    if result.returncode:
        if "HTTP 403" in result.stderr:
            raise RuntimeError(
                "GitHub denied the version update (403). Allow Actions contents:write "
                "and updates to dev in branch rules, then rerun this workflow."
            )
        if "HTTP 409" in result.stderr:
            raise RuntimeError(
                "Development changed concurrently (409). Rerun this workflow "
                "to fetch the latest dev version and retry safely."
            )
        raise RuntimeError(f"GitHub API request failed: {result.stderr.strip()}")
    return json.loads(result.stdout)


def file_content(document: dict) -> bytes:
    if document.get("type") != "file" or document.get("encoding") != "base64":
        raise ValueError("Expected version.properties to be a regular, base64-encoded file.")
    return base64.b64decode("".join(document["content"].split()), validate=True)


def validate_release_version(content: bytes, branch: str, expected: tuple[int, int]) -> None:
    properties = {}
    for line in content.decode("utf-8").splitlines(keepends=True):
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        match = PROPERTY.fullmatch(line)
        if match is None or match.group("key") in properties:
            raise ValueError("The release branch contains invalid or duplicate version properties.")
        properties[match.group("key")] = match.group("value")
    version = VERSION_NAME.fullmatch(properties.get("versionName", ""))
    if version is None or tuple(map(int, version.groups()[:2])) != expected:
        raise ValueError(f"The release branch versionName does not match {branch}; dev was not changed.")


def advance_development_version(repository: str, branch: str) -> tuple[str, int] | None:
    release = RELEASE_BRANCH.fullmatch(branch)
    if release is None:
        raise ValueError("Release branch must be release/MAJOR.MINOR with no leading zeros.")
    expected = tuple(map(int, release.groups()))
    endpoint = f"repos/{repository}/contents/version.properties"
    release_content = file_content(github_api(f"{endpoint}?ref={quote(branch, safe='')}"))
    validate_release_version(release_content, branch, expected)

    development = github_api(f"{endpoint}?ref=dev")
    original = file_content(development)
    with tempfile.TemporaryDirectory(prefix="glyph-dev-version-") as directory:
        version_file = Path(directory) / "version.properties"
        version_file.write_bytes(original)
        result = bump_version(version_file, branch)
        updated = version_file.read_bytes()
    if updated == original:
        return None

    # The original blob SHA makes concurrent edits fail instead of overwriting them.
    github_api(endpoint, {
        "message": f"chore: advance dev version after {branch}",
        "content": base64.b64encode(updated).decode("ascii"),
        "sha": development["sha"],
        "branch": "dev",
    })
    return result


def main() -> int:
    repository = os.environ.get("GH_REPO", "")
    branch = os.environ.get("RELEASE_BRANCH", "")
    try:
        if not repository or not branch:
            raise ValueError("GH_REPO and RELEASE_BRANCH must be set.")
        result = advance_development_version(repository, branch)
    except (OSError, RuntimeError, ValueError) as error:
        print(f"GitHub version bump failed: {error}", file=sys.stderr)
        return 1
    if result is None:
        print(f"Development version already advanced beyond {branch}; no change.")
    else:
        version, code = result
        print(f"Committed development version {version} (code {code}) to dev after {branch}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
