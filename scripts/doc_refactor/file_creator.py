"""File creation helpers for doc_refactor."""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

LEDGER_FILENAME = "file_creation.json"


@dataclass
class CreationRecord:
    relative_path: str
    template_name: str

    def to_dict(self) -> dict:
        return {"relative_path": self.relative_path, "template": self.template_name}


def create_placeholder(
    root: Path,
    relative_path: str,
    template_contents: str,
    *,
    template_name: str,
    report_dir: Path,
    dry_run: bool = False,
) -> None:
    """Create a placeholder file and record it in the ledger."""

    absolute_path = root / relative_path
    if not dry_run:
        absolute_path.parent.mkdir(parents=True, exist_ok=True)
        absolute_path.write_text(template_contents, encoding="utf-8")
    _append_to_ledger(report_dir, CreationRecord(relative_path=relative_path, template_name=template_name))


def _append_to_ledger(report_dir: Path, record: CreationRecord) -> None:
    report_dir.mkdir(parents=True, exist_ok=True)
    ledger_path = report_dir / LEDGER_FILENAME
    existing: List[dict] = []

    if ledger_path.exists():
        try:
            loaded = json.loads(ledger_path.read_text(encoding="utf-8"))
            if isinstance(loaded, list):
                existing = loaded
            elif isinstance(loaded, dict):
                existing = [loaded]
            else:
                existing = []
        except json.JSONDecodeError:
            existing = []
    existing.append(record.to_dict())
    ledger_path.write_text(json.dumps(existing, indent=2), encoding="utf-8")


def archive_document(
    root: Path,
    source_rel: str,
    *,
    dest_rel: Optional[str] = None,
    report_dir: Path,
    dry_run: bool = False,
) -> Optional[str]:
    """Move a document into docs/archive and log the action."""

    source_path = root / source_rel
    if not source_path.exists():
        return None

    resolved_dest = Path(dest_rel) if dest_rel else Path("docs") / "archive" / Path(source_rel)
    destination = root / resolved_dest

    if not dry_run:
        destination.parent.mkdir(parents=True, exist_ok=True)
        header = f"<!-- Archived from {source_rel} -->\n"
        body = source_path.read_text(encoding="utf-8")
        destination.write_text(f"{header}\n{body}\n", encoding="utf-8")
        source_path.unlink()

    _append_to_ledger(report_dir, CreationRecord(relative_path=str(resolved_dest), template_name="archive_move"))
    return str(resolved_dest)

