Update Constitution — Data Model & Schema Supplement

1. Authority and Canonical Sources

The canonical descriptions of the model and schema are the authoritative sources for DB structure and semantics:
[model_core.md](../../../../../docs/model_core.md) — canonical markdown Formalized Model Core and Database Schema;  
[04-data-model.md](../../../../../spec/04-data-model.md) — canonical SQL schema specifications for implementers.  
[26-seed_knowledge_base_table_value.md](../../../../../spec/26-seed_knowledge_base_table_value.md) — Knowledge Base Table Values for Default Seeding.  
[Data_Schema.md](../../../../../docs/Data_Schema.md) — canonical markdown schema specifications for implementers;  
[SECURITY.md](../../../../../SECURITY.md) — security and verification requirements;  
[CONTRIBUTING.md](../../../../../CONTRIBUTING.md) — quality and testing requirements;  
[14-quality-gates.md](../../../../../spec/04-data-model.md) — minimum requirements for PR acceptance.  


Rule (Authority): Any change to the runtime DB schema, table names, primary/foreign key types, or semantic meaning of fields must be reconciled with — and implemented as — updates to the canonical files above. Implementations that diverge without an approved migration plan violate the constitution.

2. Normative Rule — Preventing Schema Distortion

Single Source of Truth: The schema described in docs/model_core.md is authoritative. Implementations (core, desktop, android, server, cli) must target those files as the ground truth for table names, column types, PK/FK definitions, and indexes. 

constitution

No Shadow Schemas: Implementations may not retain or ship divergent table names, key types (e.g., TEXT vs INTEGER PK for the same logical entity), or incompatible FK constraints without a formally approved migration and a side-by-side compatibility plan.

Declaration Requirement: Every PR that alters storage code or DB DDL must include:

Updated canonical schema files (04-data-model.md / docs/Data_Schema.md) and/or mat_model.md if the change is conceptual. 
- Forward and backward migration scripts.
- Schema validation tests (see Section 5).
- A Spec-Kit plan /specify → /plan describing the migration rollout and compatibility strategy.

3. Dual-Database Allocation (normative)

To keep responsibilities clearly separated and to reduce risk of cross-concern changes, the repository standardizes two local database files for embedded/local persistence:

truth_training.sqlite — primary domain DB: truth_events, impact, judgments, participants, progress_metrics, knowledge base tables (category, forma, etc.). This DB contains the event and assessment history and must follow the strict Quality Gates for truth/judgments and impacts.

discovery_nodes.sqlite — discovery and network metadata: nodes list, reachability, TTLs, registry snapshots, behavioral signatures, node trust limits, and ephemeral discovery caches. This DB may have shorter TTLs, separate lifecycle rules and a different backup cadence.

Requirement: Migrations and changes affecting either DB must be explicitly targeted to the correct file and documented with which DB they affect.

4. Data Movement, Mutation & History Rules

Append-Only for Judgment History: Judgments and their versions are historical records. Do not silently overwrite judgment rows. Use version tables (or append versions) to keep complete history. (This is mandated for auditability and reproducibility.) 

Impact Immutability Constraints: Impact records must remain bound to their originating event and preserve timestamps. Deletions of impact records are allowed only via an explicit cleanup script with justification logged and approved.

No Silent Deletes: Any operation that removes historical data must be documented, batched, and reversible (via backups). Quiet or automatic deletion that is not approved by a migration/cleanup plan is forbidden.

Signed Records: Any judgment/impact/critical append must include verifiable cryptographic metadata (signature, public key or proof) where the spec requires it. Unsigned critical updates must be rejected or downgraded in sanity checks. 

TTL & Cleanup: For discovery and ephemeral caches only (e.g., in discovery_nodes.sqlite) apply TTL and automated cleanup, but preserve an audit log of removals and reasons. TTL rules must be part of the migration/change plan.

5. Migration, Validation & Quality Gates (enforced)

Every schema change or storage-related code change must pass these gates before merging:

Spec Update Gate: The change must be described in a Spec-Kit spec /specify and approved plan /plan. The PR must reference the spec and include the generated plan ID. (Spec-Kit is mandatory per project rules.) 

Migration Scripts: Provide forward and backward SQL migrations (or programmatic migrations for non-SQL changes). Each migration must include:
- data transformation steps,
- verification queries,
- rollback procedure,
- budgeted downtime (if any).
- Schema Validation Tests: Automated tests that:
- assert table presence and column types,
- validate PK/FK integrity,
- verify indices that the performance expectations depend on,
- run PRAGMA/schema diffs used by CI.
- These are part of CI Quality Gates (see spec/14-quality-gates.md). 

Contract Tests: Any API or P2P message that depends on schema must include contract tests that fail fast if schema and message format drift.

Behavior Tests (Quality): For Judgment and Impact axes include:
- cryptographic signature validation tests,
- immutability/append tests,
- aggregation correctness tests (non-regression on aggregator functions).

Blocking Policy: Failing schema/migration tests block merge and release — per constitution Rule 5. 

6. Traceability, Documentation, and Releases

One PR = Canonical Schema
One PR = Code + Documentation + Migration

A canonical schema change must include updated schema documentation:
- 1 The main canonical schema file docs/model_core.md - cannot be edited and must be pre-approved. 
- 2 semantic changes to 04-data-model.md, Data_Schema.md, 26-seed_knowledge_base_table_value.md, corresponding to the data in docs/model_core.md
- 3 In the file 26-seed_knowledge_base_table_value.md, table field values ​​cannot be edited; only the database schema in Data_Schema.md must be reviewed and corrected.

A single PR must not contain code changes, migrations, secondary documentation, or tests along with the canonical schema update.

Release Checklist: Prepare a release that includes:
- schema validation approved by at least one database/schema maintainer. The file located in the main branch of the docs directory is considered validated,
- migration smoke tests run in a test environment,
- an updated release-info.txt file with a link to the schema/migration summary,
- Spec-Kit artifacts in .cursor format reflecting the plan and approvals.

Audit Log: Migration scripts, test results, and Spec-Kit plan IDs should be stored in the merge request and saved in the release artifacts.

7. Enforcement & Governance

Enforcement: Repository CI must include automated schema-validation steps that run on PRs. Human code review must enforce that checks were added and pass.

Non-compliant changes: Any change that circumvents the rules (missing migration or docs) should be rejected by reviewers; persistent deviations must be escalated to governance and tracked in the constitution change log. 

Spec-Kit Integration: Use Spec-Kit to record the specification, plan and authorization. The Spec-Kit /specify artifact becomes part of the PR and is required for merges that touch schema or data lifecycle.

8. Short, Practical Checklist (for PR authors)

- Did you update spec/04-data-model.md or docs/Data_Schema.md (if relevant)? 
- Do you provide forward and backward migrations?
- Are schema validation tests added/updated and green in CI?
- Do contract tests reflect any API/P2P format change?
- Did you attach or reference a Spec-Kit /specify and /plan?
- Did you include release notes for the migration (script location, rollback steps)?
- If sensitive: did you include a security review step (per SECURITY.md)?

9. Relationship to Rule 5 — Database & Schema Integrity

This supplement operationalizes Rule 5 — Database & Schema Integrity from the Constitution by:
- requiring canonical doc updates for every schema change (prevents hidden drift);
- mandating migrations with testable forward/backward operations;
- enforcing append-only and auditable histories for judgments/impact axes;
- splitting concerns (truth domain vs discovery metadata) into two DB files to reduce accidental coupling.

10. Formal Normative Clause (to append to Constitution)

Norm (Schema Integrity & Migration): All storage-level changes that alter table names, primary/foreign key types, column semantics, or data lifecycle semantics shall be blocked from merging until: (1) an authoritative update to spec/04-data-model.md and docs/Data_Schema.md (and mat_model.md if conceptual semantics changed) is included in the same PR; (2) migration scripts (forward & rollback) are present; (3) schema validation and contract tests pass in CI; and (4) a Spec-Kit /specify and approved /plan are attached to the PR. Violation of this norm is a constitutional breach and must be remediated before release.

11. Closing note (why this matters)

The relational schema is the material substrate of the system's cognitive model: careless or undocumented schema changes risk destroying auditability, reproducibility, and the emergent properties (independence of impact and judgments) that the system encodes. The rules above make schema changes explicit, tested, reversible and traceable — turning the model from theory into verifiable, production-grade infrastructure.
