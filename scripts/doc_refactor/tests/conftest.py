"""Pytest fixtures for doc_refactor tests."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Iterator

import pytest

REPO_ROOT = Path(__file__).resolve().parents[3]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))


@pytest.fixture(scope="session")
def repo_root() -> Path:
    """Return repository root based on this file location."""

    return REPO_ROOT


@pytest.fixture
def tmp_repo(tmp_path: Path) -> Iterator[Path]:
    """Yield a temporary repository layout with a basic README/spec pair."""

    (tmp_path / "spec").mkdir(parents=True, exist_ok=True)
    (tmp_path / "docs").mkdir(parents=True, exist_ok=True)
    (tmp_path / "README.md").write_text(
        "# Sample Repo\n\nspec/01-product-vision.md\n\nhttp://invalid.localhost/doc\n",
        encoding="utf-8",
    )
    (tmp_path / "spec" / "01-product-vision.md").write_text(
        "# Vision\n\nVersion v1.0.0\n",
        encoding="utf-8",
    )
    yield tmp_path

