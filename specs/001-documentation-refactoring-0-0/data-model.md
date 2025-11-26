# Data Model — Documentation Refactoring v1.0.0

**Feature**: Documentation Refactoring v1.0.0  
**Branch**: `001-documentation-refactoring-0-0`  
**Spec**: `specs/001-documentation-refactoring-0-0/spec.md`

---

## 1. DocumentationFile

Represents any Markdown file in the repository.

- **Attributes**
  - `path: String` — repository-relative file path (e.g., `docs/Truth-training/Truth-training.md`).  
  - `role: Enum` — `{ README, INDEX, GUIDE, SPEC, REPORT, OTHER }`.  
  - `audience: Enum` — `{ HUMAN, AI, BOTH }`.  
  - `version_scope: Enum` — `{ GLOBAL, V1_0_0, HISTORICAL }`.  
  - `reachable_from: Set<Path>` — indices from which this file is reachable (e.g., `["README.md", "docs/DESIGN_INDEX.md"]`).  
  - `excluded_from_refactor: Bool` — true for `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, `CHANGELOG.md`.  

- **Validation Rules**
  - Every `DocumentationFile` with `role` in `{INDEX, GUIDE, SPEC, REPORT}` and `version_scope != HISTORICAL` MUST be reachable from at least one index (`README.md`, `docs/DESIGN_INDEX.md`, or `spec/README.md`).  
  - Files marked `excluded_from_refactor` MUST NOT be renamed or structurally rewritten, though link additions are allowed.  

---

## 2. Link

Represents a Markdown link or implicit path reference between documentation files.

- **Attributes**
  - `source_path: String` — file containing the link.  
  - `target_path: String` — normalized target (if it resolves to a `.md` file).  
  - `raw_text: String` — original Markdown or plaintext representation.  
  - `is_markdown_link: Bool` — true if already `[text](target)`.  
  - `is_plain_path_candidate: Bool` — true if `raw_text` looks like a repo-relative path to an `.md` file.  

- **Validation Rules**
  - If `target_path` exists and points to a `.md` file, link MUST be expressed as a Markdown hyperlink `[...](target_path)` in final docs.  
  - If `target_path` does not exist, the refactor MUST either create the file (with v1.0.0 content) or update the link to a valid existing target.  

---

## 3. IndexDocument

Top-level navigational document that lists and categorizes other docs.

- **Examples**
  - `README.md`  
  - `docs/DESIGN_INDEX.md`  
  - `spec/README.md`  

- **Attributes**
  - `path: String`  
  - `children: Set<Path>` — direct doc paths it links to.  
  - `scope: Enum` — `{ ROOT, DOCS, SPEC }`.  

- **Validation Rules**
  - `README.md` MUST link to:
    - high-level `/docs` entrypoints (e.g., platform overview, main guides), and  
    - `/spec/README.md` as the AI/spec index.  
  - `docs/DESIGN_INDEX.md` MUST link to all design-related docs under `docs/` and `specs/*` that are not already reachable from `README.md`.  
  - `spec/README.md` MUST explain the role of `/spec` and link to the key specs (product vision, requirements, architecture, API, test plan, traceability).  

---

## 4. SpecDocument

Compact, AI-oriented specification under `/spec`.

- **Attributes**
  - `path: String` — e.g., `spec/05-api.md`.  
  - `audience: Enum` — always `{ AI, BOTH }`.  
  - `canonical_for: Set<String>` — conceptual areas for which this doc is the source of truth (e.g., `["API", "DataModel"]`).  

- **Validation Rules**
  - Spec documents MUST avoid long narrative sections; they should focus on requirements, models, contracts, and success criteria.  
  - If a concept is canonical in a `SpecDocument`, related `/docs` material MUST point back to this spec rather than redefining behavior.  

---

## 5. HumanGuide

Narrative documentation meant primarily for human readers under `/docs`.

- **Attributes**
  - `path: String` — e.g., `docs/ANDROID_MIGRATION.md`.  
  - `linked_specs: Set<Path>` — related spec documents (e.g., `["spec/05-api.md"]`).  

- **Validation Rules**
  - Guides MUST explain behavior, rationale, and operational steps, but MUST NOT contradict their linked specs; in case of conflict, specs are canonical.  
  - Guides SHOULD link back to their corresponding specs for deeper structural detail.  

---

## 6. VersionTag

Represents versioning information used across docs.

- **Attributes**
  - `version: String` — e.g., `"1.0.0"`.  
  - `applies_to: Set<String>` — components (e.g., `["core", "desktop", "android"]`).  

- **Validation Rules**
  - All v1.0.0 documentation touched by this refactor MUST refer to the same baseline version and MUST NOT present conflicting version statements for the same component.  
  - `docs/VERSION_REGISTRY.md` and any version mentions in `README.md` and `/spec` MUST remain consistent.

---

## 7. LinkGraph (Conceptual)

The aggregate of `DocumentationFile` nodes and `Link` edges.

- **Validation Rules**
  - Every `DocumentationFile` node with `role != OTHER` MUST be reachable from at least one `IndexDocument` within two hops.  
  - There MUST be no edges pointing to non-existent `.md` files after the refactor.  


