"""SpecCompressionProfile dataclass."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import List

from .base import BaseModel


@dataclass
class SpecCompressionProfile(BaseModel):
    path: Path
    section_order: List[str] = field(default_factory=list)
    paragraph_lengths: List[int] = field(default_factory=list)
    ai_directive_present: bool = False

    def add_paragraph_length(self, length: int) -> None:
        self.paragraph_lengths.append(length)

