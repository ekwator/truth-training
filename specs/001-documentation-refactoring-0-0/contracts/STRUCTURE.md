# Structural Contracts — Documentation Refactoring v1.0.0

**Purpose**: Define structural rules for documentation after the v1.0.0 refactor. These contracts are enforced by review and future tooling (e.g., link checkers), not by runtime code.

---

## 1. Directory Roles

1. `/docs`  
   - Primary home for human-readable, narrative documentation.  
   - Contains: guides, migration docs, architecture narratives, test reports, troubleshooting, release summaries.  

2. `/spec`  
   - Primary home for compressed, decision-ready specs for AI agents and architects.  
   - Contains: product vision, requirements, architecture, API contracts, test plans, traceability, quality gates.  

3. `/specs/*`  
   - Feature-specific spec kits (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `tasks.md`).  

---

## 2. Index Requirements

1. `README.md` MUST:
   - Provide a concise overview, version badges, and platform matrix.  
   - Link to:
     - main human docs entrypoints in `/docs/`, and  
     - `spec/README.md` as the AI/spec index.  
   - NOT embed long release notes, full API references, or detailed test reports.  

2. `docs/DESIGN_INDEX.md` MUST:
   - List all design-related docs under `docs/` and `specs/*` that are not already obvious from `README.md`.  
   - Serve as a single index for design-oriented human readers.  

3. `spec/README.md` MUST:
   - State that `/spec` is the primary decision source for AI agents.  
   - Instruct agents to consult `/spec` first, then `/docs` for narrative context.  
   - Link to core specs: product vision, requirements, architecture, API, test plan, traceability, quality gates.

---

## 3. Link Integrity

1. All file-path-like references to existing `.md` files (e.g., `spec/01-product-vision.md`) MUST be expressed as Markdown links in the final docs:  
   - Example: `spec/01-product-vision.md` → `[spec/01-product-vision.md](spec/01-product-vision.md)`.  

2. Every Markdown link whose target ends with `.md` MUST resolve to an existing file.  

3. No documentation file MAY link to removed or renamed files; such links MUST be updated or removed during refactor.

---

## 4. Reachability

1. Every non-trivial documentation file (`role` in `{README, INDEX, GUIDE, SPEC, REPORT}`) MUST be reachable from at least one index document within two clicks:  
   - Root index: `README.md`  
   - Docs index: `docs/DESIGN_INDEX.md`  
   - Spec index: `spec/README.md`  

2. Feature-specific spec-kits under `specs/*` MUST be reachable from at least one of:  
   - `spec/13-traceability.md`  
   - `docs/DESIGN_INDEX.md`  

---

## 5. Depth vs. Detail

1. Top-level docs (e.g., `README.md`, `spec/README.md`) MUST contain only high-level summaries and navigation links; deep technical detail MUST be moved into `/docs` or corresponding `/spec` files.  

2. `/docs` and `/spec` MUST avoid duplicating each other’s core content:
   - Structural truth (what the system does, API shapes, invariants) → `/spec`.  
   - Narrative explanations, examples, and migration stories → `/docs`.  

3. For any shared concept:
   - Declare exactly one canonical file (either in `/spec` or `/docs`).  
   - All other mentions MUST link back to the canonical source instead of re-specifying behavior.

---

## 6. Versioning

1. v1.0.0 is the baseline for all updated docs in this refactor.  
2. Historical descriptions MUST be framed explicitly as “before v1.0.0” and SHOULD live in migration or release documents rather than separate archived trees.  
3. `docs/VERSION_REGISTRY.md` is the canonical version map; any version mention in `README.md` or `/spec` MUST be consistent with it.


