# Quality Gates

Use /spec as the primary decision source before reading /docs.
Version: v1.1.0
Updated: 2026-01-10
Spec ID: 14

## Quality Gates

### Code Quality
- **Lint**: `cargo fmt --all` + `cargo clippy --all-targets --all-features -- -D warnings` (workspace-wide, zero warnings required)
- **Build**: `cargo check` (Rust edition 2021)
- **Type Checking**: `pnpm typecheck` (Desktop UI), `ktlint` + `detekt` (Android)
- **Security Audit**: `cargo audit` (no vulnerabilities allowed)

### Database & Schema Integrity (Rule 5 Compliance)
#### Normalization Requirements
- **Normalization**: All Truth Training data schemas MUST satisfy 1NF, 2NF, 3NF, BCNF, 4NF, and 5NF; 6NF/DKNF may be adopted for temporal/domain-heavy modules when justified in specs
- **Unique Keys**: Every table requires a unique key, unused tables are removed, and each schema change ships with documented forward/backward migrations plus cleanup scripts

#### Authority and Canonical Sources
- **Canonical Schema Files**: Changes to runtime DB schema, table names, primary/foreign key types, or semantic meaning of fields must be reconciled with canonical files:
  - `docs/model_core.md` — canonical markdown Formalized Model Core and Database Schema
  - `spec/04-data-model.md` — canonical SQL schema specifications for implementers
  - `spec/26-seed_knowledge_base_table_value.md` — Knowledge Base Table Values for Default Seeding
  - `docs/Data_Schema.md` — canonical markdown schema specifications for implementers
  - `SECURITY.md` — security and verification requirements
  - `CONTRIBUTING.md` — quality and testing requirements

#### Schema Validation Requirements
- **Single Source of Truth**: Implementations (core, desktop, android, server, cli) must target `docs/model_core.md` as the ground truth for table names, column types, PK/FK definitions, and indexes
- **No Shadow Schemas**: Implementations may not retain or ship divergent table names, key types (e.g., TEXT vs INTEGER PK for the same logical entity), or incompatible FK constraints without a formally approved migration and a side-by-side compatibility plan

#### Dual-Database Allocation
- **Primary Domain DB**: `truth_training.sqlite` — primary domain DB: `truth_events`, `impact`, `judgments`, `participants`, `progress_metrics`, knowledge base tables (`category`, `forma`, etc.). This DB contains the event and assessment history and must follow the strict Quality Gates for truth/judgments and impacts
- **Discovery Metadata DB**: `discovery_nodes.sqlite` — discovery and network metadata: nodes list, reachability, TTLs, registry snapshots, behavioral signatures, node trust limits, and ephemeral discovery caches

#### Data Movement & History Rules
- **Append-Only for Judgment History**: Judgments and their versions are historical records. Do not silently overwrite judgment rows. Use version tables (or append versions) to keep complete history. This is mandated for auditability and reproducibility
- **Impact Immutability Constraints**: Impact records must remain bound to their originating event and preserve timestamps. Deletions of impact records are allowed only via an explicit cleanup script with justification logged and approved
- **No Silent Deletes**: Any operation that removes historical data must be documented, batched, and reversible (via backups). Quiet or automatic deletion that is not approved by a migration/cleanup plan is forbidden
- **Signed Records**: Any judgment/impact/critical append must include verifiable cryptographic metadata (signature, public key or proof) where the spec requires it. Unsigned critical updates must be rejected or downgraded in sanity checks

### Testing
- **Unit Tests**: API signature verification, storage schema init, expert heuristic cases, component rendering
- **Integration Tests**: REST API endpoints, context engine, event lifecycle, cross-platform compatibility
- **E2E Tests**: Desktop Playwright tests, Android instrumentation tests (physical device required)
- **Test Coverage**: Rust Core ≥90%, Desktop UI ≥85%, Android ≥70%, Server API ≥90%
- **Schema Validation Tests**: Automated tests that assert table presence and column types, validate PK/FK integrity, verify indices that the performance expectations depend on, run PRAGMA/schema diffs used by CI
- **Contract Tests**: Any API or P2P message that depends on schema must include contract tests that fail fast if schema and message format drift
- **Behavior Tests (Quality)**: For Judgment and Impact axes include: Cryptographic signature validation tests, Immutability/append tests, Aggregation correctness tests (non-regression on aggregator functions)
- **No Skipped Tests**: All `#[ignore]`, `it.skip`, `@Ignore` must be justified

### Documentation
- **Spec-Kit**: Must be referenced in README and kept up to date in PRs. All PRs must include `/speckit.specify`, `/speckit.plan`, `/speckit.task` or `/speckit.implementation` chain
- **Doc Refactor**: `make doc-refactor-run` must succeed and attach `inventory.json`, `link_report.json`, and `validation.json` artifacts to the PR/CI run. No `ReferenceEdge` may remain in `status: "missing"`

### CI Requirements
- **Cross-Platform Build**: Linux, Windows, macOS, Android, Desktop UI
- **All Tests Pass**: Rust, Desktop, Android (both CI emulator and physical device)
- **Static Analysis**: Clean (no warnings, no errors)
- **Schema Validation**: Automated schema-validation steps must run on PRs and pass
- **Performance**: No regressions (Desktop navigation <200ms, pagination <100ms, Android cold start <1.3s)

### PR Acceptance
- **Spec-Kit Chain**: Required for all PRs (PRs without Spec-Kit workflow will be closed)
- **Schema Changes Declaration**: Every PR that alters storage code or DB DDL must include:
  - Updated canonical schema files (`spec/04-data-model.md` / `docs/Data_Schema.md`) and/or `docs/model_core.md` if the change is conceptual
  - Forward and backward migration scripts
  - Schema validation tests
  - A Spec-Kit plan `/specify → /plan` describing the migration rollout and compatibility strategy
- **Migration Requirements**: Provide forward and backward SQL migrations (or programmatic migrations for non-SQL changes) that include: Data transformation steps, Verification queries, Rollback procedure, Budgeted downtime (if any)
- **Review Approvals**: Minimum 2 maintainers (3 for security-sensitive changes)
- **No TODOs**: All TODOs must be resolved before merge
- **Documentation**: Updated when needed, CHANGELOG.md updated
- **PR Author Checklist for Schema Changes**:
  - [ ] Did you update `spec/04-data-model.md` or `docs/Data_Schema.md` (if relevant)?
  - [ ] Do you provide forward and backward migrations?
  - [ ] Are schema validation tests added/updated and green in CI?
  - [ ] Do contract tests reflect any API/P2P format change?
  - [ ] Did you attach or reference a Spec-Kit `/specify` and `/plan`?
  - [ ] Did you include release notes for the migration (script location, rollback steps)?
  - [ ] If sensitive: did you include a security review step (per `SECURITY.md`)?

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed requirements.

_Version: v1.1.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.

- Aligned with [.specify/memory/constitution.md](../.specify/memory/constitution.md) Rule 5 — Database & Schema Integrity (version 2.4.0).
