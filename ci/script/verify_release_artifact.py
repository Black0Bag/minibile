#!/usr/bin/env python3
"""Verify the final Android release APK against repository release metadata."""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import subprocess
import sys
from pathlib import Path

from release_version import APPLICATION_ID, VersionContractError, read_worktree_version


PACKAGE_PATTERN = re.compile(
    r"^package:\s+name='(?P<package>[^']+)'\s+versionCode='(?P<code>[0-9]+)'\s+versionName='(?P<name>[^']+)'"
)
CERTIFICATE_PATTERN = re.compile(
    r"Signer #1 certificate SHA-256 digest:\s*(?P<digest>[0-9A-Fa-f:]+)"
)
SHA256_PATTERN = re.compile(r"^(?P<digest>[0-9a-f]{64})\s{2}(?P<name>[^/\\]+)$")


class ArtifactVerificationError(ValueError):
    """Raised when a release artifact does not match the repository contract."""


def normalized_digest(value: str, source: str) -> str:
    digest = value.strip().replace(":", "").lower()
    if re.fullmatch(r"[0-9a-f]{64}", digest) is None:
        raise ArtifactVerificationError(f"{source} must contain one SHA-256 digest")
    return digest


def parse_badging(output: str) -> tuple[str, int, str]:
    first_line = next((line for line in output.splitlines() if line.startswith("package:")), "")
    match = PACKAGE_PATTERN.match(first_line)
    if match is None:
        raise ArtifactVerificationError("aapt output does not contain a parseable package line")
    return match.group("package"), int(match.group("code")), match.group("name")


def parse_certificate_digest(output: str) -> str:
    match = CERTIFICATE_PATTERN.search(output)
    if match is None:
        raise ArtifactVerificationError(
            "apksigner output does not contain signer #1 certificate SHA-256 digest"
        )
    return normalized_digest(match.group("digest"), "apksigner certificate digest")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_checksum(path: Path, apk: Path, digest: str) -> None:
    path.write_text(f"{digest}  {apk.name}\n", encoding="utf-8")


def validate_checksum(path: Path, apk: Path, digest: str) -> None:
    if not path.is_file():
        raise ArtifactVerificationError(f"missing checksum file: {path}")
    match = SHA256_PATTERN.fullmatch(path.read_text(encoding="utf-8").strip())
    if match is None:
        raise ArtifactVerificationError(f"invalid checksum file format: {path}")
    if match.group("name") != apk.name:
        raise ArtifactVerificationError(
            f"checksum names {match.group('name')}, expected {apk.name}"
        )
    if match.group("digest") != digest:
        raise ArtifactVerificationError(
            f"checksum records {match.group('digest')}, computed {digest}"
        )


def run_command(*command: str) -> str:
    result = subprocess.run(command, check=True, capture_output=True, text=True)
    return result.stdout + result.stderr


def verify(args: argparse.Namespace) -> None:
    root = args.root.resolve()
    version = read_worktree_version(root)
    expected_apk_name = version.apk_name
    expected_checksum_name = version.checksum_name
    if args.apk.name != expected_apk_name:
        raise ArtifactVerificationError(
            f"APK file name is {args.apk.name}, expected {expected_apk_name}"
        )
    if args.checksum.name != expected_checksum_name:
        raise ArtifactVerificationError(
            f"checksum file name is {args.checksum.name}, expected {expected_checksum_name}"
        )
    if not args.apk.is_file() or args.apk.stat().st_size == 0:
        raise ArtifactVerificationError(f"missing or empty APK: {args.apk}")

    package_name, version_code, version_name = parse_badging(
        run_command(str(args.aapt), "dump", "badging", str(args.apk))
    )
    if package_name != APPLICATION_ID:
        raise ArtifactVerificationError(
            f"APK applicationId is {package_name}, expected {APPLICATION_ID}"
        )
    if version_name != str(version):
        raise ArtifactVerificationError(
            f"APK versionName is {version_name}, expected {version}"
        )
    if version_code != version.version_code:
        raise ArtifactVerificationError(
            f"APK versionCode is {version_code}, expected {version.version_code}"
        )

    certificate_digest = parse_certificate_digest(
        run_command(
            str(args.apksigner),
            "verify",
            "--verbose",
            "--print-certs",
            str(args.apk),
        )
    )
    expected_certificate_digest = normalized_digest(
        args.certificate_sha256.read_text(encoding="utf-8"),
        str(args.certificate_sha256),
    )
    if certificate_digest != expected_certificate_digest:
        raise ArtifactVerificationError(
            f"APK certificate SHA-256 is {certificate_digest}, expected {expected_certificate_digest}"
        )

    digest = sha256(args.apk)
    if args.write_checksum:
        write_checksum(args.checksum, args.apk, digest)
    validate_checksum(args.checksum, args.apk, digest)
    print(
        "Release artifact OK: "
        f"apk={args.apk.name} bytes={args.apk.stat().st_size} "
        f"applicationId={package_name} versionName={version_name} "
        f"versionCode={version_code} sha256={digest} certificateSha256={certificate_digest}"
    )


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--root", type=Path, default=Path.cwd())
    result.add_argument("--apk", type=Path, required=True)
    result.add_argument("--checksum", type=Path, required=True)
    result.add_argument("--certificate-sha256", type=Path, required=True)
    result.add_argument("--aapt", type=Path, required=True)
    result.add_argument("--apksigner", type=Path, required=True)
    result.add_argument("--write-checksum", action="store_true")
    return result


def main() -> int:
    try:
        verify(parser().parse_args())
        return 0
    except (OSError, subprocess.CalledProcessError, VersionContractError, ArtifactVerificationError) as error:
        if os.environ.get("GITHUB_ACTIONS") == "true":
            print(f"::error title=Android release artifact::{error}", file=sys.stderr)
        else:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())