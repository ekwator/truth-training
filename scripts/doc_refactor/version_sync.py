"""Version synchronization helpers."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import List, Optional

from .models import DocumentationFile

TARGET_VERSION = "v1.0.0"
DEFAULT_REPORT_DIR = Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor"))


def run_version_sync(
    root: Path,
    records: List[DocumentationFile],
    *,
    dry_run: bool = False,
    report_dir: Optional[Path] = None,
) -> dict:
    report_directory = report_dir or DEFAULT_REPORT_DIR
    report_directory.mkdir(parents=True, exist_ok=True)

    updated: List[str] = []
    for record in records:
        path = record.path
        text = _read_text(path)
        if TARGET_VERSION in text:
            record.version = TARGET_VERSION
            continue
        new_text = text.rstrip() + f"\n\n_Version: {TARGET_VERSION}_\n"
        if not dry_run:
            path.write_text(new_text, encoding="utf-8")
        record.version = TARGET_VERSION
        updated.append(str(path.relative_to(root)))

    payload = {"updated": updated, "version": TARGET_VERSION}
    (report_directory / "version_sync.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    return payload


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")

