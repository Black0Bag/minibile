from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "ci" / "script"))

from verify_release_artifact import (  # noqa: E402
    ArtifactVerificationError,
    normalized_digest,
    parse_badging,
    parse_certificate_digest,
    sha256,
    validate_checksum,
    write_checksum,
)


class ReleaseArtifactParserTest(unittest.TestCase):
    def test_aapt_badging_is_parsed(self) -> None:
        package_name, version_code, version_name = parse_badging(
            "package: name='io.github.black0bag.minibile' versionCode='1' versionName='0.0.0' compileSdkVersion='36'\n"
        )

        self.assertEqual(package_name, "io.github.black0bag.minibile")
        self.assertEqual(version_code, 1)
        self.assertEqual(version_name, "0.0.0")

    def test_certificate_digest_is_normalized(self) -> None:
        raw = "AA:" * 31 + "AA"
        output = f"Signer #1 certificate SHA-256 digest: {raw}\n"

        self.assertEqual(parse_certificate_digest(output), "aa" * 32)
        self.assertEqual(normalized_digest(("AA" * 32) + "\n", "fixture"), "aa" * 32)

    def test_invalid_tool_outputs_are_rejected(self) -> None:
        with self.assertRaises(ArtifactVerificationError):
            parse_badging("no package metadata")
        with self.assertRaises(ArtifactVerificationError):
            parse_certificate_digest("Verified")
        with self.assertRaises(ArtifactVerificationError):
            normalized_digest("short", "fixture")

    def test_checksum_round_trip_and_mismatch(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            apk = root / "minibile-v0.0.0-arm64-v8a.apk"
            checksum = root / "minibile-v0.0.0-arm64-v8a.apk.sha256"
            apk.write_bytes(b"apk fixture")
            digest = sha256(apk)

            write_checksum(checksum, apk, digest)
            validate_checksum(checksum, apk, digest)
            with self.assertRaises(ArtifactVerificationError):
                validate_checksum(checksum, apk, "0" * 64)


if __name__ == "__main__":
    unittest.main()