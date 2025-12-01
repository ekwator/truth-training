"""DocumentationFile dataclass definition."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Literal

from .base import BaseModel

Role = Literal["README", "INDEX", "DETAIL", "SPEC", "ARCHIVE"]
Audience = Literal["root", "docs", "spec", "other"]


def _default_flags() -> Dict[str, bool]:
    return {"orphan": False, "needs_archive": False, "broken_links": False}


@dataclass
class DocumentationFile(BaseModel):
    """Canonical record for each Markdown file discovered during inventory."""

    path: Path
    slug: str
    depth: int
    role: Role
    audience: Audience = "other"
    version_tag: str = "legacy"
    word_count: int = 0
    linked_from: List[str] = field(default_factory=list)
    links_to: List[str] = field(default_factory=list)
    is_orphan: bool = False
    is_excluded: bool = False
    flags: Dict[str, bool] = field(default_factory=_default_flags)

    def add_outbound(self, target: str) -> None:
        if target not in self.links_to:
            self.links_to.append(target)

    def add_inbound(self, source: str) -> None:
        if source not in self.linked_from:
            self.linked_from.append(source)

