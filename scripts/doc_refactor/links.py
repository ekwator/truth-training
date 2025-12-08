"""Link discovery and normalization utilities."""

from __future__ import annotations

import os
import re
from dataclasses import dataclass
from pathlib import Path
from collections import Counter
from typing import Dict, Iterable, Iterator, List, Literal, Optional, Tuple
from urllib import error, request

try:  # Optional dependency: prefer Mistune AST parsing when available
    from mistune import create_markdown

    MARKDOWN = create_markdown(renderer="ast")
    HAVE_MISTUNE = True
except ImportError:  # pragma: no cover - executed only if mistune missing
    HAVE_MISTUNE = False
    MARKDOWN = None

from .models import DocumentationFile, LinkGraphReport
from .models.base import BaseModel

ReferenceStatus = Literal["ok", "missing", "external_ok", "external_warning"]
PLAIN_PATH_PATTERN = re.compile(r"(?<!\]\()(?P<path>[A-Za-z0-9_\-./]+\.md)(?!\))")
URL_PATTERN = re.compile(r"https?://[^\s)]+")
MARKDOWN_LINK_PATTERN = re.compile(r"\[(?P<label>[^\]]+)\]\((?P<target>[^)]+)\)")
DEFAULT_REPORT_DIR = Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor"))


@dataclass
class ReferenceEdge(BaseModel):
    """Directed link between two documentation nodes or external URL."""

    source_path: str
    target: str
    link_text: str
    status: ReferenceStatus
    is_external: bool
    normalized: bool = False
    anchor: Optional[str] = None


def build_link_report(
    root: Path,
    records: List[DocumentationFile],
    *,
    dry_run: bool = False,
    report_dir: Optional[Path] = None,
    external_timeout: float = 3.0,
) -> LinkGraphReport:
    """Discover markdown links, normalize bare paths, and validate references."""

    report_directory = report_dir or DEFAULT_REPORT_DIR
    report_directory.mkdir(parents=True, exist_ok=True)

    report = LinkGraphReport()
    record_map: Dict[Path, DocumentationFile] = {record.path.resolve(): record for record in records}
    rel_map: Dict[str, DocumentationFile] = {
        str(record.path.relative_to(root)): record for record in records
    }

    for record in records:
        report.add_node(record)
        relative_source = str(record.path.relative_to(root))
        text = _read_text(record.path)
        normalized_text, replacements = _normalize_plain_paths(text)

        if replacements:
            report.normalizations.append({"path": relative_source, "replacements": replacements})
            if not dry_run:
                record.path.write_text(normalized_text, encoding="utf-8")
            text = normalized_text

        if HAVE_MISTUNE and MARKDOWN is not None:
            ast = MARKDOWN(text)
            for node in _walk_nodes(ast):
                if node.get("type") == "link":
                    _handle_link_node(
                        root,
                        record,
                        node,
                        record_map,
                        rel_map,
                        report,
                        dry_run=dry_run,
                        external_timeout=external_timeout,
                    )
                elif node.get("type") == "text":
                    raw_text = node.get("raw", "")
                    _collect_plain_paths(root, record, raw_text, report)
                    _collect_bare_urls(
                        root,
                        record,
                        raw_text,
                        report,
                        record_map,
                        rel_map,
                        dry_run=dry_run,
                        external_timeout=external_timeout,
                    )
        else:
            _fallback_scan_links(
                root,
                record,
                text,
                record_map,
                rel_map,
                report,
                dry_run=dry_run,
                external_timeout=external_timeout,
            )

    report.orphans = _detect_orphans(root, records)
    report.stats = _build_stats(records)

    return report


def _handle_link_node(
    root: Path,
    record: DocumentationFile,
    node: dict,
    record_map: Dict[Path, DocumentationFile],
    rel_map: Dict[str, DocumentationFile],
    report: LinkGraphReport,
    *,
    dry_run: bool,
    external_timeout: float,
) -> None:
    attrs = node.get("attrs") or {}
    target = attrs.get("url", "").strip()
    label = "".join(child.get("raw", "") for child in node.get("children", []) or []) or target
    _register_reference(
        root,
        record,
        target=target,
        label=label,
        record_map=record_map,
        rel_map=rel_map,
        report=report,
        normalized=False,
        dry_run=dry_run,
        external_timeout=external_timeout,
    )


def _fallback_scan_links(
    root: Path,
    record: DocumentationFile,
    text: str,
    record_map: Dict[Path, DocumentationFile],
    rel_map: Dict[str, DocumentationFile],
    report: LinkGraphReport,
    *,
    dry_run: bool,
    external_timeout: float,
) -> None:
    for match in MARKDOWN_LINK_PATTERN.finditer(text):
        _register_reference(
            root,
            record,
            target=match.group("target").strip(),
            label=match.group("label").strip(),
            record_map=record_map,
            rel_map=rel_map,
            report=report,
            normalized=False,
            dry_run=dry_run,
            external_timeout=external_timeout,
        )
    _collect_plain_paths(root, record, text, report)
    _collect_bare_urls(
        root,
        record,
        text,
        report,
        record_map,
        rel_map,
        dry_run=dry_run,
        external_timeout=external_timeout,
    )


def _register_reference(
    root: Path,
    record: DocumentationFile,
    *,
    target: str,
    label: str,
    record_map: Dict[Path, DocumentationFile],
    rel_map: Dict[str, DocumentationFile],
    report: LinkGraphReport,
    normalized: bool,
    dry_run: bool,
    external_timeout: float,
) -> None:
    if not target:
        target = str(record.path.relative_to(root))
    anchor = None
    if "#" in target:
        target, anchor = target.split("#", 1)

    is_external = target.startswith(("http://", "https://"))
    resolved_target = (
        target if is_external else _normalize_relative(root, record.path, target or str(record.path.relative_to(root)))
    )
    status: ReferenceStatus

    if is_external:
        reachable = _check_url(resolved_target, timeout=external_timeout)
        status = "external_ok" if reachable else "external_warning"
        if not reachable:
            report.broken_urls.append(
                {"source": str(record.path.relative_to(root)), "url": resolved_target, "status": status}
            )
    else:
        resolved_path = _resolve_internal_target(record.path, target)
        if resolved_path and resolved_path.resolve() in record_map:
            target_doc = record_map[resolved_path.resolve()]
            rel = str(target_doc.path.relative_to(root))
            record.add_outbound(rel)
            target_doc.add_inbound(str(record.path.relative_to(root)))
            status = "ok"
            # Use normalized relative path as target
            resolved_target = rel
        elif target in rel_map:
            record.add_outbound(target)
            rel_map[target].add_inbound(str(record.path.relative_to(root)))
            status = "ok"
            # Use target as-is if it's in rel_map
            resolved_target = target
        else:
            status = "missing"
            report.broken_urls.append(
                {"source": str(record.path.relative_to(root)), "target": target or resolved_target, "status": status}
            )
            # Keep original target for missing files
            resolved_target = target

    edge = ReferenceEdge(
        source_path=str(record.path.relative_to(root)),
        target=resolved_target or target,
        link_text=label or resolved_target,
        status=status,
        is_external=is_external,
        normalized=normalized,
        anchor=anchor,
    )
    report.add_edge(edge)


def _normalize_plain_paths(text: str) -> Tuple[str, List[str]]:
    replacements: List[str] = []
    lines = text.splitlines()
    in_code_block = False

    for idx, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("```"):
            in_code_block = not in_code_block
            continue
        if in_code_block:
            continue

        # Pre-compute spans of existing Markdown links in this line so we don't
        # try to "normalize" paths that are already valid link targets.
        existing_link_spans: List[Tuple[int, int]] = [
            (m.start(), m.end()) for m in MARKDOWN_LINK_PATTERN.finditer(line)
        ]

        def _repl(match: re.Match[str]) -> str:
            path = match.group("path")
            start, _ = match.span()
            # Skip matches that fall inside an already well-formed Markdown link.
            for span_start, span_end in existing_link_spans:
                if span_start <= start < span_end:
                    return match.group(0)
            # Avoid re-wrapping entries that already appear as `[path](path)` on this line.
            if f"[{path}]" in line and f"]({path})" in line:
                return match.group(0)
            replacements.append(path)
            return f"[{path}]({path})"

        new_line = PLAIN_PATH_PATTERN.sub(_repl, line)
        lines[idx] = new_line

    return "\n".join(lines), replacements


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")


def _walk_nodes(tree: Iterable[dict] | dict) -> Iterator[dict]:
    if isinstance(tree, list):
        for node in tree:
            yield from _walk_nodes(node)
        return

    if not isinstance(tree, dict):
        return

    yield tree
    for child in tree.get("children", []) or []:
        yield from _walk_nodes(child)


def _collect_plain_paths(root: Path, record: DocumentationFile, text: str, report: LinkGraphReport) -> None:
    for match in PLAIN_PATH_PATTERN.finditer(text or ""):
        start, end = match.span()
        prev_char = text[start - 1] if start > 0 else ""
        next_char = text[end] if end < len(text) else ""
        if prev_char in "[(" or next_char in "])":
            continue
        report.plain_paths.append(
            {
                "source": str(record.path.relative_to(root)),
                "match": match.group("path"),
            }
        )


def _collect_bare_urls(
    root: Path,
    record: DocumentationFile,
    text: str,
    report: LinkGraphReport,
    record_map: Dict[Path, DocumentationFile],
    rel_map: Dict[str, DocumentationFile],
    *,
    dry_run: bool,
    external_timeout: float,
) -> None:
    for match in URL_PATTERN.finditer(text or ""):
        _register_reference(
            root,
            record,
            target=match.group(0),
            label=match.group(0),
            record_map=record_map,
            rel_map=rel_map,
            report=report,
            normalized=False,
            dry_run=dry_run,
            external_timeout=external_timeout,
        )


def _resolve_internal_target(base_path: Path, target: str) -> Optional[Path]:
    if not target:
        return base_path
    candidate = (base_path.parent / target).resolve()
    if candidate.is_file():
        return candidate
    return None


def _normalize_relative(root: Path, base_path: Path, target: str) -> str:
    resolved = _resolve_internal_target(base_path, target)
    if resolved and resolved.is_file():
        try:
            return str(resolved.relative_to(root))
        except ValueError:
            return str(resolved)
    return target


def _detect_orphans(root: Path, records: List[DocumentationFile]) -> List[str]:
    orphans = []
    for record in records:
        if record.role in {"README", "INDEX", "ARCHIVE"}:
            record.is_orphan = False
            continue
        record.is_orphan = len(record.linked_from) == 0
        if record.is_orphan:
            orphans.append(str(record.path.relative_to(root)))
    return orphans


def _build_stats(records: List[DocumentationFile]) -> Dict[str, int]:
    stats: Dict[str, int] = {"total_nodes": len(records)}
    role_counter = Counter(record.role for record in records)
    for role, count in role_counter.items():
        stats[f"role_{role.lower()}"] = count

    audience_counter = Counter(record.audience for record in records)
    for audience, count in audience_counter.items():
        stats[f"audience_{audience}"] = count

    stats["orphans"] = sum(1 for record in records if record.is_orphan)
    return stats


def _check_url(url: str, *, timeout: float) -> bool:
    try:
        req = request.Request(url, method="HEAD")
        with request.urlopen(req, timeout=timeout) as response:  # noqa: S310
            status = getattr(response, "status", 0)
            if 200 <= status < 400:
                return True
        return False
    except (error.HTTPError, error.URLError, TimeoutError, ValueError, Exception):
        return False

