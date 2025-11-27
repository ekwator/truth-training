"""Shared dataclass helpers for doc_refactor models."""

from __future__ import annotations

from dataclasses import asdict, is_dataclass
from typing import Any, Dict, TypeVar

T = TypeVar("T")


class BaseModel:
    """Common mixin that exposes a dict serialization helper."""

    def to_dict(self) -> Dict[str, Any]:
        if not is_dataclass(self):
            raise TypeError(f"{self.__class__.__name__} must be a dataclass to use BaseModel")
        return asdict(self)


def to_dict(instance: T) -> Dict[str, Any]:
    """Serialize a dataclass instance to a dictionary."""

    if not is_dataclass(instance):
        raise TypeError("to_dict expects a dataclass instance")
    return asdict(instance)

