"""Contract tests for the doc_refactor CLI."""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path


def run_cli(repo_root: Path, fixture_root: Path, phases: str) -> subprocess.CompletedProcess[str]:
    env = os.environ.copy()
    report_dir = fixture_root / "reports"
    env["DOC_REFACTOR_REPORT_DIR"] = str(report_dir)
    cmd = [
        sys.executable,
        str(repo_root / "scripts/doc_refactor/main.py"),
        "run",
        "--root",
        str(fixture_root),
        "--phases",
        phases,
        "--dry-run",
    ]
    return subprocess.run(cmd, cwd=repo_root, env=env, capture_output=True, text=True)


def test_cli_contract_phases(repo_root: Path, tmp_repo: Path) -> None:
    phases = "inventory,link_discovery,validation"
    result = run_cli(repo_root, tmp_repo, phases)
    assert result.returncode == 0, result.stderr

    report_dir = tmp_repo / "reports"
    for filename in ("inventory.json", "link_report.json", "validation.json"):
        assert (report_dir / filename).exists(), f"expected {filename} in {report_dir}"

    run_artifacts = report_dir / "run_artifacts"
    for phase in ["inventory", "link_discovery", "validation"]:
        assert (run_artifacts / f"{phase}.json").exists(), f"expected artifact for {phase}"

