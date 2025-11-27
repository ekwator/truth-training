"""CLI orchestrator for the documentation refactor workflow."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, Iterable, List, Optional

if __package__ in (None, ""):
    PACKAGE_ROOT = Path(__file__).resolve().parents[2]
    if str(PACKAGE_ROOT) not in sys.path:
        sys.path.insert(0, str(PACKAGE_ROOT))
    from scripts.doc_refactor import (  # type: ignore
        duplicate_detector,
        inventory,
        link_graph,
        restructuring,
        spec_optimizer,
        validation,
        version_sync,
    )
    from scripts.doc_refactor.models import (  # type: ignore
        DocumentationFile,
        LinkGraphReport,
        RunArtifact,
    )
else:
    from . import (
        duplicate_detector,
        inventory,
        link_graph,
        restructuring,
        spec_optimizer,
        validation,
        version_sync,
    )
    from .models import DocumentationFile, LinkGraphReport, RunArtifact

PHASES = [
    "inventory",
    "link_discovery",
    "validation",
    "file_creation",
    "version_sync",
    "restructuring",
    "dedupe",
    "spec_opt",
]

@dataclass
class CLIContext:
    root: Path
    report_dir: Path
    dry_run: bool
    fail_fast: bool
    records: List[DocumentationFile] = field(default_factory=list)
    link_report: Optional[LinkGraphReport] = None
    phase_outputs: Dict[str, dict] = field(default_factory=dict)
    dedupe_actions: int = 0

    def ensure_report(self, filename: str, payload: dict | list) -> Path:
        target = self.report_dir / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(payload, indent=2), encoding="utf-8")
        return target


def parse_args(argv: Optional[Iterable[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Documentation refactor CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    run_parser = subparsers.add_parser("run", help="Execute doc_refactor phases")
    run_parser.add_argument(
        "--phases",
        default="all",
        help="Comma-separated list of phases to run (default: all)",
    )
    run_parser.add_argument("--root", default=".", help="Repository root (default: current directory)")
    run_parser.add_argument("--dry-run", action="store_true", help="Execute without modifying files")
    run_parser.add_argument("--resume", action="store_true", help="Resume from last successful phase")
    run_parser.add_argument("--fail-fast", action="store_true", help="Stop at first failure")
    run_parser.add_argument("--report-dir", help="Override report output directory")

    return parser.parse_args(list(argv) if argv is not None else None)


def main(argv: Optional[Iterable[str]] = None) -> int:
    args = parse_args(argv)
    if args.command != "run":
        print("Unsupported command", file=sys.stderr)
        return 2

    root = Path(args.root).resolve()
    report_dir = Path(args.report_dir).resolve() if args.report_dir else Path(
        os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor")
    ).resolve()
    os.environ["DOC_REFACTOR_REPORT_DIR"] = str(report_dir)

    context = CLIContext(root=root, report_dir=report_dir, dry_run=args.dry_run, fail_fast=args.fail_fast)
    phases = _normalize_phases(args.phases)
    if args.resume:
        phases = _phases_pending(phases, context)

    exit_code = 0
    for phase in phases:
        try:
            _run_phase(phase, context)
        except Exception as exc:  # noqa: BLE001
            print(f"[doc_refactor] Phase '{phase}' failed: {exc}", file=sys.stderr)
            exit_code = 1
            if args.fail_fast or phase == phases[-1]:
                break

    _write_run_summary(context)
    return exit_code


def _normalize_phases(phases_arg: str) -> List[str]:
    if phases_arg == "all":
        return PHASES.copy()
    requested = [phase.strip() for phase in phases_arg.split(",") if phase.strip()]
    invalid = [phase for phase in requested if phase not in PHASES]
    if invalid:
        raise ValueError(f"Unknown phases requested: {', '.join(invalid)}")
    return requested


def _phases_pending(phases: List[str], context: CLIContext) -> List[str]:
    completed = _load_completed_phases(_artifact_dir(context))
    return [phase for phase in phases if phase not in completed]


def _load_completed_phases(directory: Path) -> List[str]:
    if not directory.exists():
        return []
    completed = []
    for artifact_file in directory.glob("*.json"):
        try:
            data = json.loads(artifact_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        if data.get("status") == "success":
            completed.append(artifact_file.stem)
    return completed


def _run_phase(phase: str, context: CLIContext) -> None:
    handler = PHASE_HANDLERS.get(phase)
    if handler is None:
        raise ValueError(f"No handler registered for phase '{phase}'")

    artifact = RunArtifact.start(phase)
    started = time.perf_counter()
    output_path: Optional[Path] = None
    status = "success"
    return_path: Optional[str] = None
    try:
        output_path = handler(context)
        return_path = str(output_path) if output_path else None
    except Exception as exc:  # noqa: BLE001
        status = "failed"
        artifact.metadata["error"] = str(exc)
        raise
    finally:
        elapsed = time.perf_counter() - started
        artifact.finish(status, output_path=return_path, elapsed_sec=elapsed)
        artifact.save(_artifact_dir(context))
        context.phase_outputs[phase] = {
            "status": status,
            "output": return_path,
            "elapsed_sec": round(elapsed, 3),
        }
        if status == "success":
            print(f"[doc_refactor] Phase '{phase}' completed in {elapsed:.2f}s")


def phase_inventory(context: CLIContext) -> Optional[Path]:
    context.records = inventory.generate_inventory(context.root)
    return DEFAULT_REPORT(context.report_dir, "inventory.json")


def phase_link_discovery(context: CLIContext) -> Optional[Path]:
    records = _ensure_records(context)
    context.link_report = link_graph.build_link_graph(context.root, records)
    payload = _serialize_link_report(context)
    return context.ensure_report("link_graph.json", payload)


def phase_validation(context: CLIContext) -> Optional[Path]:
    records = _ensure_records(context)
    link_report = context.link_report or link_graph.build_link_graph(context.root, records)
    context.link_report = link_report
    validation.run_validation(
        root=context.root,
        records=records,
        link_report=link_report,
        dry_run=context.dry_run,
        report_dir=context.report_dir,
    )
    return context.report_dir / "validation.json"


def phase_version_sync(context: CLIContext) -> Optional[Path]:
    records = _ensure_records(context)
    version_sync.run_version_sync(
        root=context.root,
        records=records,
        dry_run=context.dry_run,
        report_dir=context.report_dir,
    )
    return context.report_dir / "version_sync.json"


def phase_dedupe(context: CLIContext) -> Optional[Path]:
    records = _ensure_records(context)
    report = duplicate_detector.run_duplicate_detection(
        root=context.root,
        records=records,
        dry_run=context.dry_run,
        report_dir=context.report_dir,
    )
    context.dedupe_actions = len(report.get("actions", []))
    return context.report_dir / "dedupe.json"


def phase_spec_opt(context: CLIContext) -> Optional[Path]:
    records = _ensure_records(context)
    spec_optimizer.run_spec_optimizer(
        root=context.root,
        records=records,
        report_dir=context.report_dir,
        dry_run=context.dry_run,
    )
    return context.report_dir / "spec_opt.json"


def phase_placeholder(context: CLIContext, name: str) -> Optional[Path]:
    payload = {"phase": name, "status": "pending_implementation"}
    return context.ensure_report(f"{name}.json", payload)


def DEFAULT_REPORT(report_dir: Path, filename: str) -> Path:
    return report_dir / filename


PHASE_HANDLERS: Dict[str, Callable[[CLIContext], Optional[Path]]] = {
    "inventory": phase_inventory,
    "link_discovery": phase_link_discovery,
    "validation": phase_validation,
    "file_creation": lambda ctx: phase_placeholder(ctx, "file_creation"),
    "version_sync": phase_version_sync,
    "restructuring": lambda ctx: phase_restructuring(ctx),
    "dedupe": phase_dedupe,
    "spec_opt": phase_spec_opt,
}


def _artifact_dir(context: CLIContext) -> Path:
    directory = context.report_dir / "run_artifacts"
    directory.mkdir(parents=True, exist_ok=True)
    return directory


def _ensure_records(context: CLIContext) -> List[DocumentationFile]:
    if not context.records:
        context.records = inventory.generate_inventory(context.root)
    return context.records


def _serialize_link_report(context: CLIContext) -> dict:
    assert context.link_report is not None
    graph = context.link_report
    nodes = []
    for node in graph.nodes:
        node_dict = node.to_dict()
        node_dict["path"] = str(node.path.relative_to(context.root))
        nodes.append(node_dict)

    edges = [edge.to_dict() for edge in graph.edges]
    return {
        "nodes": nodes,
        "edges": edges,
        "orphans": graph.orphans,
        "broken_urls": graph.broken_urls,
        "plain_paths": graph.plain_paths,
        "stats": graph.stats,
    }


def phase_restructuring(context: CLIContext) -> Optional[Path]:
    records = _ensure_records(context)
    link_report = context.link_report or link_graph.build_link_graph(context.root, records)
    context.link_report = link_report
    restructuring.run_restructuring(
        root=context.root,
        records=records,
        link_report=link_report,
        dry_run=context.dry_run,
        report_dir=context.report_dir,
    )
    if not context.dry_run:
        context.link_report = link_graph.build_link_graph(context.root, records)
    return context.report_dir / "restructure.json"


def _write_run_summary(context: CLIContext) -> None:
    broken_links = len(context.link_report.broken_urls) if context.link_report else 0
    coverage = _coverage_ratio(context)
    summary = {
        "broken_links": broken_links,
        "coverage_ratio": coverage,
        "phases": context.phase_outputs,
        "dedupe_actions": context.dedupe_actions,
    }
    path = context.report_dir / "run_summary.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(summary, indent=2), encoding="utf-8")


def _coverage_ratio(context: CLIContext) -> int:
    if not context.records:
        return 100
    if context.link_report and not context.link_report.orphans:
        return 100
    linked = 0
    for record in context.records:
        if record.role in {"README", "INDEX", "ARCHIVE", "SPEC"} or record.inbound_links:
            linked += 1
    return int(round((linked / len(context.records)) * 100))


if __name__ == "__main__":
    raise SystemExit(main())

