"""RunArtifact dataclass for phase orchestration."""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from .base import BaseModel


@dataclass
class RunArtifact(BaseModel):
    phase_name: str
    started_at: str
    finished_at: Optional[str] = None
    elapsed_sec: Optional[float] = None
    status: str = "pending"
    output_path: Optional[str] = None
    metadata: dict = field(default_factory=dict)

    @classmethod
    def start(cls, phase_name: str) -> "RunArtifact":
        return cls(phase_name=phase_name, started_at=_now())

    def finish(self, status: str, *, output_path: Optional[str] = None, elapsed_sec: Optional[float] = None) -> None:
        self.status = status
        self.finished_at = _now()
        self.elapsed_sec = elapsed_sec
        if output_path:
            self.output_path = output_path

    def save(self, directory: Path) -> Path:
        directory.mkdir(parents=True, exist_ok=True)
        artifact_path = directory / f"{self.phase_name}.json"
        artifact_path.write_text(json.dumps(self._serializable(), indent=2), encoding="utf-8")
        return artifact_path

    def _serializable(self) -> dict:
        payload = self.to_dict()
        return payload


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()

