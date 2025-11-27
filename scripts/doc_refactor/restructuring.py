"""Restructuring helpers for README compression and archival."""

from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

from . import file_creator
from .models import DocumentationFile, LinkGraphReport

DEFAULT_REPORT_DIR = Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor"))


def run_restructuring(
    root: Path,
    records: List[DocumentationFile],
    link_report: Optional[LinkGraphReport] = None,
    *,
    report_dir: Optional[Path] = None,
    dry_run: bool = False,
) -> dict:
    link_report = link_report or LinkGraphReport()
    report_directory = report_dir or DEFAULT_REPORT_DIR
    report_directory.mkdir(parents=True, exist_ok=True)

    moved_sections = _compress_readme(root, dry_run=dry_run)
    archived_entries = _archive_documents(root, records, link_report, report_directory, dry_run=dry_run)
    _update_archive_indexes(root, [entry["destination"] for entry in archived_entries], dry_run=dry_run)

    report = {
        "moved_sections": moved_sections,
        "archived": archived_entries,
    }

    (report_directory / "restructure.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    return report


def _compress_readme(root: Path, *, dry_run: bool) -> List[dict]:
    readme_path = root / "README.md"
    if not readme_path.exists():
        return []

    text = readme_path.read_text(encoding="utf-8")
    summary, sections = _split_sections(text)
    if not sections:
        return []

    moved: List[dict] = []
    docs_dir = root / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)
    nav_lines = ["## Documentation", ""]

    for section in sections:
        slug = _slugify(section["heading"])
        target_rel = f"docs/{slug}.md"
        target_path = root / target_rel
        doc_content = _build_section_content(section)

        if not dry_run:
            target_path.parent.mkdir(parents=True, exist_ok=True)
            target_path.write_text(doc_content, encoding="utf-8")

        nav_lines.append(f"- [{section['heading']}]({target_rel})")
        moved.append(
            {
                "source": f"README.md#{slug}",
                "target": target_rel,
            }
        )

    nav_lines.append("")
    new_readme = summary.strip() + "\n\n" + "\n".join(nav_lines)
    if not dry_run:
        readme_path.write_text(new_readme.strip() + "\n", encoding="utf-8")

    return moved


def _split_sections(text: str) -> Tuple[str, List[Dict[str, str]]]:
    lines = text.splitlines()
    summary_lines: List[str] = []
    sections: List[Dict[str, str]] = []
    current: Optional[Dict[str, str]] = None
    current_lines: List[str] = []

    for line in lines:
        if line.startswith("## "):
            if current:
                current["content"] = "\n".join(current_lines).strip()
                sections.append(current)
                current_lines = []
            current = {"heading": line[3:].strip(), "content": ""}
        else:
            if current:
                current_lines.append(line)
            else:
                summary_lines.append(line)

    if current:
        current["content"] = "\n".join(current_lines).strip()
        sections.append(current)

    summary = "\n".join(summary_lines).strip()
    return summary, sections


def _build_section_content(section: Dict[str, str]) -> str:
    heading = section["heading"]
    body = section["content"]
    content_parts = [f"# {heading}", "", body, "", "_Version: v1.0.0_"]
    return "\n".join(part for part in content_parts if part).strip() + "\n"


def _slugify(value: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
    return slug or "section"


def _archive_documents(
    root: Path,
    records: List[DocumentationFile],
    link_report: LinkGraphReport,
    report_dir: Path,
    *,
    dry_run: bool,
) -> List[dict]:
    candidates = set(link_report.orphans or [])
    for record in records:
        if record.flags.get("needs_archive"):
            candidates.add(str(record.path.relative_to(root)))

    archived: List[dict] = []
    for rel_path in sorted(candidates):
        if rel_path.startswith("docs/archive"):
            continue
        if rel_path.startswith("spec/"):
            continue
        dest_rel = _archive_destination(rel_path)
        archived_rel = file_creator.archive_document(
            root=root,
            source_rel=rel_path,
            dest_rel=dest_rel,
            report_dir=report_dir,
            dry_run=dry_run,
        )
        if archived_rel:
            archived.append({"source": rel_path, "destination": archived_rel})
    return archived


def _archive_destination(relative_path: str) -> str:
    rel = Path(relative_path)
    if rel.parts and rel.parts[0] == "docs":
        rel = Path(*rel.parts[1:])
    return str(Path("docs") / "archive" / rel)


def _update_archive_indexes(root: Path, archived_paths: Sequence[str], *, dry_run: bool) -> None:
    if not archived_paths:
        return

    archive_dir = root / "docs" / "archive"
    archive_dir.mkdir(parents=True, exist_ok=True)

    archive_readme = archive_dir / "README.md"
    lines = ["# Archive", "", "## Entries", ""]
    for rel in sorted(archived_paths):
        path = Path(rel)
        display_name = path.stem.replace("-", " ").title()
        link_target = path.name if len(path.parts) <= 2 else str(Path(*path.parts[2:]))
        lines.append(f"- [{display_name}]({link_target})")
    lines.append("")
    if not dry_run:
        archive_readme.write_text("\n".join(lines), encoding="utf-8")

    docs_readme = root / "docs" / "README.md"
    existing = docs_readme.read_text(encoding="utf-8") if docs_readme.exists() else "# Documentation\n"
    updated = _inject_archive_section(existing, archived_paths)
    if not dry_run:
        docs_readme.parent.mkdir(parents=True, exist_ok=True)
        docs_readme.write_text(updated, encoding="utf-8")


def _inject_archive_section(content: str, archived_paths: Sequence[str]) -> str:
    lines = content.splitlines()
    new_lines: List[str] = []
    skipping = False

    for line in lines:
        if line.startswith("## Archive"):
            skipping = True
            continue
        if skipping and line.startswith("## "):
            skipping = False
        if not skipping:
            new_lines.append(line)

    if new_lines and new_lines[-1] != "":
        new_lines.append("")

    new_lines.append("## Archive")
    new_lines.append("")
    for rel in sorted(archived_paths):
        name = Path(rel).stem.replace("-", " ").title()
        new_lines.append(f"- [{name}]({rel})")
    new_lines.append("")

    return "\n".join(line for line in new_lines if line is not None).strip() + "\n"

