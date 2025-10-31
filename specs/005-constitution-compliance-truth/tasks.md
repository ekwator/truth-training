
# Tasks: Constitution Compliance (Truth Training Core & UI)

Branch: 005-constitution-compliance-truth

Conventions:
- [P] = can run in parallel with others in same group
- TDD-first: write failing tests before implementation

## Ordered Task List

T001. Create compliance contract tests for endpoints [P] [X]
- Path: /home/ekwator/Code/truth-training/specs/005-constitution-compliance-truth/contracts/openapi.yaml
- Action: Generate test files asserting request/response stubs for `/events` (anonymous submit) and `/judgments` (ternary signal)
- Output: tests/contract/api_events_test.rs, tests/contract/api_judgments_test.rs (failing)
- Dependencies: none

T002. Add core data models (TruthEvent, Judgment, NodeReputation, SyncEnvelope) [X]
- Paths: /home/ekwator/Code/truth-training/core/
- Action: Define structs and SQLite schema migrations aligned with data-model.md (no author metadata; envelope fields; ternary judgment enum)
- Output: models.rs, storage migration scripts, compile-only (no logic yet)
- Dependencies: T001

T003. Implement anti-replay and envelope validation in server P2P/HTTP middleware [X]
- Paths: /home/ekwator/Code/truth-training/server/
- Action: Add middleware to verify transport envelope signature, extract sender node id, attach to request context; reject replays
- Output: middleware module + unit tests (failing first)
- Dependencies: T002

T004. Implement POST /events (anonymous event submission) [X]
- Paths: /home/ekwator/Code/truth-training/server/api.rs
- Action: Accept payload without author fields; persist TruthEvent with envelope sender id and signature; forbid author metadata
- Output: handler + validation; contract test passes
- Dependencies: T001, T003

T005. Implement POST /judgments (ternary: confirm/reject/abstain) [X]
- Paths: /home/ekwator/Code/truth-training/server/api.rs
- Action: Persist Judgment with envelope sender id; enforce independence by unique sender within anti-replay window
- Output: handler + validation; contract test passes
- Dependencies: T001, T003, T004

T006. Add weighting and decay computation in core [X]
- Paths: /home/ekwator/Code/truth-training/core/
- Action: Compute `status_weight` and `decay_score` from judgments; expose functions used by server
- Output: core services + unit tests
- Dependencies: T002, T005

T007. Add NodeReputation updates based on historical accuracy [X]
- Paths: /home/ekwator/Code/truth-training/core/
- Action: Update reputation after ground truth convergence windows; mitigate Sybil via weighting hooks
- Output: reputation service + unit tests
- Dependencies: T006

T008. Wi‑Fi Direct sync module scaffold [X]
- Paths: /home/ekwator/Code/truth-training/server/p2p/
- Action: Create feature-flagged module for nearby sync; define interface and stubs (no platform bindings yet)
- Output: p2p/wifi_direct.rs + trait; compile-only
- Dependencies: T002

T009. CLI (app) text-only flows for confession and judgment [P] [X]
- Paths: /home/ekwator/Code/truth-training/app/
- Action: Add commands: `app confess --file|-` and `app judge --event <id> --signal <confirm|reject|abstain>`; print risk notice about plaintext-at-rest
- Output: new CLI subcommands + help
- Dependencies: T004, T005

T010. Integration tests: quickstart scenarios [X]
- Paths: /home/ekwator/Code/truth-training/tests/integration/
- Action: Implement steps from quickstart.md including multi-node confirmations and Wi‑Fi Direct stubbed sync
- Output: failing tests first; then pass with T011–T013
- Dependencies: T004–T009

T011. Documentation updates: constitution alignment notes [X]
- Paths: /home/ekwator/Code/truth-training/docs/
- Action: Add/Update docs to reflect authorship removal, plaintext-at-rest policy, ternary judgments, independence definition, Wi‑Fi Direct
- Output: docs/Constitution-Compliance.md
- Dependencies: T004–T009

T012. Observability: structured logs for envelope verification and consensus [X]
- Paths: /home/ekwator/Code/truth-training/server/
- Action: Add trace IDs and logs for envelope checks, independence decisions, weight/decay updates
- Output: logging additions + tests
- Dependencies: T006

T013. Post-Design Constitution Check gate [X]
- Paths: /home/ekwator/Code/truth-training/specs/005-constitution-compliance-truth/
- Action: Revalidate plan against constitution; update plan.md Gate Status
- Output: plan.md updated gate check
- Dependencies: T004–T012

## Parallelization Guidance
- Group A [P]: T001, T008 can run immediately
- Group B [P]: After T004/T005, run T009 and T011 in parallel
- Group C [P]: T012 can run alongside T006 once interfaces stabilize

## Agent Commands Examples
- Run contract tests: `cargo test --tests contract`
- Run integration tests: `cargo test --tests integration`
- Build server: `cargo build -p server`
- Build app: `cargo build -p app`


