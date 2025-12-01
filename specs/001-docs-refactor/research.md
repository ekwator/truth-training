# Documentation Refactor v1.0.0 — Research & Decisions

## Overview

This document captures the key design decisions for refactoring Truth Training documentation to a fully linked, hierarchical, and v1.0.0-accurate state, using existing Spec-Kit and doc-refactor tooling.

## Decision 1 — Inventory Mechanism

- **Decision**: Use `scripts/doc_refactor/main.py` as the primary engine for building a Markdown inventory, extended (if needed) with a simple file-walk that records all `.md` paths and their classification (`root`, `docs`, `spec`, `other`).
- **Rationale**: The doc-refactor pipeline is already integrated into CONTRIBUTING and CI, so reusing it avoids new tooling and keeps automation accessible to Cursor agents.
- **Alternatives considered**:
  - Pure shell-based `find` + `grep` solution (rejected for lack of parsing robustness and poor cross-platform portability).
  - Adding a new Python CLI tool outside `scripts/doc_refactor` (rejected to avoid fragmentation of documentation tooling).

## Decision 2 — Link Detection & Normalization

- **Decision**: Treat any Markdown inline code or text that matches `*.md` as a candidate reference and normalize it to a standard Markdown link `[path](path)` when it refers to a file within the repository.
- **Rationale**: This ensures full clickability and makes link traversal deterministic for automated tools and human readers.
- **Alternatives considered**:
  - Only touching explicit Markdown links (rejected because many current references are bare paths and would remain non-clickable).
  - Introducing a custom link syntax (rejected to keep Markdown simple and familiar).

## Decision 3 — External URL Handling

- **Decision**: Validate external URLs best-effort (e.g., HTTP HEAD/GET with timeouts) and treat failures as warnings recorded in a report, without blocking the refactor.
- **Rationale**: External availability is outside project control; we must record but not fail the pipeline on transient network or remote issues.
- **Alternatives considered**:
  - Ignoring external URLs entirely (rejected because dead external links still harm usability).
  - Failing the run on any unreachable URL (rejected as too brittle for CI).

## Decision 4 — Hierarchical Restructuring Rules

- **Decision**: Enforce the rule “depth = detail”:
  - `[README.md](README.md)`: ≤400 words, high-level overview, list of release surfaces (CLI, Server, Desktop UI, Mobile), and links into `/docs` and `/spec`.
  - `/docs`: full narrative, tutorials, release details, troubleshooting, and OS-specific nuances.
  - `/spec`: compressed, directive-style content aimed at AI agents, with minimal narrative and pointers back to `/docs` where necessary.
- **Rationale**: Aligns with the specification and constitution by giving humans and AI distinct, predictable entry points.
- **Alternatives considered**:
  - Keeping detailed release info in `[README.md](README.md)` (rejected because it reduces clarity and conflicts with the new hierarchy).
  - Merging `/docs` and `/spec` (rejected because it blurs audiences and reduces Spec-Kit effectiveness).

## Decision 5 — Duplicate Content Detection

- **Decision**: Implement a simple block-based heuristic for detecting duplicated paragraphs between `/docs` and `/spec` (e.g., normalized text blocks, ignoring whitespace and minor formatting) and report duplicates rather than attempting automatic deletion.
- **Rationale**: Automated deletion is risky for nuanced documentation; a report lets maintainers or Cursor-controlled tasks decide what to trim while preserving intent.
- **Alternatives considered**:
  - Full text-similarity engine (rejected as overkill for this repo size).
  - Manual-only duplicate hunting (rejected as non-scalable and error-prone).

## Decision 6 — Index & Orphan Resolution

- **Decision**: Maintain three primary index “gateways”: `[README.md](README.md)`, `[docs/README.md](docs/README.md)`, and `[spec/README.md](spec/README.md)`. Any `.md` file not reachable from at least one of these after traversal is considered orphaned and must be:
  - Linked from an appropriate index, or
  - Explicitly documented as archived/legacy in an index section.
- **Rationale**: Guarantees there are no hidden docs and ensures auditors can start from a small set of entry points.
- **Alternatives considered**:
  - Keeping some “internal-only” docs unlinked (rejected because the spec requires zero orphaned files).

## Decision 7 — Version Tagging for v1.0.0

- **Decision**: Ensure every in-scope doc either:
  - States applicability to v1.0.0 explicitly (e.g., via a version line or context), or
  - Is clearly marked as historical/legacy if it refers to earlier versions.
- **Rationale**: Aligns with success criteria that all documentation is correct for v1.0.0 and avoids confusion around older content.
- **Alternatives considered**:
  - Adding a global “this repo is v1.0.0” note only in one place (rejected, as it leaves ambiguity for older spec/doc artifacts).


