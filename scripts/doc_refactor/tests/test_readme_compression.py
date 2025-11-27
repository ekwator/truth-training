"""Scenario 3: README compression and content relocation."""

from __future__ import annotations

from pathlib import Path

from scripts.doc_refactor import inventory, link_graph, restructuring


def _seed_readme(tmp_path: Path) -> None:
    (tmp_path / "docs").mkdir(exist_ok=True)
    summary = "Truth Training aligns networks via anonymous trust.\n"
    detail = "\n".join(["This section contains release information and deep detail."] * 50)
    text = f"{summary}\n## Release Information\n\n{detail}\n"
    (tmp_path / "README.md").write_text(text, encoding="utf-8")


def test_readme_compression_creates_docs_and_limits_word_count(tmp_path: Path) -> None:
    _seed_readme(tmp_path)
    records = inventory.generate_inventory(root=tmp_path, save_report=False)
    graph = link_graph.build_link_graph(tmp_path, records)
    report_dir = tmp_path / "reports"

    restructure_report = restructuring.run_restructuring(
        root=tmp_path,
        records=records,
        link_report=graph,
        report_dir=report_dir,
        dry_run=False,
    )

    readme_text = (tmp_path / "README.md").read_text(encoding="utf-8")
    word_count = len(readme_text.split())
    assert word_count <= 700
    assert "## Documentation" in readme_text

    section_doc = tmp_path / "docs" / "release-information.md"
    assert section_doc.exists()
    doc_text = section_doc.read_text(encoding="utf-8")
    assert "Release Information" in doc_text

    assert any(action["target"] == "docs/release-information.md" for action in restructure_report["moved_sections"])
    assert (report_dir / "restructure.json").exists()

