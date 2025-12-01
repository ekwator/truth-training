"""LinkGraphReport dataclass."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, TYPE_CHECKING

from .base import BaseModel
from .documentation_file import DocumentationFile

if TYPE_CHECKING:  # pragma: no cover
    from scripts.doc_refactor.links import ReferenceEdge  # noqa: F401


@dataclass
class LinkGraphReport(BaseModel):
    nodes: List[DocumentationFile] = field(default_factory=list)
    edges: List["ReferenceEdge"] = field(default_factory=list)
    orphans: List[str] = field(default_factory=list)
    broken_urls: List[dict] = field(default_factory=list)
    plain_paths: List[dict] = field(default_factory=list)
    normalizations: List[dict] = field(default_factory=list)
    stats: Dict[str, int] = field(default_factory=dict)

    def add_node(self, node: DocumentationFile) -> None:
        self.nodes.append(node)

    def add_edge(self, edge: "ReferenceEdge") -> None:
        self.edges.append(edge)

    def record_orphan(self, path: str) -> None:
        if path not in self.orphans:
            self.orphans.append(path)

