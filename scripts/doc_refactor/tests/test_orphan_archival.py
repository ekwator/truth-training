"""Scenario 4: orphan remediation and archival."""

from __future__ import annotations

from pathlib import Path

from scripts.doc_refactor import inventory, link_graph, restructuring
from scripts.doc_refactor.models import LinkGraphReport


def _seed_orphan(tmp_path: Path) -> None:
    docs_dir = tmp_path / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)
    (docs_dir / "README.md").write_text("# Docs\n", encoding="utf-8")
    (docs_dir / "orphan.md").write_text("# Orphan\n\nLegacy content predating v1.0.0\n", encoding="utf-8")
    (tmp_path / "README.md").write_text("# Root\n", encoding="utf-8")


def test_orphan_archival_moves_files_and_updates_index(tmp_path: Path) -> None:
    _seed_orphan(tmp_path)
    records = inventory.generate_inventory(root=tmp_path, save_report=False)
    for record in records:
        if record.path.name == "orphan.md":
            record.flags["needs_archive"] = True

    graph = LinkGraphReport()
    graph.orphans = ["docs/orphan.md"]

    report_dir = tmp_path / "reports"
    restructuring.run_restructuring(
        root=tmp_path,
        records=records,
        link_report=graph,
        report_dir=report_dir,
        dry_run=False,
    )

    archived_path = tmp_path / "docs" / "archive" / "orphan.md"
    assert archived_path.exists()
    assert not (tmp_path / "docs" / "orphan.md").exists()

    archive_index = (tmp_path / "docs" / "archive" / "README.md").read_text(encoding="utf-8")
    assert "orphan.md" in archive_index

    docs_readme = (tmp_path / "docs" / "README.md").read_text(encoding="utf-8")
    assert "docs/archive/orphan.md" in docs_readme

