"""Compatibility module delegating to the new links subsystem."""

from __future__ import annotations

from pathlib import Path
from typing import List

from .links import ReferenceEdge, build_link_report
from .models import DocumentationFile, LinkGraphReport

__all__ = ["build_link_graph", "ReferenceEdge"]


def build_link_graph(root: Path, records: List[DocumentationFile], *, dry_run: bool = False) -> LinkGraphReport:
    """Backward-compatible wrapper used by the CLI."""

    return build_link_report(root=root, records=records, dry_run=dry_run)

