"""LinkGraphReport dataclass."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List

from .base import BaseModel
from .documentation_file import DocumentationFile
from .link_edge import LinkEdge


@dataclass
class LinkGraphReport(BaseModel):
    nodes: List[DocumentationFile] = field(default_factory=list)
    edges: List[LinkEdge] = field(default_factory=list)
    orphans: List[str] = field(default_factory=list)
    broken_urls: List[str] = field(default_factory=list)
    plain_paths: List[str] = field(default_factory=list)
    stats: Dict[str, int] = field(default_factory=dict)

    def add_node(self, node: DocumentationFile) -> None:
        self.nodes.append(node)

    def add_edge(self, edge: LinkEdge) -> None:
        self.edges.append(edge)

    def record_orphan(self, path: str) -> None:
        if path not in self.orphans:
            self.orphans.append(path)

