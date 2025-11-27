"""Inventory integration tests covering depth and exclusions."""

from __future__ import annotations

from pathlib import Path

from scripts.doc_refactor import inventory


def _build_layout(tmp_path: Path) -> Path:
    (tmp_path / "docs" / "archive").mkdir(parents=True)
    (tmp_path / "nested" / "deep" / "tree").mkdir(parents=True)

    (tmp_path / "README.md").write_text("# Root\n", encoding="utf-8")
    (tmp_path / "docs" / "README.md").write_text("# Docs Index\n", encoding="utf-8")
    (tmp_path / "docs" / "guide.md").write_text("# Guide\n", encoding="utf-8")
    (tmp_path / "docs" / "archive" / "legacy.md").write_text("# Legacy\n", encoding="utf-8")
    (tmp_path / "nested" / "deep" / "tree" / "detail.md").write_text("# Deep Detail\n", encoding="utf-8")
    (tmp_path / "CONTRIBUTING.md").write_text("skip me", encoding="utf-8")
    return tmp_path


def test_inventory_depth_and_roles(tmp_path: Path) -> None:
    root = _build_layout(tmp_path)
    records = inventory.generate_inventory(root=root, save_report=False)

    readme = next(record for record in records if record.path == root / "README.md")
    assert readme.role == "README"
    assert readme.depth == 0

    docs_index = next(record for record in records if record.path == root / "docs" / "README.md")
    assert docs_index.role == "INDEX"

    archive = next(record for record in records if record.path == root / "docs" / "archive" / "legacy.md")
    assert archive.role == "ARCHIVE"

    deep_detail = next(record for record in records if record.path.name == "detail.md")
    assert deep_detail.role == "DETAIL"
    assert deep_detail.depth == 3  # capped at max depth

    excluded_names = {record.path.name for record in records}
    assert "CONTRIBUTING.md" not in excluded_names

