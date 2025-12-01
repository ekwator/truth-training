"""Duplicate paragraph detection between docs and spec."""

from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

from .models import DocumentationFile

DEFAULT_REPORT_DIR = Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor"))
MIN_WORDS = 30


def run_duplicate_detection(
    root: Path,
    records: List[DocumentationFile],
    *,
    dry_run: bool = False,
    report_dir: Optional[Path] = None,
    threshold: float = 0.8,  # kept for backward compatibility, unused
) -> dict:
    del dry_run
    del threshold

    report_directory = report_dir or DEFAULT_REPORT_DIR
    report_directory.mkdir(parents=True, exist_ok=True)

    blocks = _collect_blocks(root, records)
    actions = []
    for content_hash, occurrences in blocks.items():
        if len(occurrences) < 2:
            continue
        classification = _classify_occurrences(occurrences)
        actions.append(
            {
                "content_hash": content_hash,
                "classification": classification,
                "occurrences": [
                    {
                        "path": occurrence["path"],
                        "audience": occurrence["audience"],
                        "line_range": occurrence["line_range"],
                        "text": occurrence["text"],
                    }
                    for occurrence in occurrences
                ],
            }
        )

    payload = {"actions": actions}
    (report_directory / "dedupe.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    return payload


def _collect_blocks(root: Path, records: List[DocumentationFile]) -> Dict[str, List[dict]]:
    blocks: Dict[str, List[dict]] = {}
    for record in records:
        if record.audience not in {"docs", "spec"}:
            continue
        text = _read_text(record.path)
        for start, end, block_text in _iter_blocks(text):
            word_count = len(block_text.split())
            if word_count < MIN_WORDS:
                continue
            normalized = _normalize_block(block_text)
            content_hash = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
            blocks.setdefault(content_hash, []).append(
                {
                    "path": str(record.path.relative_to(root)),
                    "audience": record.audience,
                    "line_range": [start, end],
                    "text": block_text.strip(),
                }
            )
    return blocks


def _iter_blocks(text: str) -> Iterable[Tuple[int, int, str]]:
    lines = text.splitlines()
    block: List[str] = []
    start_line = 1

    for idx, line in enumerate(lines, start=1):
        if line.strip():
            if not block:
                start_line = idx
            block.append(line)
            continue
        if block:
            yield start_line, idx - 1, "\n".join(block)
            block = []
    if block:
        yield start_line, len(lines), "\n".join(block)


def _normalize_block(block: str) -> str:
    collapsed = " ".join(block.split())
    return collapsed.lower()


def _classify_occurrences(occurrences: List[dict]) -> str:
    audiences = {entry["audience"] for entry in occurrences}
    if "docs" in audiences and "spec" in audiences:
        return "needs_review"
    return "harmless"


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")

