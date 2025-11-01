
# Implementation Plan: Context Fields Embedded in Events — UI Template Editor Screen

**Branch**: `006-context-fields-embedded` | **Date**: 2025-01-27 | **Spec**: /home/ekwator/Code/truth-training/specs/006-context-fields-embedded/spec.md
**Input**: Feature specification from `/home/ekwator/Code/truth-training/specs/006-context-fields-embedded/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Ensure collective intelligence principles are preserved
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, `GEMINI.md` for Gemini CLI, `QWEN.md` for Qwen Code, or `AGENTS.md` for all other agents).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Refactor the event/context data model to embed context fields (category_id, forma_id, cause_id, develop_id, effect_id) directly into events instead of using context_id foreign key reference. Add a Context Editor UI screen for template management with duplicate detection (non-NULL field comparison) and FK validation (reject invalid references). Template matching for display uses non-NULL field comparison. All crates version bumped to v1.0.0 as first stable baseline. No automatic database migrations — manual data migration expected.

## Technical Context
**Language/Version**: Rust (stable), TypeScript/React (desktop UI), SQLite (embedded DB)  
**Primary Dependencies**: Actix Web (server API), Tauri (desktop UI), rusqlite (storage), React/TypeScript (UI)  
**Storage**: SQLite embedded database (local persistence)  
**Testing**: cargo test (unit/integration), Jest (UI tests), contract tests  
**Target Platform**: Linux/macOS/Windows desktop (Tauri), server (cross-platform Rust)  
**Project Type**: Multi-crate Rust workspace (core/server/app) with TypeScript/React desktop UI  
**Performance Goals**: Low-latency local operations (<100ms for event creation), responsive UI rendering  
**Constraints**: No automatic database migrations; manual migration path required. Foreign key validation required (reject invalid FKs). Template matching and duplicate detection use non-NULL field comparison. Backward compatibility consideration for existing events.  
**Scale/Scope**: Single-node local database with P2P sync capability. Context templates: ~10-50 typical, events: unlimited. Version bump marks v1.0.0 stable baseline.

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Separation of Concerns**: ✅ Changes span core (models), server (API), app (CLI), and UI (React). Each crate maintains clear boundaries. Models in `core`, API in `server`, UI logic in `ui/desktop`.
- **API-First Interfaces**: ✅ New endpoints for context template management follow REST patterns. CLI remains first-class client.
- **Cryptographic Integrity**: ✅ No changes to crypto layer; envelope signing preserved.
- **Integration Testing**: ✅ Contract tests required for new endpoints. Integration tests for template selection, duplicate detection, FK validation.
- **Observability & Versioning**: ✅ Structured logging for template operations. Semantic version bump to v1.0.0 across all crates. CHANGELOG entries required.
- **Collective Intelligence Principles**: ✅ Context templates support truth training methodology by enabling consistent event categorization and pattern recognition.
- **Simplicity (YAGNI)**: ✅ Direct field embedding simplifies data model (removes FK lookup overhead). Template system prevents duplication without over-engineering.

**Constitution Compliance**: ✅ PASS — All principles preserved. Data model refactoring improves simplicity while maintaining collective intelligence alignment.

## Project Structure

### Documentation (this feature)
```
specs/006-context-fields-embedded/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
core/
├── src/
│   ├── models.rs        # Update TruthEvent, NewTruthEvent (remove context_id, add 5 fields)
│   └── storage.rs       # Update schema, add_truth_event, get_context helpers, FK validation, NULL-aware matching

src/
├── api.rs               # Update POST /events (accept 5 fields, FK validation), add context template endpoints (NULL-aware duplicate detection)
└── main.rs              # No changes (unless config)

app/
├── src/
│   └── cli.rs           # Update event creation commands if applicable

ui/desktop/
├── src/
│   ├── pages/
│   │   ├── NewEvent.tsx      # Update form to accept template selection, prefill fields (allow modification)
│   │   ├── ContextEditor.tsx # NEW: Context template editor screen with duplicate detection
│   │   └── Events.tsx        # Update display logic (NULL-aware template matching, [Create Template] button)
│   ├── services/
│   │   └── api.ts            # Update createEvent, add context template endpoints
│   └── types/
│       ├── events.ts         # Update Event interface (remove context_id, add 5 fields)
│       └── contexts.ts       # NEW: Context template types

tests/
├── contract/
│   ├── api_events_test.rs    # Update for new field structure, FK validation
│   └── api_contexts_test.rs  # NEW: Context template endpoints, NULL-aware duplicate detection
└── integration/
    └── context_templates.rs   # NEW: Template selection, duplicate detection, FK validation, NULL handling flows
```

**Structure Decision**: Use existing multi-crate workspace structure. Core model changes in `core/`, API changes in `src/api.rs`, UI changes in `ui/desktop/`. New Context Editor screen as separate React component. Contract tests for new endpoints. No database migration scripts (manual migration expected).

## Phase 0: Outline & Research

**Research Tasks** (all resolved per research.md):
1. ✅ Field embedding pattern: Direct field embedding chosen over FK lookup
2. ✅ Template duplicate detection: NULL values ignored (compare only non-NULL fields)
3. ✅ Foreign key validation: Reject invalid FKs immediately with error message
4. ✅ UI template matching: NULL values ignored (compare only non-NULL fields, consistent with duplicate detection)
5. ✅ Version bump strategy: Coordinated v1.0.0 across all crates

**Output**: research.md (complete)

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Event: Remove context_id, add category_id, forma_id, cause_id, develop_id, effect_id (all Option<i64> for nullable FKs)
   - Context Template: name, category_id, forma_id, cause_id, develop_id, effect_id, description
   - Validation: FK existence checks (reject invalid), duplicate template detection (non-NULL field comparison)

2. **Generate API contracts** from functional requirements:
   - POST /events: Update to accept 5 fields instead of context_id, validate FKs
   - GET /contexts: List all context templates
   - GET /contexts/by-name/{name}: Get template by name
   - POST /contexts: Create new template (non-NULL field duplicate check)
   - POST /contexts/match: Match event fields to template (non-NULL field comparison)
   - POST /contexts/from-event: Create template from event fields
   - OpenAPI schema in `/contracts/`

3. **Generate contract tests** from contracts:
   - Update tests/contract/api_events_test.rs for new field structure, FK validation
   - Create tests/contract/api_contexts_test.rs for template endpoints, NULL-aware duplicate detection

4. **Extract test scenarios** from user stories:
   - Template selection prefills event form (allow modification)
   - Event creation saves embedded fields with FK validation
   - Template matching displays context name (non-NULL field comparison)
   - Duplicate detection prevents creation (non-NULL field comparison)
   - FK validation rejects invalid references

5. **Update agent file incrementally**:
   - Run `.specify/scripts/bash/update-agent-context.sh cursor`

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each entity → model creation task [P]
- Each user story → integration test task
- Implementation tasks to make tests pass
- Version bump tasks for all crates
- CHANGELOG update tasks

**Ordering Strategy**:
- TDD order: Tests before implementation
- Dependency order: Models (core) → API (server) → UI (desktop)
- Version bump at end (after all tests pass)
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 20-25 numbered, ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

No violations — all changes align with constitution principles.

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved (per clarifications session 2025-01-27)
- [x] Complexity deviations documented (none)

---
*Based on Constitution v2.1.0 - See `.specify/memory/constitution.md`*
