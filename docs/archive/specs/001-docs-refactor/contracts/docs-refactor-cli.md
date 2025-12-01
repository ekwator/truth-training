<!-- Archived from [specs/001-docs-refactor/contracts/docs-refactor-cli.md](specs/001-docs-refactor/contracts/docs-refactor-cli.md) -->

# Docs Refactor CLI & Reports — Contract

## 1. Inventory Phase

### Command

```bash
python scripts/doc_refactor/main.py run --phases inventory
```

### Expected Behavior

- Scans the entire repository for `.md` files.
- Classifies each file as `root`, `docs`, `spec`, or `other`.
- Produces a machine-readable report in `reports/doc_refactor/inventory.json` (or equivalent).

### Report Shape (Conceptual)

```json
{
  "generated_at": "ISO-8601 timestamp",
  "nodes": [
    {
      "path": "[docs/README.md](docs/README.md)",
      "audience": "docs",
      "version_tag": "v1.0.0",
      "is_excluded": false
    }
  ]
}
```

## 2. Link Discovery & Validation

### Command

```bash
python scripts/doc_refactor/main.py run --phases link_discovery,validation
```

### Expected Behavior

- Discovers Markdown links and bare `.md` paths.
- Normalizes internal paths to `[text](path)` form.
- Checks existence of each internal `.md` target.
- Records external URL status (OK/warning).

### Report Shape (Conceptual)

```json
{
  "edges": [
    {
      "source_path": "[README.md](README.md)",
      "target": "[docs/README.md](docs/README.md)",
      "status": "ok",
      "normalized": true
    }
  ],
  "broken_links": [],
  "external_warnings": []
}
```

## 3. Restructuring & Spec Optimization

### Command

```bash
python scripts/doc_refactor/main.py run --phases restructuring,spec_opt
```

### Expected Behavior

- Moves detailed sections from top-level files into `docs/`.
- Ensures `[README.md](README.md)` is short and links into `/docs` and `/spec`.
- Flags duplicated blocks between `/docs` and `/spec` for review.

### Report Shape (Conceptual)

```json
{
  "restructured_files": [
    "[README.md](README.md)",
    "[docs/README.md](docs/README.md)"
  ],
  "duplicate_blocks": [
    {
      "content_hash": "sha256...",
      "occurrences": [
        {
          "path": "[docs/README.md](docs/README.md)",
          "lines": [10, 20]
        },
        {
          "path": "[spec/README.md](spec/README.md)",
          "lines": [5, 15]
        }
      ]
    }
  ]
}
```



