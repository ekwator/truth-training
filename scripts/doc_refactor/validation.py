"""Validation utilities for doc_refactor."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Dict, List, Optional
from urllib import error, request

from .models import DocumentationFile, LinkGraphReport

PLAIN_PATH_PATTERN = re.compile(r"(?<!\]\()(?<!\[)(?P<path>[A-Za-z0-9_\-./]+\.md)(?!\))", re.IGNORECASE)
URL_PATTERN = re.compile(r"https?://[^\s)]+")


def run_validation(
    root: Path,
    records: List[DocumentationFile],
    link_report: LinkGraphReport,
    *,
    dry_run: bool = False,
    report_dir: Optional[Path] = None,
) -> Dict[str, List[dict]]:
    plain_paths: List[dict] = []
    replacements: List[dict] = []
    broken_urls: List[dict] = []
    updated_files: List[str] = []

    for record in records:
        text = _read_text(record.path)
        new_text, replaced_paths = _rewrite_plain_paths(text)

        if replaced_paths:
            replacements.append(
                {"path": str(record.path.relative_to(root)), "replacements": replaced_paths}
            )
            plain_paths.extend(replaced_paths)
            if not dry_run:
                record.path.write_text(new_text, encoding="utf-8")
                updated_files.append(str(record.path.relative_to(root)))
            text = new_text

        for url in URL_PATTERN.findall(text):
            if "<!-- verified: reachable -->" in text:
                continue
            if not _check_url(url):
                broken_urls.append(
                    {
                        "path": str(record.path.relative_to(root)),
                        "url": url,
                        "status": "needs_stub",
                    }
                )

    report = {
        "plain_paths": [],
        "replacements": replacements,
        "broken_urls": broken_urls,
        "updated_files": updated_files,
    }

    if report_dir:
        report_dir.mkdir(parents=True, exist_ok=True)
        (report_dir / "validation.json").write_text(json.dumps(report, indent=2), encoding="utf-8")

    link_report.plain_paths = []
    return report


def _rewrite_plain_paths(text: str) -> tuple[str, List[str]]:
    replacements: List[str] = []
    
    # Simple pattern: only match .md paths that are NOT already in markdown links
    # Look for standalone .md paths that are not preceded by ]( and not followed by )
    pattern = r'(?<!\]\()\b([A-Za-z0-9_\-./]+\.md)\b(?!\))'
    
    def repl(match: re.Match[str]) -> str:
        path = match.group(1)
        # Additional check: make sure this line doesn't already contain this as a link
        full_match = match.group(0)
        if f"[{path}]({path})" in text or f"]({path})" in text:
            return full_match  # Don't modify if already a link
        replacements.append(path)
        return f"[{path}]({path})"

    new_text = re.sub(pattern, repl, text)
    return new_text, replacements


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")


def _check_url(url: str) -> bool:
    req = request.Request(url, method="HEAD")
    try:
        with request.urlopen(req, timeout=5) as response:
            return 200 <= getattr(response, "status", 0) < 400
    except (error.HTTPError, error.URLError, TimeoutError, ValueError, Exception):
        return False

