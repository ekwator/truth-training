# Implementation Plan: Documentation Refactoring v1.0.0

**Branch**: `001-documentation-refactoring-0-0` | **Date**: 2025-11-26 | **Spec**: [`spec.md`](./spec.md)  
**Input**: Feature specification from `/specs/001-documentation-refactoring-0-0/spec.md`

**Note**: Generated via `/plan` for the documentation refactoring feature; see `.specify/templates/commands/plan.md` for workflow details.

## Summary

Documentation Refactoring v1.0.0 will restructure all Markdown documentation in this repository to:  
- Ensure complete link integrity starting from `README.md` and across `/docs` and `/spec`.  
- Enforce a clean hierarchy where `/docs` contains deep, human-readable content and `/spec` contains compressed, decision-ready specs for AI agents.  
- Remove duplicated content by moving detailed material out of top-level files (especially `README.md`) into appropriate deeper docs, while keeping code and runtime behavior unchanged.

The approach is a docs-only refactor guided by the existing constitution and spec-kit: build a full inventory of `.md` files, enforce link integrity through graph traversal, then reorganize content according to a “depth = detail” rule and clear role separation between `/docs` and `/spec`.

## Technical Context

**Language/Version**: Markdown documentation within a Rust/TypeScript monorepo (no runtime code changes for this feature)  
**Primary Dependencies**: Git, Markdown tooling (renderers, link checkers), spec-kit scripts in `.specify/`  
**Storage**: Git repository (files on disk) only; no database or API changes  
**Testing**: Manual/scripted Markdown link checks; spot checks of v1.0.0 behavior vs. docs; optional CI doc-link job later  
**Target Platform**: Documentation consumers (GitHub UI, local editors) and AI agents using `/spec` as primary input  
**Project Type**: Single multi-product repository; this feature operates solely on documentation and specs  
**Performance Goals**: N/A for runtime; qualitative goal is faster human/agent navigation and lower cognitive load  
**Constraints**: Must not alter runtime behavior or public APIs; must preserve Git history when possible (edit in place rather than delete/recreate)  
**Scale/Scope**: All `.md` files in the repo excluding `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, `CHANGELOG.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Security & Crypto**: No changes to cryptographic flows, protocols, or storage; documentation only → cryptographic integrity principles remain satisfied.  
- **Separation of Concerns**: The refactor clarifies documentation responsibilities (`/docs` vs `/spec`) without adding crates or runtime modules, supporting the constitution’s simplicity and separation guidelines.  
- **Collective Intelligence & Traceability**: By making all design docs reachable and clarifying `/spec` as the AI entrypoint, the change strengthens traceability and collective-intelligence alignment rather than weakening it.  
- **Complexity**: No new structural complexity is introduced beyond reorganizing existing docs; complexity tracking remains N/A for this plan.

## Project Structure

### Documentation (this feature)

```text
specs/001-documentation-refactoring-0-0/
├── [plan.md](specs/001-documentation-refactoring-0-0/plan.md)              # This file (/plan command output)
├── [research.md](specs/001-documentation-refactoring-0-0/research.md)          # Phase 0 output (/plan command)
├── [data-model.md](specs/001-documentation-refactoring-0-0/data-model.md)        # Phase 1 output (/plan command)
├── [quickstart.md](specs/001-documentation-refactoring-0-0/quickstart.md)        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── [tasks.md](specs/001-documentation-refactoring-0-0/tasks.md)             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code & Docs (repository root)

```text
docs/
├── Truth-training/                   # Cross-platform comparison & indices
├── api_reference/                    # Human-readable API reference (v1.0.0)
├── ANDROID_MIGRATION.md              # Android migration v0.3.0 → v1.0.0
├── TEST_REPORT_ANDROID_v1.0.0.md     # Android test report
├── VERSION_REGISTRY.md               # Central version map
└── ...                               # Other human-facing guides, reports, specs

spec/
├── 01-product-vision.md
├── 02-requirements.md
├── 03-architecture.md
├── 05-api.md
├── 13-traceability.md
├── 16-test-plan.md
└── README.md                         # Will be updated with AI usage guidance
```

**Structure Decision**: Use existing `docs/` as the deep human-readable tree and `spec/` as the compressed AI-oriented spec tree; this feature adjusts their internal indices and cross-links without creating new code-level structure.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [ ] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [ ] Complexity deviations documented

---
*Based on Constitution v2.1.0 - See `.specify/memory/constitution.md`*
