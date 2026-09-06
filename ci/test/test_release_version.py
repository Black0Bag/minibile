from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "ci" / "script"))

from release_version import (  # noqa: E402
    ReleaseVersion,
    VersionContractError,
    changelog_section,
    validate_current,
    validate_pull_request,
)


def git(repository: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=repository,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def write_release(repository: Path, version: str) -> None:
    (repository / "VERSION").write_text(f"{version}\n", encoding="utf-8")
    (repository / "CHANGELOG.md").write_text(
        f"# Changelog\n\n## {version} - 2026-09-05\n\n### Changes\n\n- Release {version}.\n",
        encoding="utf-8",
    )


class ReleaseVersionTest(unittest.TestCase):
    def test_release_workflow_is_limited_to_main_and_minimal_write_scope(self) -> None:
        workflow = (REPO_ROOT / ".github/workflows/android-release.yml").read_text(
            encoding="utf-8"
        )

        self.assertIn('RELEASE_REF: ${{ github.ref }}', workflow)
        self.assertIn('test "$RELEASE_REF" = "refs/heads/main"', workflow)
        self.assertEqual(workflow.count("contents: write"), 1)
        self.assertIn("publish:", workflow)
        self.assertIn("contents: read", workflow)

    def test_bootstrap_version_mapping(self) -> None:
        version = ReleaseVersion.parse("0.0.0\n")

        self.assertEqual(version.version_code, 1)
        self.assertEqual(version.tag, "v0.0.0")
        self.assertEqual(version.apk_name, "minibile-v0.0.0-arm64-v8a.apk")

    def test_invalid_semver_is_rejected(self) -> None:
        for value in ("v0.0.0", "0.0", "01.0.0", "0.0.0-alpha", "0.1000.0"):
            with self.subTest(value=value), self.assertRaises(VersionContractError):
                ReleaseVersion.parse(value)

    def test_changelog_requires_one_nonempty_section(self) -> None:
        version = ReleaseVersion.parse("0.1.0")
        section = changelog_section(
            "# Log\n\n## 0.1.0 - 2026-09-05\n\n### Added\n\n- Item\n\n## 0.0.0 - 2026-09-04\n\n- Base\n",
            version,
        )

        self.assertIn("- Item", section)
        self.assertNotIn("0.0.0", section)
        with self.assertRaises(VersionContractError):
            changelog_section("## 0.1.0 - 2026-09-05\n\n### Added\n", version)

    def test_pull_request_bootstrap_requires_zero_version(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            git(repository, "init", "-b", "main")
            git(repository, "config", "user.name", "CI Test")
            git(repository, "config", "user.email", "ci@example.com")
            (repository / "README.md").write_text("base\n", encoding="utf-8")
            git(repository, "add", "README.md")
            git(repository, "commit", "-m", "base")
            base = git(repository, "rev-parse", "HEAD")
            write_release(repository, "0.0.0")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "bootstrap")
            candidate = git(repository, "rev-parse", "HEAD")

            previous = Path.cwd()
            try:
                os.chdir(repository)
                self.assertEqual(validate_pull_request(base, candidate), ReleaseVersion(0, 0, 0))
            finally:
                os.chdir(previous)
    def test_bootstrap_pull_request_allows_strict_fix_versions_after_zero(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            git(repository, "init", "-b", "main")
            git(repository, "config", "user.name", "CI Test")
            git(repository, "config", "user.email", "ci@example.com")
            (repository / "README.md").write_text("base\n", encoding="utf-8")
            git(repository, "add", "README.md")
            git(repository, "commit", "-m", "base")
            base = git(repository, "rev-parse", "HEAD")
            write_release(repository, "0.0.0")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "bootstrap candidate")
            write_release(repository, "0.0.1")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "bootstrap fix")
            candidate = git(repository, "rev-parse", "HEAD")

            previous = Path.cwd()
            try:
                os.chdir(repository)
                self.assertEqual(validate_pull_request(base, candidate), ReleaseVersion(0, 0, 1))
                self.assertEqual(
                    validate_current(repository, allow_existing_tag_at_head=False, base=base),
                    ReleaseVersion(0, 0, 1),
                )
            finally:
                os.chdir(previous)

    def test_bootstrap_pull_request_rejects_skipping_zero(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            git(repository, "init", "-b", "main")
            git(repository, "config", "user.name", "CI Test")
            git(repository, "config", "user.email", "ci@example.com")
            (repository / "README.md").write_text("base\n", encoding="utf-8")
            git(repository, "add", "README.md")
            git(repository, "commit", "-m", "base")
            base = git(repository, "rev-parse", "HEAD")
            write_release(repository, "0.0.1")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "invalid bootstrap")
            candidate = git(repository, "rev-parse", "HEAD")

            previous = Path.cwd()
            try:
                os.chdir(repository)
                with self.assertRaises(VersionContractError):
                    validate_pull_request(base, candidate)
            finally:
                os.chdir(previous)

    def test_pull_request_requires_strict_increment_and_unused_tag(self) -> None:

        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            git(repository, "init", "-b", "main")
            git(repository, "config", "user.name", "CI Test")
            git(repository, "config", "user.email", "ci@example.com")
            write_release(repository, "0.0.0")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "base")
            base = git(repository, "rev-parse", "HEAD")
            git(repository, "tag", "-a", "v0.0.0", "-m", "base")
            write_release(repository, "0.0.1")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "next")
            candidate = git(repository, "rev-parse", "HEAD")

            previous = Path.cwd()
            try:
                os.chdir(repository)
                self.assertEqual(validate_pull_request(base, candidate), ReleaseVersion(0, 0, 1))
                git(repository, "tag", "-a", "v0.0.1", base, "-m", "conflict")
                with self.assertRaises(VersionContractError):
                    validate_pull_request(base, candidate)
            finally:
                os.chdir(previous)

    def test_current_version_must_exceed_pushed_base(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            git(repository, "init", "-b", "main")
            git(repository, "config", "user.name", "CI Test")
            git(repository, "config", "user.email", "ci@example.com")
            write_release(repository, "0.0.0")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "base")
            base = git(repository, "rev-parse", "HEAD")
            (repository / "README.md").write_text("changed\n", encoding="utf-8")
            git(repository, "add", "README.md")
            git(repository, "commit", "-m", "change without bump")

            previous = Path.cwd()
            try:
                os.chdir(repository)
                with self.assertRaises(VersionContractError):
                    validate_current(repository, allow_existing_tag_at_head=False, base=base)
                write_release(repository, "0.0.1")
                self.assertEqual(
                    validate_current(repository, allow_existing_tag_at_head=False, base=base),
                    ReleaseVersion(0, 0, 1),
                )
            finally:
                os.chdir(previous)

    def test_current_version_allows_only_same_head_tag_during_recovery(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            git(repository, "init", "-b", "main")
            git(repository, "config", "user.name", "CI Test")
            git(repository, "config", "user.email", "ci@example.com")
            write_release(repository, "0.0.0")
            git(repository, "add", "VERSION", "CHANGELOG.md")
            git(repository, "commit", "-m", "release")
            git(repository, "tag", "-a", "v0.0.0", "-m", "release")

            previous = Path.cwd()
            try:
                os.chdir(repository)
                with self.assertRaises(VersionContractError):
                    validate_current(repository, allow_existing_tag_at_head=False)
                self.assertEqual(
                    validate_current(repository, allow_existing_tag_at_head=True),
                    ReleaseVersion(0, 0, 0),
                )
            finally:
                os.chdir(previous)


if __name__ == "__main__":
    unittest.main()
