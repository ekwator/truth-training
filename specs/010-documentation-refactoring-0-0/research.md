# Research: Documentation Refactoring v1.0.0

**Feature**: Full documentation refactor & link integrity  
**Date**: 2025-11-27  
**Status**: Complete

## Research Questions

### 1. How do we build a complete Markdown inventory without breaking validator depth rules?
**Decision**: Walk the repo with `Path.rglob('*.md')`, skip `[CONTRIBUTING.md](CONTRIBUTING.md)(C[ONTRIBUTING.md](ONTRIBUTING.md))`, `LICENSE.txt`, `[SECURITY.md](SECURITY.md)(S[ECURITY.md](ECURITY.md))`, `[CHANGELOG.md](CHANGELOG.md)(C[HANGELOG.md](HANGELOG.md))`, and coerce any `README*.md` depth to 0. Depth derives from the number of path segments under repo root, capped at 3 to keep navigation shallow.  
**Rationale**: Prior validator failures originated from README files inheriting folder depth. Explicit coercion plus capped depth keeps the navigation graph predictable.  
**Alternatives**: Custom glob lists (fragile), manual manifest (incomplete).  
**References**: Existing `scripts/doc_refactor/inventory.py`, Speckit docs on README depth enforcement.

### 2. What is the safest way to convert plaintext `.md` paths into Markdown links?
**Decision**: Tokenize Markdown via `mistune`, look for `spec/*.md` or `docs/*.md` substrings in text nodes, and rewrite them to `[path](path)` if no link already exists. Maintain a ledger of replacements for auditing.  
**Rationale**: Regex-only approaches miss nested emphasis or code blocks; parsing AST avoids double-linking and respects escaped content.  
**Alternatives**: Blanket regex replace (risk of code fences), manual editing (impractical).  
**References**: `mistune` AST docs, Markdown CommonMark spec, prior linter failures.

### 3. How do we ensure `/docs` stays deep while `/spec` remains compressed for AI agents?
**Decision**: Introduce `spec_optimizer.py` that enforces <80-word paragraphs, mandates "Use /spec before /docs" admonition in `[spec/README.md](spec/README.md)(s[pec/README.md](pec/README.md))`, and keeps decision/context sections in bullet lists. `/docs` receives the detailed sections extracted from README + specs, with archive relocation for pre-v1.0.0 materials.  
**Rationale**: Aligns with AI-first constitution goals and success criteria SC-003/SC-005. Automation prevents regressions during future edits.  
**Alternatives**: Manual editing (error-prone), soft guidance only (no enforcement).  
**References**: Feature spec success criteria, prior doc-refactor prototypes.

### 4. How will we detect and resolve duplicate content efficiently?
**Decision**: Keep `duplicate_detector.py` but clamp cosine scores (already fixed) and add report categories: `probable_duplicate`, `needs_merge`, `safe`. Use TF-IDF with stopword removal tuned for documentation (English + code tokens).  
**Rationale**: Duplicate narratives caused user confusion; automated clustering surfaces candidates for merge or archival.  
**Alternatives**: Manual audits (too slow), heuristics based on headings only (miss paraphrased duplicates).  
**References**: scikit-learn TF-IDF guide, previous bug fixes in duplicate detector.

### 5. What versioning and runtime policies should govern the CLI?
**Decision**: All generated/updated Markdown files must explicitly mention `v1.0.0`. Version sync runs as a dedicated phase after restructuring. Runtime clarified to have *no strict upper bound* (FR-010); instrumentation logs elapsed times plus memory snapshots for observability.  
**Rationale**: Ensures compliance with release notes and avoids premature optimization that could skip fixes.  
**Alternatives**: Hard runtime cap (risks partial refactors), manual version edits (inconsistent).  
**References**: Clarification answer, success criteria, previous runtime failures on large repos.

