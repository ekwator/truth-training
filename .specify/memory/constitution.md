# Truth Training Platform Constitution

<!--
Sync Impact Report
Version: 2.3.0 → 2.4.0
Modified Principles: Rule 5 — Database & Schema Integrity (expanded with operational supplement)
Added Sections: Rule 5 operational subsections (Authority & Canonical Sources, Schema Distortion Prevention, Dual-Database Allocation, Data Movement & History Rules, Migration & Validation Gates, Traceability & Documentation, Enforcement, PR Checklist)
Removed Sections: None
Templates: [.specify/templates/plan-template.md](.specify/templates/plan-template.md) ✅ updated (Constitution Check section aligns automatically), [.specify/templates/spec-template.md](.specify/templates/spec-template.md) ✅ updated (added Rule 5 compliance note in Key Entities section), [.specify/templates/tasks-template.md](.specify/templates/tasks-template.md) ✅ updated (added Rule 5 reference to schema setup task)
Follow-ups: None
-->

## Summary

Truth Training orchestrates a cryptographically verifiable, anonymous truth network that must run everywhere our participants reach it: CLI diagnostics, autonomous servers, desktop UI, and mobile clients. This constitution captures the philosophical intent, enumerates the immovable cross-platform rules, and binds the engineering workflow (README, CONTRIBUTING, SECURITY, CHANGELOG, Spec-Kit templates, and release automation) so Cursor agents and humans always operate from the same source of truth.

## Formal Rules

### Rule 1 — Cross-Platform Scope & Parity

1. The project simultaneously ships four first-class surfaces and keeps them feature-aligned: `app/src/bin/truthctl.rs` (CLI control & diagnostics), the Actix-based server node (autonomous FidoNet-style behavior with no sysop intervention), the Desktop UI for Linux/Windows/macOS (Tauri backend with glib/GTK renderer on Linux, operating both offline and network-connected), and mobile clients implemented in Kotlin (Android) and Swift (iOS) that mirror Desktop functionality.
2. Platform parity is verified through cross-platform specs/tests (e.g., `[docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md)`) and enforced for every API, schema, and consensus rule; drift is not permitted.

### Rule 2 — Source Documents as Authority

1. `[README.md](README.md)`, `[CONTRIBUTING.md](CONTRIBUTING.md)`, `[SECURITY.md](SECURITY.md)`, and `[CHANGELOG.md](CHANGELOG.md)` are binding references. When they change, this constitution must be updated accordingly; when this constitution adds constraints, those documents must be updated in the same pull request.
2. Any deviation from those sources is recorded (with justification) in the Change History table below so auditors see when and why governance diverged.

### Rule 3 — Releases, Installation & Automation

1. Every tagged release must be installable in four choices (CLI binary, server node, desktop UI installers, mobile binaries). If a surface is not ready, the release is blocked.
2. The README must contain up-to-date install instructions for each surface before a release is approved.
3. Release preparation and automation must remain executable by the Cursor AI agent exactly as described in `[CONTRIBUTING.md](CONTRIBUTING.md)` section “## 2. Release Preparation Requirements,” using `create-release.sh`, `release-info.txt`, and the associated versioned release-info files. Scripts and docs must be maintained alongside the code.
4. Release automation outputs (artifacts, CHANGELOG entry, README install sections) are audited by Spec-Kit workflows, making this policy testable.

### Rule 4 — Dependency, Vulnerability & Platform Safeguards

1. Dependencies with known vulnerabilities must be upgraded proactively when a non-breaking safe version exists; blocking advisories require justification and an action date.
2. Platform-specific vulnerabilities (e.g., glib < 0.20.0 impacting GTK3/Linux builds) demand an immediate mitigation strategy documented in specs plus conditional builds until patched.
3. Linux/GTK builds must perform a periodic glib version check; release automation validates that dependency manifests (`Cargo.lock`, `pnpm-lock.yaml`, Gradle catalogs) reflect those updates.
4. Automated scans (`cargo audit`, `npm audit`, `pnpm audit`, `gradlew lint`, Dependabot, CodeQL, or approved equivalents) run in CI and pre-release checklists; failures block merges until resolved or formally waived.

### Rule 5 — Database & Schema Integrity

#### 5.1 Normalization Requirements

1. Truth Training data schemas MUST satisfy 1NF, 2NF, 3NF, BCNF, 4NF, and 5NF; 6NF/DKNF may be adopted for temporal/domain-heavy modules when justified in specs.
2. Every table requires a unique key, unused tables are removed, and each schema change ships with documented forward/backward migrations plus cleanup scripts.

#### 5.2 Authority and Canonical Sources

The canonical descriptions of the model and schema are the authoritative sources for DB structure and semantics:
- `[docs/model_core.md](docs/model_core.md)` — canonical markdown Formalized Model Core and Database Schema
- `[spec/04-data-model.md](spec/04-data-model.md)` — canonical SQL schema specifications for implementers
- `[spec/26-seed_knowledge_base_table_value.md](spec/26-seed_knowledge_base_table_value.md)` — Knowledge Base Table Values for Default Seeding
- `[docs/Data_Schema.md](docs/Data_Schema.md)` — canonical markdown schema specifications for implementers
- `[SECURITY.md](SECURITY.md)` — security and verification requirements
- `[CONTRIBUTING.md](CONTRIBUTING.md)` — quality and testing requirements
- `[spec/14-quality-gates.md](spec/14-quality-gates.md)` — minimum requirements for PR acceptance

**Authority Rule**: Any change to the runtime DB schema, table names, primary/foreign key types, or semantic meaning of fields must be reconciled with — and implemented as — updates to the canonical files above. Implementations that diverge without an approved migration plan violate the constitution.

#### 5.3 Preventing Schema Distortion

**Single Source of Truth**: The schema described in `docs/model_core.md` is authoritative. Implementations (core, desktop, android, server, cli) must target those files as the ground truth for table names, column types, PK/FK definitions, and indexes.

**No Shadow Schemas**: Implementations may not retain or ship divergent table names, key types (e.g., TEXT vs INTEGER PK for the same logical entity), or incompatible FK constraints without a formally approved migration and a side-by-side compatibility plan.

**Declaration Requirement**: Every PR that alters storage code or DB DDL must include:
- Updated canonical schema files (`spec/04-data-model.md` / `docs/Data_Schema.md`) and/or `docs/model_core.md` if the change is conceptual
- Forward and backward migration scripts
- Schema validation tests (see Section 5.6)
- A Spec-Kit plan `/specify → /plan` describing the migration rollout and compatibility strategy

#### 5.4 Dual-Database Allocation

To keep responsibilities clearly separated and to reduce risk of cross-concern changes, the repository standardizes two local database files for embedded/local persistence:

- **`truth_training.sqlite`** — primary domain DB: `truth_events`, `impact`, `judgments`, `participants`, `progress_metrics`, knowledge base tables (`category`, `forma`, etc.). This DB contains the event and assessment history and must follow the strict Quality Gates for truth/judgments and impacts.

- **`discovery_nodes.sqlite`** — discovery and network metadata: nodes list, reachability, TTLs, registry snapshots, behavioral signatures, node trust limits, and ephemeral discovery caches. This DB may have shorter TTLs, separate lifecycle rules and a different backup cadence.

**Requirement**: Migrations and changes affecting either DB must be explicitly targeted to the correct file and documented with which DB they affect.

#### 5.5 Data Movement, Mutation & History Rules

**Append-Only for Judgment History**: Judgments and their versions are historical records. Do not silently overwrite judgment rows. Use version tables (or append versions) to keep complete history. This is mandated for auditability and reproducibility.

**Impact Immutability Constraints**: Impact records must remain bound to their originating event and preserve timestamps. Deletions of impact records are allowed only via an explicit cleanup script with justification logged and approved.

**No Silent Deletes**: Any operation that removes historical data must be documented, batched, and reversible (via backups). Quiet or automatic deletion that is not approved by a migration/cleanup plan is forbidden.

**Signed Records**: Any judgment/impact/critical append must include verifiable cryptographic metadata (signature, public key or proof) where the spec requires it. Unsigned critical updates must be rejected or downgraded in sanity checks.

**TTL & Cleanup**: For discovery and ephemeral caches only (e.g., in `discovery_nodes.sqlite`) apply TTL and automated cleanup, but preserve an audit log of removals and reasons. TTL rules must be part of the migration/change plan.

#### 5.6 Migration, Validation & Quality Gates

Every schema change or storage-related code change must pass these gates before merging:

**Spec Update Gate**: The change must be described in a Spec-Kit spec `/specify` and approved plan `/plan`. The PR must reference the spec and include the generated plan ID. Spec-Kit is mandatory per project rules.

**Migration Scripts**: Provide forward and backward SQL migrations (or programmatic migrations for non-SQL changes). Each migration must include:
- Data transformation steps
- Verification queries
- Rollback procedure
- Budgeted downtime (if any)

**Schema Validation Tests**: Automated tests that:
- Assert table presence and column types
- Validate PK/FK integrity
- Verify indices that the performance expectations depend on
- Run PRAGMA/schema diffs used by CI
These are part of CI Quality Gates (see `spec/14-quality-gates.md`).

**Contract Tests**: Any API or P2P message that depends on schema must include contract tests that fail fast if schema and message format drift.

**Behavior Tests (Quality)**: For Judgment and Impact axes include:
- Cryptographic signature validation tests
- Immutability/append tests
- Aggregation correctness tests (non-regression on aggregator functions)

**Blocking Policy**: Failing schema/migration tests block merge and release — per constitution Rule 5.

#### 5.7 Traceability, Documentation, and Releases

**One PR = Canonical Schema**: A canonical schema change must include updated schema documentation:
- The main canonical schema file `docs/model_core.md` cannot be edited without pre-approval
- Semantic changes to `spec/04-data-model.md`, `docs/Data_Schema.md`, `spec/26-seed_knowledge_base_table_value.md` corresponding to the data in `docs/model_core.md`
- In the file `spec/26-seed_knowledge_base_table_value.md`, table field values cannot be edited; only the database schema in `docs/Data_Schema.md` must be reviewed and corrected

A single PR must not contain code changes, migrations, secondary documentation, or tests along with the canonical schema update.

**One PR = Code + Documentation + Migration**: A single PR must not contain code changes, migrations, secondary documentation, or tests along with the canonical schema update (canonical schema updates must be separate).

**Release Checklist**: Prepare a release that includes:
- Schema validation approved by at least one database/schema maintainer. The file located in the main branch of the docs directory is considered validated
- Migration smoke tests run in a test environment
- An updated `release-info.txt` file with a link to the schema/migration summary
- Spec-Kit artifacts in `.cursor` format reflecting the plan and approvals

**Audit Log**: Migration scripts, test results, and Spec-Kit plan IDs should be stored in the merge request and saved in the release artifacts.

#### 5.8 Enforcement & Governance

**Enforcement**: Repository CI must include automated schema-validation steps that run on PRs. Human code review must enforce that checks were added and pass.

**Non-compliant changes**: Any change that circumvents the rules (missing migration or docs) should be rejected by reviewers; persistent deviations must be escalated to governance and tracked in the constitution change log.

**Spec-Kit Integration**: Use Spec-Kit to record the specification, plan and authorization. The Spec-Kit `/specify` artifact becomes part of the PR and is required for merges that touch schema or data lifecycle.

#### 5.9 PR Author Checklist

Before submitting a PR that touches schema or storage:
- [ ] Did you update `spec/04-data-model.md` or `docs/Data_Schema.md` (if relevant)?
- [ ] Do you provide forward and backward migrations?
- [ ] Are schema validation tests added/updated and green in CI?
- [ ] Do contract tests reflect any API/P2P format change?
- [ ] Did you attach or reference a Spec-Kit `/specify` and `/plan`?
- [ ] Did you include release notes for the migration (script location, rollback steps)?
- [ ] If sensitive: did you include a security review step (per `SECURITY.md`)?

#### 5.10 Release Checklist Integration

Release checklists include a database/schema review referencing `[spec/04-data-model.md](spec/04-data-model.md)`, `[docs/Data_Schema.md](docs/Data_Schema.md)`, and migration scripts; releases are blocked if documentation or migrations lag behind.

### Rule 6 — CI, Tooling & Automation Discipline

1. Spec-Kit artifacts (specs, plans, tasks), docs, release scripts, lint/test runners, and validation checks MUST exist, be versioned, and remain runnable by Cursor agents without manual intervention.
2. `[.specify/memory/constitution.md](.specify/memory/constitution.md)` is the canonical constitution; all `/speckit.*` commands reference it and update Spec-Kit templates when new governance gates appear.
3. Templates checked during this amendment (`.specify/templates/{spec,plan,tasks}[-template.md](-template.md)`) stay aligned; missing command templates are tracked until restored.

### Rule 7 — Security & Privacy Enforcement

1. `[SECURITY.md](SECURITY.md)` defines the security model; this constitution requires periodic dependency scans (Dependabot/CodeQL or equivalent) plus manual `cargo audit`, `npm audit`, and Android/Desktop security tooling runs before releases.
2. Remediation SLAs: Critical vulnerabilities addressed within 48 hours, High within 7 days, Medium within 14 days, and documentation recorded in issues/specs.
3. Privacy rules from `[SECURITY.md](SECURITY.md)` (no persistent identity, no telemetry, no plaintext secrets) are binding on every platform and must be validated during code review and release automation.

### Rule 8 — UI Desktop Emoji Accessibility Requirement

1. All Desktop UI interface elements MUST be accompanied by appropriate emojis to improve understanding of element purpose for users who have difficulty understanding the interface language.
2. Emojis must be semantically meaningful and directly related to the function or purpose of the interface element (buttons, menu items, navigation links, form labels, status indicators, etc.).
3. Emoji selection must be consistent across the application and follow established patterns for similar functionality.
4. This requirement applies to all Desktop UI surfaces (Linux/Windows/macOS) and must be validated during UI development, code review, and release automation.
5. Rationale: Emojis provide universal visual cues that transcend language barriers, making the interface more accessible to users regardless of their proficiency in the interface language.

## Philosophical Foundation

The "Truth Training" concept emerged as a response to the crisis of trust and evolved into a philosophical model of collective human self-learning through shared cognition. Its foundation lies at the intersection of science, ethics, and philosophy — from probability theory and behavioral psychology to ancient ideas of harmony and the relativity of observation.

In this view, truth is not declared — it is born through collaboration, correction, and convergence among many minds. Truth is a process, not an object, emerging from continuous interaction among many observers — like rhythm arising from multiple beats.

### Truth as Anonymous Confession

The system enables individuals to express truth without fear of judgment — anonymously and cryptographically verified. Like a digital form of confession, it allows a person to share, acknowledge, or correct information within a collective context. The act of revealing truth itself becomes part of the training — a feedback loop of ethical learning.

### Core Philosophical Principles

**Collective Intelligence as a Neural System of Society**: Each participant acts as a neuron — transmitting, receiving, and adapting — while the network learns through feedback.

**Trust as a Quantifiable Parameter**: Reputation becomes a measurable property, similar to energy or density, integrating ethics with information theory.

**The Wisdom of the Crowd as Natural Selection of Information**: Averaging independent inputs filters out extremes, leaving stable and reproducible approximations of truth.

**Ethics as Algorithm**: Instead of moral judgment or punishment, the system relies on adaptive feedback loops — reducing influence for error, rewarding correction.

**Harmony of Perception as the Goal**: Truth is not an absolute endpoint but a state of dynamic balance achieved through constant exchange and recalibration.

**Truth Without Author**: The platform separates truth from authorship. Validation occurs through distributed resonance — multiple confirmations and rejections — rather than authority or identity.

## Core Technical Principles

### I. Separation of Concerns by Crate and Surface
The system is organized into Rust crates with clear responsibilities and mirrored clients:
- `core` — domain logic, SQLite persistence, and data models; self-contained, independently testable, and documented.
- `server` — Actix Web HTTP API and P2P synchronization layer; exposes API and peer protocols while behaving autonomously in networks reminiscent of FidoNet.
- `app` — hosts both the CLI (`app/src/bin/truthctl.rs`) and launcher logic consumed by desktop/mobile shells for administration and monitoring.
- Desktop and mobile front ends call the same APIs and binaries; any shared logic lives in `core`.

### II. API-, CLI-, and UI-First Interfaces
All capabilities are accessible via HTTP API (`server`), command-line (`app`), desktop UI (Tauri + glib/GTK on Linux), and mobile clients (Kotlin/Swift wrappers):
- Text/JSON I/O: stdin/args → stdout; errors → stderr; JSON and human-readable outputs supported.
- CLI is a first-class client of the API; examples double as contract tests.
- Desktop and mobile UIs reuse the contracts; offline-first workflows are mandatory for desktop/mobile parity.

### III. Cryptographic Integrity (NON-NEGOTIABLE)
All inter-node communication and sensitive API operations must be signed and verifiable.
- Uses `ed25519_dalek` (Rust), platform-specific bindings on Kotlin/Swift, for signing and verifying messages.
- Nodes authenticate requests via public-key headers and request signatures.
- Deterministic serialization for signed payloads; unsigned/invalid requests are rejected.

### IV. Integration Testing Across Layers
Coherence between API, storage, and P2P is enforced via integration tests.
- Contract tests for new/changed endpoints, schemas, and P2P message formats.
- End-to-end flows: node bootstrap, peer discovery, synchronization, conflict handling.
- Signature validation and replay protection are covered by tests.

### V. Observability, Versioning & Simplicity
- Structured logging and trace IDs; text I/O ensures debuggability.
- Semantic versioning per crate (MAJOR.MINOR.PATCH); document breaking changes.
- Prefer simple designs and SQLite; evolve via explicit migrations (YAGNI).

## Collective Intelligence Principles

### VI. Truth as Emergent Consensus
Truth is not stored but converged upon through continuous exchanges. The system should not "store truth" but converge toward it over time. Repeated confirmations strengthen an event; repeated rejections weaken it.

### VII. Dynamic Reputation and Weighted Consensus
Each validator's influence (weight) depends on the accuracy of their past judgments. The collective judgment becomes a weighted consensus of experience rather than a mere vote count.

### VIII. Self-Correcting Information Ecosystem
The system promotes learning and correction rather than punishment — turning truth verification into a form of cognitive training. Each transmission or confirmation has semantic meaning — a signal of trust, correction, or decay.

### IX. Cross-Platform Collective Intelligence
The system maintains platform parity between desktop and mobile implementations, ensuring that collective intelligence can emerge across all access points while respecting platform-specific constraints and capabilities.

## Societal Applications

### I. Anti-Fraud and Integrity Detection

The accumulation of verified events enables probabilistic modeling of truth. Repeated independent confirmations form statistical weight, allowing the system to estimate the authenticity of new claims. Fraud, misinformation, or manipulation attempts decay over time as inconsistent signals lose coherence.

### II. Digital Conscience and Ethical Reflection

By allowing anonymous truth submission and collective verification, the system acts as a distributed moral network — replacing institutional confession with self-organized introspection.

### III. Decentralized Civic Dialogue

Truth Training can function as a decentralized replacement for traditional voting. Instead of casting binary votes, participants express judgments on factual events. Social consensus emerges as a real-time measure of collective reasoning rather than a periodic count.

### IV. Local Mesh of Truth Exchange

Even without central connectivity, nodes can exchange and synchronize events over short-range or opportunistic connections (Wi-Fi, Bluetooth, etc.). This makes the network resilient — similar to LoRa mesh systems but using standard communication layers. Truth spreads naturally, hop by hop, without requiring additional hardware.

## Architecture and Technology Stack

### Programming Languages
- Rust — core logic, P2P communication, API, Tauri backend.
- TypeScript — desktop interface and admin tools.
- Kotlin — Android client, JNI bindings, mobile services.
- Swift — iOS client interop (sharing Kotlin feature parity).
- SQL (SQLite/Postgres) — local embedded data storage.

### Crates and Clients
1. `core` — domain logic, storage, and models.
2. `server` — Actix Web API and P2P synchronization.
3. `app` — CLI plus shared administration logic.
4. Desktop/mobile shells — Tauri/React front end and mobile Compose/SwiftUI clients tethered to the Rust core artifacts.

### Key Modules
- `p2p/` — node synchronization, peer discovery, crypto identity (Node, Sync, Encryption).
- `api.rs` — HTTP endpoints for TruthEvent, Impact, and signature validation.
- `main.rs` — initializes database, spawns node, starts HTTP server.
- `core/storage.rs` — CRUD and data seeding for domain entities.
- Mobile/desktop service bridges — FFI bindings to the Rust core, ensuring schema parity.

### Encryption and Identity
- `ed25519_dalek` for signatures; nodes authenticate via public-key headers and signatures.
- Keys are per-node; rotation follows a governed process; never log private material.

### Future Direction
- Expand Tauri/desktop offline workflows and publish GTK parity tests.
- Keep Kotlin/Swift releases aligned with desktop semantics for offline and network modes.
- Extend `app` crate to integrate with the P2P layer via API.

### Documentation References
- [spec/01-product-vision.md](spec/01-product-vision.md)
- [spec/02-requirements.md](spec/02-requirements.md)
- [spec/03-architecture.md](spec/03-architecture.md)
- [spec/04-data-model.md](spec/04-data-model.md)
- [spec/05-api.md](spec/05-api.md)
- [spec/06-expert-system.md](spec/06-expert-system.md)
- [spec/07-event-rating-protocol.md](spec/07-event-rating-protocol.md)
- [spec/08-p2p-sync.md](spec/08-p2p-sync.md)
- [spec/09-ux-guidelines.md](spec/09-ux-guidelines.md)
- [spec/10-roadmap.md](spec/10-roadmap.md)
- [spec/11-decision-log.md](spec/11-decision-log.md)
- [spec/12-open-questions.md](spec/12-open-questions.md)
- [spec/13-traceability.md](spec/13-traceability.md)
- [spec/14-quality-gates.md](spec/14-quality-gates.md)
- [spec/15-prompts-and-automation.md](spec/15-prompts-and-automation.md)
- [spec/16-test-plan.md](spec/16-test-plan.md)
- [spec/18-cross-platform-architecture.md](spec/18-cross-platform-architecture.md)
- [spec/19-build-instructions.md](spec/19-build-instructions.md)
- [spec/20-cargo-configuration.md](spec/20-cargo-configuration.md)

### Primary Goal
Ensure coherence between API, storage, and P2P layers; maintain cryptographic integrity of communications; provide a foundation for a portable, extensible truth‑evaluation platform that enables collective intelligence through distributed consensus.

## Development Workflow and Quality Gates

- Test-first: follow [spec/16-test-plan.md](spec/16-test-plan.md); Red-Green-Refactor cycle enforced.
- Quality gates: see [spec/14-quality-gates.md](spec/14-quality-gates.md); CI runs fmt, clippy, unit/integration tests; coverage thresholds enforced.
- Reviews: PRs must verify compliance with this constitution and relevant specs.
- Traceability: keep [spec/13-traceability.md](spec/13-traceability.md) in sync; link commits/PRs to requirements.
- Breaking changes: require updates to [spec/03-architecture.md](spec/03-architecture.md), [spec/04-data-model.md](spec/04-data-model.md), [spec/05-api.md](spec/05-api.md), [spec/08-p2p-sync.md](spec/08-p2p-sync.md) and a migration plan.
- Storage migrations: provide forward/backward migrations and seed updates in `core` while meeting the normal form guarantees in Rule 5.
- API/P2P contracts: add/extend contract tests; bump versions; document in [spec/11-decision-log.md](spec/11-decision-log.md).
- Release process: per-crate semver bump, changelog entry, artifacts; maintain compatibility notes in specs; ensure README install sections reflect the four release surfaces before running `create-release.sh`.
- Release automation must also confirm glib version checks, dependency audits, and schema reviews, logging outcomes in `[CHANGELOG.md](CHANGELOG.md)` and the versioned release-info files.

## Governance

This constitution supersedes other practices. Amendments require documentation, approval, and a migration strategy.
- All PRs/reviews must assert compliance and link to updated specs.
- Complexity must be justified; prefer simple, observable solutions.
- Cryptographic/protocol changes require security review and test evidence.
- Use [spec/15-prompts-and-automation.md](spec/15-prompts-and-automation.md) for automation and agent guidance; `/speckit.*` commands MUST confirm they sourced assumptions from this file.
- Collective intelligence principles must be preserved in all architectural decisions.
- Truth training methodology must be reflected in all user-facing interfaces and data flows.
- Acceptance criteria: this file (and associated Spec-Kit state) stays at `[.specify/memory/constitution.md](.specify/memory/constitution.md)`, referenced by all governance-related commands, and deviations from authoritative docs are captured in the Change History table.

## Change History

| Date       | Version | Author     | Notes                                                                                                    | Deviations vs README/CONTRIBUTING/SECURITY/CHANGELOG |
|------------|---------|------------|----------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| 2025-12-28 | 2.4.0   | Cursor AI  | Expanded Rule 5 — Database & Schema Integrity with operational supplement covering canonical sources, schema distortion prevention, dual-database allocation, data movement rules, migration gates, traceability, and enforcement. | None; aligned with referenced docs                   |
| 2025-12-09 | 2.3.0   | Cursor AI  | Added Rule 8 — UI Desktop Emoji Accessibility Requirement for improved interface comprehension.        | None; aligned with referenced docs                   |
| 2025-12-01 | 2.2.0   | Cursor AI  | Added explicit cross-platform scope, release automation, dependency/DB policies, Spec-Kit enforcement.  | None; aligned with referenced docs                   |
| 2025-10-31 | 2.1.0   | Maintainers | Prior governance uplift aligning constitution with anonymous confession and collective intelligence.     | Not recorded                                          |

**Version**: 2.4.0 | **Ratified**: 2025-10-31 | **Last Amended**: 2025-12-28
