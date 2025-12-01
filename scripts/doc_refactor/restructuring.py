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
READ_ME_WORD_CAP = 400
DOC_SECTION_RULES = [
    ("Release Notes & Plans", ("release", "version", "deployment", "artifact", "p2p")),
    ("Tutorials & Guides", ("guide", "tutorial", "howto", "build", "install", "cli")),
    ("Troubleshooting & Quality", ("troubleshooting", "test", "validation", "report", "qa")),
    ("Architecture & Concepts", ("architecture", "concept", "specification", "overview")),
]
DEFAULT_DOC_SECTION = "Core References"
PROTECTED_DOCS = {
    "docs/CLI_Usage.md",
    "docs/Deployment.md",
    "docs/UI_Desktop.md",
    "docs/android_discovery_architecture.md",
    "docs/Documentation_Refactor_Overview.md",
    "docs/Documentation_Refactor_Inventory.md",
    "docs/Documentation_Refactor_Links.md",
}


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
    archived_paths = [entry["destination"] for entry in archived_entries]
    _update_archive_indexes(root, archived_paths, dry_run=dry_run)
    _rewrite_docs_readme(root, archived_paths=archived_paths, dry_run=dry_run)

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
    nav_lines: List[str] = []

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

    navigation_block = _build_navigation_block(nav_lines)
    summary_block = _truncate_summary(summary)
    release_block = _release_surfaces_block()
    entry_points_block = _entry_points_block()

    assembled = "\n\n".join(
        block
        for block in [summary_block, release_block, entry_points_block, navigation_block]
        if block
    ).strip() + "\n"

    if not dry_run:
        readme_path.write_text(assembled, encoding="utf-8")

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
        if not rel_path.startswith("docs/"):
            continue
        if rel_path in PROTECTED_DOCS:
            continue
        if rel_path.startswith("docs/archive"):
            continue
        if rel_path.startswith("spec/"):
            continue
        if rel_path.startswith("specs/"):
            # Keep Spec-Kit feature specs in place; they are part of the
            # development workflow rather than end-user docs.
            continue
        source_path = root / rel_path
        if not source_path.exists():
            # Skip orphan entries that no longer exist on disk (e.g., deleted
            # templates or generated files). We still want restructuring to
            # succeed for the rest of the documentation set.
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
    if not docs_readme.exists():
        docs_readme.parent.mkdir(parents=True, exist_ok=True)
        docs_readme.write_text("# Documentation\n", encoding="utf-8")


def _build_navigation_block(nav_lines: List[str]) -> str:
    if not nav_lines:
        return ""
    lines = ["## Documentation Topics", ""]
    lines.extend(nav_lines)
    lines.append("")
    return "\n".join(lines).strip()


def _truncate_summary(summary: str) -> str:
    words = summary.split()
    if len(words) <= READ_ME_WORD_CAP:
        return summary.strip()
    truncated = " ".join(words[:READ_ME_WORD_CAP]) + " …"
    return truncated.strip()


def _release_surfaces_block() -> str:
    lines = [
        "## Release Surfaces",
        "",
        "- **CLI** — interact via [docs/CLI_Usage.md](docs/CLI_Usage.md)",
        "- **Server** — deploy following [docs/Deployment.md](docs/Deployment.md)",
        "- **Desktop UI** — reference [docs/UI_Desktop.md](docs/UI_Desktop.md)",
        "- **Mobile** — architecture in [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md)",
    ]
    return "\n".join(lines).strip()


def _entry_points_block() -> str:
    lines = [
        "## Documentation Entry Points",
        "",
        "- [docs/README.md](docs/README.md) — Human-readable, narrative depth",
        "- [spec/README.md](spec/README.md) — AI-focused directives and constraints",
        "- [docs/Documentation_Refactor_Overview.md](docs/Documentation_Refactor_Overview.md) — Pipeline summary",
        "- [docs/Documentation_Refactor_Inventory.md](docs/Documentation_Refactor_Inventory.md) — Inventory instructions",
        "- [docs/Documentation_Refactor_Links.md](docs/Documentation_Refactor_Links.md) — Link validation workflow",
    ]
    return "\n".join(lines).strip()


def _rewrite_docs_readme(root: Path, *, archived_paths: Sequence[str], dry_run: bool) -> None:
    docs_dir = root / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)
    index: Dict[str, List[str]] = {title: [] for title, _ in DOC_SECTION_RULES}
    index[DEFAULT_DOC_SECTION] = []

    for path in docs_dir.rglob("*.md"):
        relative = path.relative_to(root)
        if relative.parts and relative.parts[0].lower() == "docs" and len(relative.parts) > 1:
            if relative.parts[1].lower() == "archive":
                continue
        if path.name.lower() == "readme.md" and relative == Path("docs/README.md"):
            continue
        section = _match_doc_section(path.name)
        index[section].append(str(relative))

    lines = ["# Truth Training Documentation Hub (v1.0.0)", "", "Use this index to reach every human-facing reference."]
    for title, _ in DOC_SECTION_RULES + [(DEFAULT_DOC_SECTION, tuple())]:
        documents = sorted(index.get(title, []))
        if not documents:
            continue
        lines.append("")
        lines.append(f"## {title}")
        lines.append("")
        for rel in documents:
            display = Path(rel).stem.replace("_", " ").replace("-", " ").title()
            lines.append(f"- [{display}]({rel})")

    if archived_paths:
        lines.append("")
        lines.append("## Archive")
        lines.append("")
        for rel in sorted(archived_paths):
            lines.append(f"- [{Path(rel).name}]({rel})")

    content = "\n".join(lines).strip() + "\n"
    if not dry_run:
        (root / "docs" / "README.md").write_text(content, encoding="utf-8")


def _match_doc_section(filename: str) -> str:
    lowered = filename.lower()
    for title, keywords in DOC_SECTION_RULES:
        if any(keyword in lowered for keyword in keywords):
            return title
    return DEFAULT_DOC_SECTION


