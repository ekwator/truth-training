"""Link graph discovery utilities."""

from __future__ import annotations

import re
from collections import Counter
from pathlib import Path
from typing import Dict, Iterable, Iterator, List, Optional

from mistune import create_markdown

from .models import DocumentationFile, LinkEdge, LinkGraphReport

MARKDOWN = create_markdown(renderer="ast")
PLAIN_PATH_PATTERN = re.compile(r"(?<!\]\()(?P<path>[A-Za-z0-9_\-./]+\.md)", re.IGNORECASE)


def build_link_graph(root: Path, records: List[DocumentationFile]) -> LinkGraphReport:
    report = LinkGraphReport()
    record_map = {record.path.resolve(): record for record in records}

    for record in records:
        report.add_node(record)
        text = _read_text(record.path)
        ast = MARKDOWN(text)
        for node in _walk_nodes(ast):
            if node.get("type") == "link":
                _handle_link_node(root, record, node, record_map, report)
            elif node.get("type") == "text":
                _collect_plain_paths(root, record, node.get("raw", ""), report)

    report.stats = _build_stats(records)
    report.orphans = _detect_orphans(root, records)
    return report


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


def _handle_link_node(
    root: Path,
    record: DocumentationFile,
    node: dict,
    record_map: Dict[Path, DocumentationFile],
    report: LinkGraphReport,
) -> None:
    attrs = node.get("attrs") or {}
    target = attrs.get("url", "")
    label = "".join(child.get("raw", "") for child in node.get("children", []) or [])
    is_external = target.startswith(("http://", "https://"))
    normalized_target = target.split("#", 1)[0]

    if not normalized_target and not is_external:
        normalized_target = str(record.path.relative_to(root))

    target_display = normalized_target if is_external else _normalize_relative(root, record.path, normalized_target)
    edge = LinkEdge.from_paths(record.path, target_display, label or target, is_external=is_external)
    report.add_edge(edge)

    if not is_external:
        resolved = _resolve_internal_target(record.path, normalized_target)
        if resolved and resolved.resolve() in record_map:
            target_doc = record_map[resolved.resolve()]
            record.add_outbound(str(target_doc.path.relative_to(root)))
            target_doc.add_inbound(str(record.path.relative_to(root)))


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


def _collect_plain_paths(root: Path, record: DocumentationFile, text: str, report: LinkGraphReport) -> None:
    for match in PLAIN_PATH_PATTERN.finditer(text):
        path_snippet = match.group("path")
        report.plain_paths.append(
            {
                "source": str(record.path.relative_to(root)),
                "match": path_snippet,
            }
        )


def _build_stats(records: List[DocumentationFile]) -> Dict[str, int]:
    counter = Counter(record.role for record in records)
    stats = {f"role_{role.lower()}": count for role, count in counter.items()}
    stats["total_nodes"] = len(records)
    return stats


def _detect_orphans(root: Path, records: List[DocumentationFile]) -> List[str]:
    orphans = []
    for record in records:
        if record.role in {"README", "INDEX", "ARCHIVE"}:
            continue
        if not record.inbound_links:
            orphans.append(str(record.path.relative_to(root)))
    return orphans

