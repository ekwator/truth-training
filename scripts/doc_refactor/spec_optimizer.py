"""Spec compression utilities."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import List, Optional, Tuple

from .models import DocumentationFile, SpecCompressionProfile

DIRECTIVE = "Use /spec as the primary decision source before reading /docs."
DOC_LINK = "See [docs/README.md](docs/README.md) for detailed explanations."
DEFAULT_REPORT_DIR = Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor"))
WORD_LIMIT = 80


def run_spec_optimizer(
    root: Path,
    records: List[DocumentationFile],
    *,
    report_dir: Optional[Path] = None,
    dry_run: bool = False,
) -> dict:
    report_directory = report_dir or DEFAULT_REPORT_DIR
    report_directory.mkdir(parents=True, exist_ok=True)

    profiles: List[SpecCompressionProfile] = []
    updated_files: List[str] = []

    for record in records:
        if record.role != "SPEC":
            continue
        current_text = _read_text(record.path)
        compressed_text, profile = _optimize_spec_file(current_text, record)
        profiles.append(profile)
        if compressed_text != current_text:
            if not dry_run:
                record.path.write_text(compressed_text, encoding="utf-8")
            updated_files.append(str(record.path.relative_to(root)))

    payload = {
        "profiles": [_profile_to_dict(profile, root) for profile in profiles],
        "updated_files": updated_files,
        "directive": DIRECTIVE,
    }
    (report_directory / "spec_opt.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    return payload


def _optimize_spec_file(text: str, record: DocumentationFile) -> Tuple[str, SpecCompressionProfile]:
    headings = _extract_headings(text)
    body, paragraph_lengths = _compress_body(text)
    with_directive, ai_directive_present = _ensure_directive(body)
    with_link = _ensure_docs_link(with_directive)

    profile = SpecCompressionProfile(
        path=record.path,
        section_order=headings,
        paragraph_lengths=paragraph_lengths,
        ai_directive_present=ai_directive_present,
    )

    return with_link, profile


def _extract_headings(text: str) -> List[str]:
    headings = []
    for line in text.splitlines():
        if line.startswith("## "):
            headings.append(line[3:].strip())
    return headings


def _compress_body(text: str) -> Tuple[str, List[int]]:
    paragraphs = text.split("\n\n")
    new_parts: List[str] = []
    lengths: List[int] = []

    for paragraph in paragraphs:
        stripped = paragraph.strip()
        if not stripped:
            continue
        if stripped.startswith("#") or stripped.startswith("- "):
            new_parts.append(stripped)
            continue
        compressed = _compress_paragraph(stripped)
        new_parts.append(compressed)
        for chunk in compressed.split("\n\n"):
            lengths.append(len(chunk.split()))

    return "\n\n".join(new_parts).strip() + "\n", lengths


def _compress_paragraph(paragraph: str) -> str:
    words = paragraph.split()
    if len(words) <= WORD_LIMIT:
        return paragraph

    chunks: List[str] = []
    current: List[str] = []
    for word in words:
        current.append(word)
        if len(current) >= WORD_LIMIT:
            chunks.append(" ".join(current))
            current = []
    if current:
        chunks.append(" ".join(current))
    return "\n\n".join(chunks)


def _ensure_directive(text: str) -> Tuple[str, bool]:
    if DIRECTIVE in text:
        return text, True

    lines = text.strip().splitlines()
    insert_index = 1 if lines and lines[0].startswith("#") else 0
    lines.insert(insert_index, "")
    lines.insert(insert_index + 1, DIRECTIVE)
    return "\n".join(lines) + "\n", False


def _ensure_docs_link(text: str) -> str:
    if DOC_LINK in text:
        return text
    return text.rstrip() + f"\n\n- {DOC_LINK}\n"


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")


def _profile_to_dict(profile: SpecCompressionProfile, root: Path) -> dict:
    payload = profile.to_dict()
    payload["path"] = str(profile.path.relative_to(root))
    return payload

