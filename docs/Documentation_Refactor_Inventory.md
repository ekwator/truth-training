# Documentation Refactor — Inventory Report (v1.0.0)

This document explains how to generate and interpret the Markdown inventory used by the Truth Training documentation refactor workflow.

## 1. Running the Inventory Phase

From the repository root:

```bash
python scripts/doc_refactor/main.py run --phases inventory
```

This command:

- Traverses the repository and finds all `*.md` files (including root, `docs/`, `spec/`, `.github/`, and `specs/`).
- Classifies each file as `root`, `docs`, `spec`, or `other`.
- Writes a machine-readable report to:

```text
reports/doc_refactor/inventory.json
```

## 2. inventory.json Structure (Conceptual)

The report contains a list of documentation nodes and summary information. Conceptually:

```json
{
  "generated_at": "ISO-8601 timestamp",
  "nodes": [
    {
      "path": "docs/README.md",
      "audience": "docs",
      "version_tag": "v1.0.0",
      "linked_from": ["README.md"],
      "links_to": ["docs/Deployment.md"],
      "is_orphan": false,
      "is_excluded": false
    }
  ],
  "summary": {
    "total_nodes": 0,
    "role_readme": 0,
    "role_spec": 0,
    "audience_docs": 0,
    "audience_spec": 0,
    "orphans": 0
  }
}
```

Exact fields are defined by the `DocumentationFile` / `DocumentationNode` model in `scripts/doc_refactor/models/documentation_file.py`.

## 3. How to Use the Inventory

- **Coverage checks**: Verify that every Markdown file in the repo appears as a node in the report.
- **Audience classification**: Use the `audience` field to confirm that root-level entry points ([README.md](../README.md), [docs/README.md](README.md), [spec/README.md](../spec/README.md)) are correctly identified.
- **Link coverage**: `linked_from` / `links_to` lists mirror the `ReferenceEdge` graph, letting you audit orphaned docs before the restructuring phase runs.
- **Exclusions**: Ensure governance files such as [CONTRIBUTING.md](../CONTRIBUTING.md), [SECURITY.md](../SECURITY.md), [CHANGELOG.md](../CHANGELOG.md), and [LICENSE.txt](../LICENSE.txt) are marked as excluded where appropriate.

## 4. Success Criteria

For v1.0.0, the inventory phase is considered successful when:

- `inventory.json` exists under `reports/doc_refactor/`.
- All Markdown files (excluding intentional exclusions) are present in the `nodes` collection.
- Audience classifications, `version_tag` values, and `is_orphan` flags match repository expectations.
- Summary stats (`role_*`, `audience_*`, `orphans`) align with manual spot checks.

