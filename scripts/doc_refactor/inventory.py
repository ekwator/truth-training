"""Markdown inventory generation for the documentation refactor CLI."""

from __future__ import annotations

import json
import os
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple

from .models import DocumentationFile

EXCLUDED_NAMES = {"CONTRIBUTING.md", "LICENSE.txt", "SECURITY.md", "CHANGELOG.md"}
EXCLUDED_DIRS = {".git", ".cursor", ".venv", "node_modules", "target", "reports"}
DEFAULT_REPORT_DIR = "reports/doc_refactor"
INVENTORY_FILENAME = "inventory.json"


def generate_inventory(root: Path | str = ".", save_report: bool = True) -> List[DocumentationFile]:
    """Walk the repository and create DocumentationFile records."""

    root_path = Path(root).resolve()
    markdown_files = sorted(_discover_markdown_files(root_path), key=lambda p: str(p))
    records: List[DocumentationFile] = []

    for path in markdown_files:
        if _should_exclude(root_path, path):
            continue
        depth = _calculate_depth(root_path, path)
        role = _classify_role(root_path, path)
        slug = _slugify(root_path, path)
        word_count = _word_count(path)
        records.append(
            DocumentationFile(
                path=path,
                slug=slug,
                depth=0 if role == "README" else depth,
                role=role,
                audience=_audience(root_path, path),
                version_tag=_infer_version_tag(root_path, path),
                word_count=word_count,
                is_excluded=path.name in EXCLUDED_NAMES,
            )
        )

    if save_report:
        _write_inventory_report(root_path, records)

    return records


def _discover_markdown_files(root: Path) -> Iterable[Path]:
    return root.rglob("*.md")


def _should_exclude(root: Path, path: Path) -> bool:
    if path.name in EXCLUDED_NAMES and path.parent == root:
        return True
    relative = path.relative_to(root)
    if any(part in EXCLUDED_DIRS for part in relative.parts):
        return True
    return False


def _calculate_depth(root: Path, path: Path) -> int:
    relative = path.relative_to(root)
    segments = len(relative.parts)
    return min(segments, 3)


def _classify_role(root: Path, path: Path) -> str:
    relative = path.relative_to(root)
    parts = [part.lower() for part in relative.parts]
    name = path.name.lower()

    if parts and parts[0] == "docs":
        if len(parts) >= 2 and parts[1] == "archive":
            return "ARCHIVE"
        if name in {"readme.md", "index.md"}:
            return "INDEX"
    if parts and parts[0] == "spec":
        return "SPEC"
    if name.startswith("readme"):
        return "README"
    return "DETAIL"


def _audience(root: Path, path: Path) -> str:
    relative = path.relative_to(root)
    if not relative.parts:
        return "root"
    first = relative.parts[0].lower()
    if first == "docs":
        return "docs"
    if first == "spec":
        return "spec"
    return "root" if len(relative.parts) == 1 else "other"


def _infer_version_tag(root: Path, path: Path) -> str:
    relative = path.relative_to(root)
    if any(part.lower() == "archive" for part in relative.parts):
        return "legacy"
    if relative.parts and relative.parts[0].lower() == "specs":
        return "legacy"
    return "v1.0.0"


def _slugify(root: Path, path: Path) -> str:
    relative = path.relative_to(root).with_suffix("")
    parts = [part.replace(" ", "-").lower() for part in relative.parts]
    slug = "-".join(parts)
    return slug or "readme"


def _word_count(path: Path) -> int:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        text = path.read_text(encoding="latin-1")
    return len(text.split())


def _write_inventory_report(root: Path, records: Sequence[DocumentationFile]) -> None:
    report_dir = _report_dir()
    report_dir.mkdir(parents=True, exist_ok=True)
    inventory_path = report_dir / INVENTORY_FILENAME
    data = {
        "root": str(root),
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "files": [_serializable_record(record) for record in records],
        "exclusions": sorted(EXCLUDED_NAMES),
    }
    inventory_path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def _serializable_record(record: DocumentationFile) -> dict:
    payload = asdict(record)
    payload["path"] = str(record.path)
    return payload


def _report_dir() -> Path:
    return Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", DEFAULT_REPORT_DIR)).resolve()

