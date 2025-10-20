
# Implementation Plan: Collective Intelligence Layer (Wisdom of the Crowd)

**Branch**: `001-collective-intelligence-layer` | **Date**: 2025-10-20 | **Spec**: `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/spec.md`
**Input**: Feature specification from `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/spec.md`

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
Enable weighted collective judgment with dynamic reputation to converge on truth over time. Judgments are fully anonymous (aggregate-only visibility). Consensus is computed for any N ≥ 1. Non-functional targets from research: <100ms update for 1000 participants; scale to 10k–100k events/day with 100–1000 concurrent participants; outlier filtering preserves minority signals.

## Technical Context
**Language/Version**: Rust (edition 2021)  
**Primary Dependencies**: serde/serde_json, rusqlite, ed25519-dalek, uuid, chrono, base64  
**Storage**: SQLite (via rusqlite)  
**Testing**: cargo test (unit + integration)  
**Target Platform**: Desktop + Mobile (feature-gated: `desktop`, `mobile`)  
**Project Type**: single (library/crate with CLI/app)  
**Performance Goals**: <100ms update latency for 1000 participants; consensus recompute within 200ms p95  
**Constraints**: Deterministic signed payloads; anonymity for judgments; reproducible consensus  
**Scale/Scope**: 100–1000 concurrent participants; 10k–100k events/day

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Separation of concerns: integrate with `core` domain and API without UI leakage — PASS
- Cryptographic integrity (ed25519; deterministic serialization; signature validation) — PASS
- Observability/versioning simplicity (structured logging, semver adherence) — PASS
- Collective intelligence principles (emergent consensus, dynamic reputation, self-correction) — PASS
- Privacy/anonymity policy (aggregate-only visibility for judgments) — PASS

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
src/
├── api.rs
├── p2p/
├── expert.rs
├── lib.rs
└── sync.rs

tests/
├── api_push.rs
└── android_verify.rs

app/
└── src/
    └── main.rs
```

**Structure Decision**: Single project with core library plus CLI; platform-specific behaviors are feature-gated (`desktop`, `mobile`).

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved
Status: COMPLETE → `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/research.md`

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Generate API contracts** from functional requirements:
   - For each user action → endpoint
   - Use standard REST/GraphQL patterns
   - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
   - One test file per endpoint
   - Assert request/response schemas
   - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
   - Each story → integration test scenario
   - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh cursor`
     **IMPORTANT**: Execute it exactly as specified above. Do not add or remove any arguments.
   - If exists: Add only NEW tech from current plan
   - Preserve manual additions between markers
   - Update recent changes (keep last 3)
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file
Status: COMPLETE → data model at `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/data-model.md`, contracts at `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/contracts/*.yaml`, quickstart at `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/quickstart.md`

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each entity → model creation task [P] 
- Each user story → integration test task
- Implementation tasks to make tests pass

**Ordering Strategy**:
- TDD order: Tests before implementation 
- Dependency order: Models before services before UI
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 25-30 numbered, ordered tasks in tasks.md
Note: Executed separately; generated at `/home/ekwator/Code/truth-training/specs/001-collective-intelligence-layer/tasks.md`.

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [x] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [ ] Complexity deviations documented

---
*Based on Constitution v2.0.0 - See `/memory/constitution.md`*
