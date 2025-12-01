"""Validation utilities for doc_refactor."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Optional

from .links import ReferenceEdge
from .models import DocumentationFile, LinkGraphReport


def run_validation(
    root: Path,
    records: List[DocumentationFile],
    link_report: LinkGraphReport,
    *,
    dry_run: bool = False,
    report_dir: Optional[Path] = None,
) -> Dict[str, List[dict]]:
    """Aggregate link graph results into a validation report."""

    del dry_run  # validation is read-only; parameter kept for compatibility

    audience_map = {str(record.path.relative_to(root)): record.audience for record in records}
    missing_internal = _collect_edges(link_report.edges, status="missing")
    external_warnings = _collect_edges(link_report.edges, status="external_warning")
    summary = {
        "missing_internal": missing_internal,
        "external_warnings": external_warnings,
        "orphans": [
            {"path": path, "audience": audience_map.get(path, "unknown")}
            for path in link_report.orphans
        ],
        "plain_paths": link_report.plain_paths,
        "normalizations": link_report.normalizations,
        "stats": link_report.stats,
    }

    if report_dir:
        report_dir.mkdir(parents=True, exist_ok=True)
        (report_dir / "validation.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")

    return summary


def _collect_edges(edges: List[ReferenceEdge], *, status: str) -> List[dict]:
    collected = []
    for edge in edges:
        if edge.status != status:
            continue
        collected.append(
            {
                "source": edge.source_path,
                "target": edge.target,
                "status": edge.status,
                "is_external": edge.is_external,
            }
        )
    return collected



