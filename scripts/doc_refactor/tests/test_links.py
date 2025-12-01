"""Unit tests for link discovery and ReferenceEdge classification."""

from __future__ import annotations

from pathlib import Path

from scripts.doc_refactor import inventory, link_graph


def test_reference_edges_mark_missing_targets(tmp_path: Path) -> None:
    (tmp_path / "docs").mkdir()
    (tmp_path / "README.md").write_text(
        "# Root\n\n- [Existing](docs/existing.md)\n- [Missing](docs/missing.md)\n",
        encoding="utf-8",
    )
    (tmp_path / "docs" / "existing.md").write_text("# Existing\n", encoding="utf-8")

    records = inventory.generate_inventory(root=tmp_path, save_report=False)
    graph = link_graph.build_link_graph(root=tmp_path, records=records)

    status_map = {edge.target: edge.status for edge in graph.edges}
    assert status_map["docs/existing.md"] == "ok"
    assert status_map["docs/missing.md"] == "missing"
    assert any(entry["target"] == "docs/missing.md" for entry in graph.broken_urls)


def test_reference_edges_capture_normalizations(tmp_path: Path) -> None:
    (tmp_path / "README.md").write_text("See docs/release.md for notes.", encoding="utf-8")
    (tmp_path / "docs").mkdir()
    (tmp_path / "docs" / "release.md").write_text("# Release\n", encoding="utf-8")

    records = inventory.generate_inventory(root=tmp_path, save_report=False)
    graph = link_graph.build_link_graph(root=tmp_path, records=records)

    readme_text = (tmp_path / "README.md").read_text(encoding="utf-8")
    assert "[docs/release.md](docs/release.md)" in readme_text
    assert graph.normalizations

