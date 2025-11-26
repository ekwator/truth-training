# Quickstart — Validating Documentation Refactoring v1.0.0

**Goal**: Provide a repeatable checklist to validate that the documentation refactor meets link integrity, structure, and `/docs` vs `/spec` separation goals.

---

## 1. Pre-checks

1. Ensure you are on branch `001-documentation-refactoring-0-0`.  
2. Confirm that `specs/001-documentation-refactoring-0-0/spec.md` and `[plan.md](specs/001-documentation-refactoring-0-0/plan.md)` are present.  

---

## 2. Link Integrity Walk (Human + Tool)

1. Open `README.md` in a browser or Markdown viewer.  
2. Manually click through:
   - All links in the top-level navigation.  
   - Links into `/docs` (e.g., platform comparisons, migration guides).  
   - Link into `spec/README.md`.  
3. Verify:
   - No 404s or “file not found” errors.  
   - You do not see references to removed files (e.g., old comparison filenames).  

**Optional scripted check** (example outline; implementation can vary):
```bash
# From repo root, run a simple link checker (pseudo-command)
./scripts/check_markdown_links.sh
```
The script should:
- Traverse `README.md`, `/docs`, `/spec`.  
- Resolve relative Markdown links.  
- Report any missing `.md` targets.

---

## 3. `/docs` vs `/spec` Role Separation

### 3.1 /spec as AI entrypoint

1. Open `spec/README.md`.  
2. Confirm that it:
   - States that `/spec` is the primary decision source for AI agents.  
   - Links to core specs: product vision, requirements, architecture, API, test plan, traceability.  

3. Using only files under `/spec`, answer these questions:
   - What is the current baseline version?  
   - Which components are at v1.0.0?  
   - Where are API contracts documented?  
If you can answer correctly without reading `/docs`, the separation is working.

### 3.2 /docs as human deep dive

1. Open `docs/Truth-training/Truth-training.md`.  
2. Follow links to migration guides, test reports, and API reference.  
3. Confirm that:
   - Detailed explanations live under `/docs`.  
   - When a structural truth is needed (e.g., exact API contract), `/docs` links back to `/spec` rather than duplicating it.  

---

## 4. README.md Depth Check

1. Open `README.md`.  
2. Confirm:
   - It contains a short project overview, version badges, and platform matrix.  
   - Release details, migration steps, and full API docs have been moved into `/docs` and replaced by short summaries + links.  
   - There is a clear link to `spec/README.md` for AI/spec consumers.  

Success criteria:
- `README.md` feels like a concise landing page, not a full manual.

---

## 5. Reachability of Design Docs

1. Open `docs/DESIGN_INDEX.md`.  
2. Confirm:
   - It links to major design and integration docs under `docs/` and `specs/*`.  
   - Feature spec-kits (e.g., `specs/001-documentation-refactoring-0-0/`) are reachable either from this index or from `spec/13-traceability.md`.  

3. Spot-check at least three randomly chosen design docs (e.g., from `spec/03-architecture.md`, `spec/05-api.md`, `docs/CI_Workflows_Artifacts.md`) and ensure each is reachable in at most two clicks from:  
   - `README.md`, or  
   - `docs/DESIGN_INDEX.md`, or  
   - `spec/README.md`.

---

## 6. Version Consistency Check

1. Open `docs/VERSION_REGISTRY.md`.  
2. Confirm:
   - v1.0.0 is listed as the baseline for core, desktop, and Android.  
3. Cross-check:
   - `README.md` version references.  
   - A sample of `/spec` documents that mention versions.  
4. Ensure no contradictions (e.g., no doc still calling Android “0.3.0 baseline”).

---

## 7. Completion Criteria

The refactor is considered **validated** when:
- All manual link clicks and any link-check scripts report **no broken `.md` links**.  
- `/spec` alone is sufficient for an AI agent to understand v1.0.0 capabilities and contracts.  
- `/docs` provides detailed human context without contradicting `/spec`.  
- `README.md` is concise and redirects readers appropriately.  
- All relevant docs are reachable from the index documents within two clicks.


