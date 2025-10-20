# Tasks: Collective Intelligence Layer (Wisdom of the Crowd)

**Input**: Design documents from `/specs/001-collective-intelligence-layer/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → If not found: ERROR "No implementation plan found"
   → Extract: tech stack, libraries, structure
2. Load optional design documents:
   → data-model.md: Extract entities → model tasks
   → contracts/: Each file → contract test task
   → research.md: Extract decisions → setup tasks
3. Generate tasks by category:
   → Setup: project init, dependencies, linting
   → Tests: contract tests, integration tests
   → Core: models, services, CLI commands
   → Integration: DB, middleware, logging
   → Polish: unit tests, performance, docs
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
8. Validate task completeness:
   → All contracts have tests?
   → All entities have models?
   → All endpoints implemented?
9. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

## Phase 3.1: Setup
- [X] T001 Create collective intelligence module structure in core/src/collective_intelligence/
- [X] T002 Add collective intelligence dependencies to Cargo.toml (serde_json, uuid, chrono, statistical)
- [X] T003 [P] Configure database migration scripts for collective intelligence tables

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**COLLECTIVE INTELLIGENCE: Ensure tests validate consensus mechanisms and truth convergence**
- [X] T004 [P] Contract test POST /api/v1/judgments in tests/contract/test_judgments_post.rs
- [X] T005 [P] Contract test GET /api/v1/judgments in tests/contract/test_judgments_get.rs
- [X] T006 [P] Contract test GET /api/v1/consensus/{event_id} in tests/contract/test_consensus_get.rs
- [X] T007 [P] Contract test POST /api/v1/consensus/{event_id}/calculate in tests/contract/test_consensus_calculate.rs
- [X] T008 [P] Contract test GET /api/v1/reputation/{participant_id} in tests/contract/test_reputation_get.rs
- [X] T009 [P] Contract test GET /api/v1/reputation/leaderboard in tests/contract/test_reputation_leaderboard.rs
- [X] T010 [P] Integration test judgment submission flow in tests/collective_intelligence/test_judgment_flow.rs
- [X] T011 [P] Integration test consensus calculation flow in tests/collective_intelligence/test_consensus_flow.rs
- [X] T012 [P] Integration test reputation evolution flow in tests/collective_intelligence/test_reputation_flow.rs

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [X] T013 [P] Participant model in core/src/collective_intelligence/models.rs
- [X] T014 [P] Event model in core/src/collective_intelligence/models.rs
- [X] T015 [P] Judgment model in core/src/collective_intelligence/models.rs
- [X] T016 [P] Consensus model in core/src/collective_intelligence/models.rs
- [X] T017 [P] ReputationHistory model in core/src/collective_intelligence/models.rs
- [X] T018 [P] Judgment submission service in core/src/collective_intelligence/judgment.rs
- [X] T019 [P] Consensus calculation service in core/src/collective_intelligence/consensus.rs
- [X] T020 [P] Reputation management service in core/src/collective_intelligence/reputation.rs
- [X] T021 POST /api/v1/judgments endpoint in src/api.rs
- [X] T022 GET /api/v1/judgments endpoint in src/api.rs
- [X] T023 GET /api/v1/consensus/{event_id} endpoint in src/api.rs
- [X] T024 POST /api/v1/consensus/{event_id}/calculate endpoint in src/api.rs
- [X] T025 GET /api/v1/reputation/{participant_id} endpoint in src/api.rs
- [X] T026 GET /api/v1/reputation/leaderboard endpoint in src/api.rs

## Phase 3.4: Integration
- [X] T027 Connect collective intelligence services to SQLite database
- [X] T028 Add collective intelligence module exports to core/src/lib.rs
- [X] T029 Add collective intelligence endpoints to src/lib.rs
- [X] T030 Implement judgment signature verification using ed25519_dalek
- [X] T031 Add collective intelligence database migrations to core/src/storage.rs
- [X] T032 Implement consensus calculation triggers on new judgments
- [X] T033 Add reputation update triggers on consensus changes

## Phase 3.5: Polish
- [ ] T034 [P] Unit tests for consensus algorithms in tests/collective_intelligence/consensus_tests.rs
- [ ] T035 [P] Unit tests for reputation scoring in tests/collective_intelligence/reputation_tests.rs
- [ ] T036 [P] Unit tests for judgment validation in tests/collective_intelligence/judgment_tests.rs
- [ ] T037 Performance tests for consensus calculation (<100ms target)
- [ ] T038 [P] Update API documentation with collective intelligence endpoints
- [ ] T039 [P] Add collective intelligence examples to quickstart.md
- [ ] T040 Remove code duplication and optimize database queries
- [ ] T041 Run manual testing scenarios from quickstart.md

## Dependencies
- Tests (T004-T012) before implementation (T013-T026)
- T013-T017 (models) before T018-T020 (services)
- T018-T020 (services) before T021-T026 (endpoints)
- T027-T033 (integration) after T021-T026 (endpoints)
- T034-T041 (polish) after T027-T033 (integration)

## Parallel Execution Examples

### Phase 3.2: Contract Tests (T004-T012)
```bash
# Launch contract tests in parallel:
Task: "Contract test POST /api/v1/judgments in tests/contract/test_judgments_post.rs"
Task: "Contract test GET /api/v1/judgments in tests/contract/test_judgments_get.rs"
Task: "Contract test GET /api/v1/consensus/{event_id} in tests/contract/test_consensus_get.rs"
Task: "Contract test POST /api/v1/consensus/{event_id}/calculate in tests/contract/test_consensus_calculate.rs"
Task: "Contract test GET /api/v1/reputation/{participant_id} in tests/contract/test_reputation_get.rs"
Task: "Contract test GET /api/v1/reputation/leaderboard in tests/contract/test_reputation_leaderboard.rs"
Task: "Integration test judgment submission flow in tests/collective_intelligence/test_judgment_flow.rs"
Task: "Integration test consensus calculation flow in tests/collective_intelligence/test_consensus_flow.rs"
Task: "Integration test reputation evolution flow in tests/collective_intelligence/test_reputation_flow.rs"
```

### Phase 3.3: Model Creation (T013-T017)
```bash
# Launch model creation in parallel:
Task: "Participant model in core/src/collective_intelligence/models.rs"
Task: "Event model in core/src/collective_intelligence/models.rs"
Task: "Judgment model in core/src/collective_intelligence/models.rs"
Task: "Consensus model in core/src/collective_intelligence/models.rs"
Task: "ReputationHistory model in core/src/collective_intelligence/models.rs"
```

### Phase 3.3: Service Implementation (T018-T020)
```bash
# Launch service implementation in parallel:
Task: "Judgment submission service in core/src/collective_intelligence/judgment.rs"
Task: "Consensus calculation service in core/src/collective_intelligence/consensus.rs"
Task: "Reputation management service in core/src/collective_intelligence/reputation.rs"
```

### Phase 3.5: Unit Tests (T034-T036)
```bash
# Launch unit tests in parallel:
Task: "Unit tests for consensus algorithms in tests/collective_intelligence/consensus_tests.rs"
Task: "Unit tests for reputation scoring in tests/collective_intelligence/reputation_tests.rs"
Task: "Unit tests for judgment validation in tests/collective_intelligence/judgment_tests.rs"
```

## Notes
- [P] tasks = different files, no dependencies
- Verify tests fail before implementing
- Commit after each task
- Avoid: vague tasks, same file conflicts
- Collective intelligence algorithms require careful testing due to mathematical complexity
- Performance targets: <100ms consensus calculation, <200ms for 100 participants

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts**:
   - Each contract file → contract test task [P] (3 contract files = 6 test tasks)
   - Each endpoint → implementation task (6 endpoints)
   
2. **From Data Model**:
   - Each entity → model creation task [P] (5 entities)
   - Relationships → service layer tasks (3 services)
   
3. **From User Stories**:
   - Each story → integration test [P] (3 integration tests)
   - Quickstart scenarios → validation tasks

4. **Ordering**:
   - Setup → Tests → Models → Services → Endpoints → Polish
   - Dependencies block parallel execution

## Validation Checklist
*GATE: Checked by main() before returning*

- [x] All contracts have corresponding tests (6 contract tests)
- [x] All entities have model tasks (5 entity models)
- [x] All tests come before implementation
- [x] Parallel tasks truly independent
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
