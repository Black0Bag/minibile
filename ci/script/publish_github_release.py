#!/usr/bin/env python3
"""Publish one immutable GitHub Release for the current repository version."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from release_version import ReleaseVersion, VersionContractError, read_worktree_version, resolve_commit


class ReleasePublicationError(ValueError):
    """Raised when an existing or new release violates the immutable release contract."""


@dataclass(frozen=True)
class LocalAsset:
    path: Path
    size: int
    sha256: str

    @classmethod
    def load(cls, path: Path) -> "LocalAsset":
        if not path.is_file() or path.stat().st_size == 0:
            raise ReleasePublicationError(f"release asset is missing or empty: {path}")
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        return cls(path=path, size=path.stat().st_size, sha256=digest.hexdigest())


def run(
    *command: str,
    check: bool = True,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        check=check,
        capture_output=True,
        text=True,
    )


def gh_json(*arguments: str, check: bool = True) -> Any:
    result = run("gh", *arguments, check=check)
    if result.returncode != 0:
        return None
    return json.loads(result.stdout)


def ensure_annotated_tag(version: ReleaseVersion, notes_file: Path) -> str:
    head = resolve_commit("HEAD")
    run("git", "fetch", "--force", "--tags", "origin")
    reference = f"refs/tags/{version.tag}"
    exists = run("git", "show-ref", "--verify", "--quiet", reference, check=False).returncode == 0
    if exists:
        object_type = run("git", "cat-file", "-t", version.tag).stdout.strip()
        if object_type != "tag":
            raise ReleasePublicationError(
                f"{version.tag} exists but is a {object_type} object; an annotated tag is required"
            )
        tagged_commit = resolve_commit(version.tag)
        if tagged_commit != head:
            raise ReleasePublicationError(
                f"{version.tag} points to {tagged_commit}, current HEAD is {head}"
            )
        return head

    run("git", "config", "user.name", "github-actions[bot]")
    run("git", "config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
    run("git", "tag", "-a", version.tag, "-F", str(notes_file), head)
    run("git", "push", "origin", reference)
    if resolve_commit(version.tag) != head:
        raise ReleasePublicationError(f"new tag {version.tag} does not resolve to {head}")
    return head


def release_view(repository: str, tag: str) -> dict[str, Any] | None:
    result = run(
        "gh",
        "release",
        "view",
        tag,
        "--repo",
        repository,
        "--json",
        "tagName,name,body,isDraft,url,assets",
        check=False,
    )
    if result.returncode != 0:
        return None
    value = json.loads(result.stdout)
    if not isinstance(value, dict):
        raise ReleasePublicationError(f"unexpected release response for {tag}")
    return value


def normalized_remote_digest(value: object) -> str | None:
    if not isinstance(value, str) or not value.startswith("sha256:"):
        return None
    digest = value.removeprefix("sha256:").lower()
    return digest if len(digest) == 64 else None


def download_asset_digest(repository: str, tag: str, asset_name: str) -> str:
    with tempfile.TemporaryDirectory() as directory:
        destination = Path(directory)
        run(
            "gh",
            "release",
            "download",
            tag,
            "--repo",
            repository,
            "--pattern",
            asset_name,
            "--dir",
            str(destination),
        )
        downloaded = destination / asset_name
        return LocalAsset.load(downloaded).sha256


def verify_remote_asset(
    repository: str,
    tag: str,
    remote: dict[str, Any],
    local: LocalAsset,
) -> None:
    if remote.get("name") != local.path.name:
        raise ReleasePublicationError(
            f"remote asset name is {remote.get('name')!r}, expected {local.path.name!r}"
        )
    if remote.get("size") != local.size:
        raise ReleasePublicationError(
            f"remote asset {local.path.name} size is {remote.get('size')}, expected {local.size}"
        )
    remote_digest = normalized_remote_digest(remote.get("digest"))
    if remote_digest is None:
        remote_digest = download_asset_digest(repository, tag, local.path.name)
    if remote_digest != local.sha256:
        raise ReleasePublicationError(
            f"remote asset {local.path.name} SHA-256 is {remote_digest}, expected {local.sha256}"
        )


def verify_release_metadata(
    release: dict[str, Any],
    version: ReleaseVersion,
    notes: str,
) -> None:
    if release.get("tagName") != version.tag:
        raise ReleasePublicationError(
            f"release tag is {release.get('tagName')!r}, expected {version.tag!r}"
        )
    if release.get("name") != version.tag:
        raise ReleasePublicationError(
            f"release name is {release.get('name')!r}, expected {version.tag!r}"
        )
    if str(release.get("body", "")).strip() != notes.strip():
        raise ReleasePublicationError(
            f"release notes for {version.tag} do not match the current CHANGELOG section"
        )


def ensure_release(
    repository: str,
    version: ReleaseVersion,
    notes_file: Path,
    assets: list[LocalAsset],
) -> dict[str, Any]:
    notes = notes_file.read_text(encoding="utf-8")
    release = release_view(repository, version.tag)
    if release is None:
        run(
            "gh",
            "release",
            "create",
            version.tag,
            "--repo",
            repository,
            "--title",
            version.tag,
            "--notes-file",
            str(notes_file),
            "--draft",
            "--verify-tag",
        )
        release = release_view(repository, version.tag)
        if release is None:
            raise ReleasePublicationError(f"draft release {version.tag} was not created")

    verify_release_metadata(release, version, notes)
    is_draft = release.get("isDraft") is True
    remote_assets = {
        asset.get("name"): asset
        for asset in release.get("assets", [])
        if isinstance(asset, dict) and isinstance(asset.get("name"), str)
    }
    expected_names = {asset.path.name for asset in assets}
    unexpected_names = set(remote_assets) - expected_names
    if unexpected_names:
        raise ReleasePublicationError(
            f"release {version.tag} contains unexpected assets: {sorted(unexpected_names)}"
        )

    for local in assets:
        remote = remote_assets.get(local.path.name)
        if remote is None:
            if not is_draft:
                raise ReleasePublicationError(
                    f"published release {version.tag} is missing {local.path.name}"
                )
            run(
                "gh",
                "release",
                "upload",
                version.tag,
                str(local.path),
                "--repo",
                repository,
            )
        else:
            verify_remote_asset(repository, version.tag, remote, local)

    release = release_view(repository, version.tag)
    if release is None:
        raise ReleasePublicationError(f"release {version.tag} disappeared after asset upload")
    verify_release_metadata(release, version, notes)
    remote_assets = {
        asset.get("name"): asset
        for asset in release.get("assets", [])
        if isinstance(asset, dict) and isinstance(asset.get("name"), str)
    }
    if set(remote_assets) != expected_names:
        raise ReleasePublicationError(
            f"release {version.tag} assets are {sorted(remote_assets)}, expected {sorted(expected_names)}"
        )
    for local in assets:
        verify_remote_asset(repository, version.tag, remote_assets[local.path.name], local)

    if release.get("isDraft") is True:
        run(
            "gh",
            "release",
            "edit",
            version.tag,
            "--repo",
            repository,
            "--draft=false",
            "--latest",
        )
        release = release_view(repository, version.tag)
        if release is None or release.get("isDraft") is True:
            raise ReleasePublicationError(f"release {version.tag} was not published")
        verify_release_metadata(release, version, notes)
    return release


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--root", type=Path, default=Path.cwd())
    result.add_argument("--repository", required=True)
    result.add_argument("--notes-file", type=Path, required=True)
    result.add_argument("--asset", type=Path, action="append", required=True)
    return result


def main() -> int:
    args = parser().parse_args()
    try:
        version = read_worktree_version(args.root)
        assets = [LocalAsset.load(path) for path in args.asset]
        names = [asset.path.name for asset in assets]
        if len(set(names)) != len(names):
            raise ReleasePublicationError(f"duplicate local release asset names: {names}")
        ensure_annotated_tag(version, args.notes_file)
        release = ensure_release(
            args.repository,
            version,
            args.notes_file,
            assets,
        )
        print(f"Published immutable release {version.tag}: {release.get('url')}")
        return 0
    except (
        OSError,
        subprocess.CalledProcessError,
        json.JSONDecodeError,
        VersionContractError,
        ReleasePublicationError,
    ) as error:
        if os.environ.get("GITHUB_ACTIONS") == "true":
            print(f"::error title=GitHub release publication::{error}", file=sys.stderr)
        else:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())