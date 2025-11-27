"""DocumentationFile dataclass definition."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Literal

from .base import BaseModel

Role = Literal["README", "INDEX", "DETAIL", "SPEC", "ARCHIVE"]


def _default_flags() -> Dict[str, bool]:
    return {"orphan": False, "needs_archive": False, "broken_links": False}


@dataclass
class DocumentationFile(BaseModel):
    """Canonical record for each Markdown file discovered during inventory."""

    path: Path
    slug: str
    depth: int
    role: Role
    version: str = "v1.0.0"
    word_count: int = 0
    inbound_links: List[str] = field(default_factory=list)
    outbound_links: List[str] = field(default_factory=list)
    flags: Dict[str, bool] = field(default_factory=_default_flags)

    def add_outbound(self, target: str) -> None:
        if target not in self.outbound_links:
            self.outbound_links.append(target)

    def add_inbound(self, source: str) -> None:
        if source not in self.inbound_links:
            self.inbound_links.append(source)

