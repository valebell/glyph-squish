#!/usr/bin/env python3
"""Fetch the pinned, checksum-verified Nothing Glyph Matrix SDK (Python 3.9+)."""

import argparse
import hashlib
import os
from pathlib import Path
import sys
import tempfile
import urllib.error
import urllib.request


# Official SDK and license at the same immutable revision:
# https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/
# blob/a65d8e1d13e796919894e54ce43f93d939cdb34c/LICENSE.md
SDK_COMMIT = "a65d8e1d13e796919894e54ce43f93d939cdb34c"
SDK_URL = (
    "https://raw.githubusercontent.com/"
    "Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/"
    f"{SDK_COMMIT}/glyph-matrix-sdk-2.0.aar"
)
SDK_SHA256 = "be00ee9cd7115f6b11984c6e31fe98e298fb726940d1555063610685ef3bbf29"
DEFAULT_DESTINATION = (
    Path(__file__).resolve().parents[1] / "app/libs/glyph-matrix-sdk-2.0.aar"
)


def verify_checksum(actual: str, source: str) -> None:
    if actual != SDK_SHA256:
        raise ValueError(
            f"SHA-256 mismatch for {source}.\n"
            f"Expected: {SDK_SHA256}\n"
            f"Actual:   {actual}\n"
            "No SDK file was installed or replaced. If an existing SDK is corrupt, "
            "remove it explicitly before retrying."
        )


def verify_existing(destination: Path) -> None:
    digest = hashlib.sha256()
    with destination.open("rb") as existing:
        for chunk in iter(lambda: existing.read(64 * 1024), b""):
            digest.update(chunk)
    verify_checksum(digest.hexdigest(), str(destination))


def fetch_sdk(destination: Path) -> None:
    if destination.exists() or destination.is_symlink():
        verify_existing(destination)
        print(f"Verified existing SDK: {destination}")
        return

    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = None
    try:
        # Keep the temporary file on the same filesystem for atomic installation.
        with tempfile.NamedTemporaryFile(
            dir=destination.parent, prefix=f".{destination.name}.", delete=False
        ) as temporary:
            temporary_path = Path(temporary.name)
            digest = hashlib.sha256()
            with urllib.request.urlopen(SDK_URL, timeout=30) as response:
                while chunk := response.read(64 * 1024):
                    temporary.write(chunk)
                    digest.update(chunk)
        verify_checksum(digest.hexdigest(), SDK_URL)

        try:
            # Unlike replace(), link() cannot overwrite a concurrent download.
            os.link(temporary_path, destination)
        except FileExistsError:
            verify_existing(destination)
        print(f"Verified SDK available: {destination}")
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--destination",
        type=Path,
        default=DEFAULT_DESTINATION,
        help="output AAR path (default: app/libs/glyph-matrix-sdk-2.0.aar in this repository)",
    )
    args = parser.parse_args()
    try:
        fetch_sdk(args.destination.expanduser().absolute())
    except (OSError, urllib.error.URLError, ValueError) as error:
        print(f"Could not prepare the Nothing Glyph Matrix SDK: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
