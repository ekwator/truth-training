"""Validation tests for plaintext path conversion and URL categorization."""

from __future__ import annotations

from pathlib import Path

from scripts.doc_refactor import inventory, link_graph, validation
from scripts.doc_refactor import links as links_module


def _prepare_plaintext_fixture(tmp_path: Path) -> Path:
    (tmp_path / "spec").mkdir()
    (tmp_path / "README.md").write_text(
        "# Title\n\nspec/01-product-vision.md\n\nSee http://broken.local/doc\n",
        encoding="utf-8",
    )
    (tmp_path / "spec" / "01-product-vision.md").write_text("# Spec\n", encoding="utf-8")
    return tmp_path


def test_plain_paths_rewritten_and_urls_categorized(tmp_path: Path, monkeypatch) -> None:
    root = _prepare_plaintext_fixture(tmp_path)
    records = inventory.generate_inventory(root=root, save_report=False)
    monkeypatch.setattr(links_module, "_check_url", lambda url, timeout: False)
    graph = link_graph.build_link_graph(root=root, records=records)
    report = validation.run_validation(root=root, records=records, link_report=graph)

    readme_text = (root / "README.md").read_text(encoding="utf-8")
    assert "[spec/01-product-vision.md](spec/01-product-vision.md)" in readme_text
    assert report["plain_paths"] == []
    assert any(entry["status"] == "external_warning" for entry in report["external_warnings"])

