"""LinkEdge dataclass."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Literal

from .base import BaseModel

LinkStatus = Literal["ok", "missing", "needs_stub", "verified"]


@dataclass
class LinkEdge(BaseModel):
    source_path: str
    target_path: str
    label: str
    is_external: bool = False
    status: LinkStatus = "ok"

    @classmethod
    def from_paths(
        cls,
        source: Path,
        target: str,
        label: str,
        *,
        is_external: bool = False,
        status: LinkStatus = "ok",
    ) -> "LinkEdge":
        return cls(
            source_path=str(source),
            target_path=target,
            label=label,
            is_external=is_external,
            status=status,
        )

