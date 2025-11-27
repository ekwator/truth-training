"""Scenario 6: End-to-end run summary validation."""

from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path


def _seed_repo(root: Path) -> None:
    (root / "docs").mkdir(parents=True, exist_ok=True)
    (root / "spec").mkdir(exist_ok=True)
    (root / "README.md").write_text("# Sample Repo\n\n## Release Information\n\nDetails.\n", encoding="utf-8")
    (root / "docs" / "README.md").write_text("# Docs\n", encoding="utf-8")
    (root / "spec" / "README.md").write_text("# Specs\n", encoding="utf-8")
    (root / "spec" / "01-overview.md").write_text("# Overview\n\nLong paragraph " + "word " * 120, encoding="utf-8")


def test_full_run_generates_run_summary(tmp_path: Path, repo_root: Path) -> None:
    _seed_repo(tmp_path)
    env = os.environ.copy()
    report_dir = tmp_path / "reports/doc_refactor"
    env["DOC_REFACTOR_REPORT_DIR"] = str(report_dir)
    cmd = [
        sys.executable,
        str(repo_root / "scripts/doc_refactor/main.py"),
        "run",
        "--root",
        str(tmp_path),
        "--phases",
        "all",
    ]
    result = subprocess.run(cmd, cwd=repo_root, env=env, capture_output=True, text=True)
    assert result.returncode == 0, result.stderr

    summary_path = report_dir / "run_summary.json"
    assert summary_path.exists()
    summary = json.loads(summary_path.read_text(encoding="utf-8"))
    assert summary["broken_links"] == 0
    assert summary["coverage_ratio"] == 100
    assert "spec_opt" in summary["phases"]

