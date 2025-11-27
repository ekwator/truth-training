"""Convenience exports for doc_refactor model dataclasses."""

from .base import BaseModel, to_dict
from .documentation_file import DocumentationFile
from .link_edge import LinkEdge
from .link_graph_report import LinkGraphReport
from .run_artifact import RunArtifact
from .spec_compression_profile import SpecCompressionProfile

__all__ = [
    "BaseModel",
    "DocumentationFile",
    "LinkEdge",
    "LinkGraphReport",
    "RunArtifact",
    "SpecCompressionProfile",
    "to_dict",
]

