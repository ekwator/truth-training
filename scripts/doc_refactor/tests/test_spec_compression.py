"""Scenario 5: Spec compression enforcement."""

from __future__ import annotations

import json
from pathlib import Path

from scripts.doc_refactor import duplicate_detector, inventory, spec_optimizer


def _seed_spec(tmp_path: Path) -> None:
    spec_dir = tmp_path / "spec"
    spec_dir.mkdir(parents=True, exist_ok=True)
    docs_dir = tmp_path / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)
    (docs_dir / "overview.md").write_text("# Overview\n", encoding="utf-8")

    long_paragraph = " ".join(["context"] * 120)
    spec_path = spec_dir / "01-long.md"
    spec_path.write_text(f"# Long Spec\n\n## Goals\n\n{long_paragraph}\n", encoding="utf-8")

    spec_readme = spec_dir / "README.md"
    spec_readme.write_text("# Specs\n\nReference list.\n", encoding="utf-8")


def test_spec_compression_enforces_word_limit_and_directive(tmp_path: Path) -> None:
    _seed_spec(tmp_path)
    records = inventory.generate_inventory(root=tmp_path, save_report=False)
    report_dir = tmp_path / "reports"

    spec_optimizer.run_spec_optimizer(
        root=tmp_path,
        records=records,
        report_dir=report_dir,
        dry_run=False,
    )

    spec_path = tmp_path / "spec" / "01-long.md"
    text = spec_path.read_text(encoding="utf-8")
    paragraphs = [p for p in text.split("\n\n") if p and not p.startswith("#")]
    assert paragraphs, "expected paragraphs after compression"
    assert all(len(paragraph.split()) <= 80 for paragraph in paragraphs)
    assert "Use /spec as the primary decision source before reading /docs." in text

    spec_readme = (tmp_path / "spec" / "README.md").read_text(encoding="utf-8")
    assert "Use /spec as the primary decision source before reading /docs." in spec_readme

    spec_opt_report = report_dir / "spec_opt.json"
    assert spec_opt_report.exists()


def test_spec_optimizer_removes_duplicates_flagged_in_report(tmp_path: Path) -> None:
    spec_dir = tmp_path / "spec"
    docs_dir = tmp_path / "docs"
    spec_dir.mkdir(parents=True, exist_ok=True)
    docs_dir.mkdir(parents=True, exist_ok=True)

    duplicate_block = " ".join(["paragraph"] * 35)
    (docs_dir / "tutorial.md").write_text(f"# Tutorial\n\n{duplicate_block}\n", encoding="utf-8")
    spec_path = spec_dir / "02-duplicate.md"
    spec_path.write_text(f"# Spec\n\n{duplicate_block}\n", encoding="utf-8")
    (spec_dir / "README.md").write_text("# Specs\n", encoding="utf-8")

    records = inventory.generate_inventory(root=tmp_path, save_report=False)
    report_dir = tmp_path / "reports"
    duplicate_detector.run_duplicate_detection(root=tmp_path, records=records, report_dir=report_dir)
    spec_optimizer.run_spec_optimizer(
        root=tmp_path,
        records=records,
        report_dir=report_dir,
        dry_run=False,
    )

    text = spec_path.read_text(encoding="utf-8")
    assert duplicate_block not in text

    spec_opt_report = json.loads((report_dir / "spec_opt.json").read_text(encoding="utf-8"))
    assert spec_opt_report["duplicate_removals"]

