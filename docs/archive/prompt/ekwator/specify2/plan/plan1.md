<!-- Archived from [docs/prompt/ekwator/specify2/plan/plan1.md](docs/prompt/ekwator/specify2/plan/plan1.md) -->

/speckit.plan Title: Documentation Refactoring v1.0.0 — Structure, Linking, and Integrity

Objective:
Refactor all project documentation for Truth Training v1.0.0 to ensure full hyperlink integrity, eliminate duplication, restructure the hierarchy of information, and separate human-readable documentation (/docs) from AI-oriented compressed specifications (/spec). All documentation must be accurate for version 1.0.0.

1. Scope
In scope:

All .md files in the repository.

Navigation and linking between documents.

Structural re-organization of documentation.

Consistency and correctness of all content.

Out of scope:

Do not modify the following files (only check links pointing to them):

[CONTRIBUTING.md](CONTRIBUTING.md)

LICENSE.txt

[SECURITY.md](SECURITY.md)

[CHANGELOG.md](CHANGELOG.md)

2. Tasks
Phase 1 — Audit & Planning

Build a full inventory of all .md files across the entire repository.

Identify all documents currently referenced by:

[README.md](README.md)

/docs/**

/spec/**

Freeze the final target structure:

/docs   → full detailed human documentation  
/spec   → compressed, decision-oriented specification for AI agents


Produce a summary of discovered inconsistencies and missing documents.

Phase 2 — Link Integrity Pass

For every markdown file, starting from [README.md](README.md):

Detect all references to .md paths (e.g., spec/01-product-vision.md).

Convert every plain-text path into a clickable link:
spec/01-product-vision.md → [spec/01-product-vision.md](spec/01-product-vision.md)

For each referenced file:

If it exists → validate and update for v1.0.0.

If it does not exist → create it with minimal correct v1.0.0 content.

Track processed files in a registry.

After completing traversal:

All unreferenced .md files must be linked from:

[README.md](README.md), or

[docs/INDEX.md](docs/INDEX.md), or

[spec/INDEX.md](spec/INDEX.md).

Goal: zero orphaned documents.

Phase 3 — Restructuring & Cleanup

Move detailed sections (e.g., "Release Information") out of [README.md](README.md) into /docs.

Keep only concise high-level summaries in:

[README.md](README.md) (overview)
docs/ (details)
spec/ (compressed AI-facing spec)


Remove duplicated explanations across documents.

Ensure /spec contains:

condensed descriptions,

clear behavioral rules,

short models,

no narratives or examples unless required for correctness.

Add an explanation in [spec/README.md](spec/README.md):

AI agents MUST prioritize /spec when making decisions.

3. Requirements & Constraints

Must maintain consistency with:

project version v1.0.0

existing design principles in [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md)

Every .md mention must be a valid hyperlink.

No dead links.

Directory hierarchy must reflect:

depth = detail,

top → summary, lower → details.

4. Deliverables

Cursor-agent must produce:

Updated full specification (/spec)
compressed, structural, cross-linked.

Updated human documentation (/docs)
fully detailed, correct for v1.0.0.

Updated root [README.md](README.md)
short, clean, linking deeper documents.

Inventory report
list of:

all .md files

all added files

all modified files

all fixed links

Broken link report
(before → after fixes)

Tasks for /speckit.plan
automatically derived from this specification.

5. Success Criteria

✔ All .md references are clickable hyperlinks.

✔ No broken or dead links.

✔ All documentation is accurate for v1.0.0.

✔ [README.md](README.md) is short and clean.

✔ /docs contains full detail; /spec contains compressed context.

✔ No content duplication.

✔ The documentation tree is logically hierarchical.

✔ All tasks can be executed automatically by Cursor-agent.
