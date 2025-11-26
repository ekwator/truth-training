# Phase 0 Research — Documentation Refactoring v1.0.0

**Feature**: Documentation Refactoring v1.0.0  
**Branch**: `001-documentation-refactoring-0-0`  
**Spec**: `specs/001-documentation-refactoring-0-0/spec.md`

---

## 1. Scope & Exclusions

### Decision 1 — Files in scope
- **Decision**: Include all `.md` files in the repository in the audit, except explicitly excluded files.  
- **Rationale**: Link integrity and structural clarity require a global view of the documentation graph; partial analysis risks leaving “stuck” docs.  
- **Alternatives considered**:
  - Limit to `docs/` and `spec/` only → Rejected because root-level and feature-specific specs (`specs/*`) also affect navigation.

### Decision 2 — Excluded files
- **Decision**: Exclude `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, and `CHANGELOG.md` from structural refactoring (but not from being linked).  
- **Rationale**: These files follow common OSS conventions and may be consumed by external tooling; restructuring them adds risk with little benefit.  
- **Alternatives considered**:
  - Refactor or move these files under `docs/` → Rejected to avoid breaking expectations and URLs.

---

## 2. Historical Documentation Policy

### Decision 3 — Historical docs handling
- **Decision**: Update historical (pre‑v1.0.0) documents **in-place** to reflect v1.0.0 behavior and terminology; capture history in release notes and migration docs (`docs/ANDROID_MIGRATION.md`, `docs/Truth-training/Truth-training.md`, etc.) instead of keeping separate archives.  
- **Rationale**: A single up-to-date doc per topic reduces confusion and avoids link duplication; history is better expressed as “what changed” sections than as parallel doc trees.  
- **Alternatives considered**:
  - Maintain `/docs/archive` with frozen historical docs → Rejected to avoid fragmenting search results and doubling maintenance.  
  - Delete old docs outright → Rejected due to loss of auditability and context.

---

## 3. Roles of /docs, /spec, README.md

### Decision 4 — `/docs` role
- **Decision**: `/docs` is the primary location for deep, human-readable documentation: guides, migration documents, architecture narratives, release/prod notes, test reports, troubleshooting.  
- **Rationale**: Human operators and contributors expect narrative docs under `/docs`; aligning to this convention keeps onboarding low-friction.  
- **Alternatives considered**:
  - Split docs across multiple top-level dirs (e.g., `/guides`, `/manual`) → Rejected as unnecessary fragmentation for this version.

### Decision 5 — `/spec` role
- **Decision**: `/spec` is the primary location for compressed, decision-ready specifications for AI agents and architecture reviewers (requirements, data models, API contracts, test plans). Specs must be concise and non-narrative.  
- **Rationale**: Agents work best with structured, dense context; isolating this in `/spec` reduces noise and drift relative to human docs.  
- **Alternatives considered**:
  - Mix specs into `/docs` → Rejected to keep a clean separation of audiences and maintainability.

### Decision 6 — `README.md` role
- **Decision**: `README.md` serves as a **thin entrypoint**: high-level overview, version badges, platform matrix, and links into `/docs` and `/spec`—no long release notes, API listings, or test reports.  
- **Rationale**: Keeps the project homepage scannable while still directing readers to deeper content.  
- **Alternatives considered**:
  - Keep detailed “What’s New” and API sections inline → Rejected as it duplicates release docs and API reference, violating “depth = detail”.

---

## 4. Link Graph & Integrity Strategy

### Decision 7 — Traversal root and reachability
- **Decision**: Use `README.md` as the primary traversal root for link integrity; supplement with `docs/DESIGN_INDEX.md` and `spec/README.md` as secondary roots for design and spec surfaces.  
- **Rationale**: `README.md` is the natural entrypoint; design and spec indices ensure all deeper design docs are reachable within two clicks.  
- **Alternatives considered**:
  - Only check links locally within each directory → Rejected because orphaned but important docs could remain hidden.

### Decision 8 — Plain path normalization
- **Decision**: Treat any plain text matching an existing `.md` path (e.g., `spec/01-product-vision.md`) as a candidate to normalize into a Markdown link.  
- **Rationale**: Normalization ensures consistent navigation and tooling support (e.g., IDE link click, GitHub rendering).  
- **Alternatives considered**:
  - Leave plain paths as-is if context suggests “code” → Rejected; even in code blocks, explicit links are usually better for docs, and edge cases can be reviewed manually.

---

## 5. Compliance with Constitution

### Decision 9 — No runtime behavior change
- **Decision**: The refactor explicitly avoids any change to cryptographic logic, APIs, P2P behavior, or storage schemas; all changes are limited to `.md` files.  
- **Rationale**: Keeps the feature aligned with the constitution’s emphasis on preserving contract integrity unless explicitly versioned and tested.  

### Decision 10 — Traceability and quality gates
- **Decision**: Ensure design docs referenced by the constitution (e.g., `spec/03-architecture.md`, `spec/05-api.md`, `spec/13-traceability.md`, `spec/16-test-plan.md`) remain reachable from the new indices and from `README.md`.  
- **Rationale**: Maintains traceability and supports existing quality gates that rely on those specs.  

---

## 6. Open Questions (Deferred to /tasks or Implementation)

These are noted but not blocking the high-level plan:
- Whether to add an automated doc-link check into CI in this feature or as a follow-up.  
- How aggressively to remove minor redundancies vs. leaving some repetition for usability (to be decided file-by-file during implementation).


