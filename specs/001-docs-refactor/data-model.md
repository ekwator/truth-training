# Documentation Refactor v1.0.0 — Data Model

## 1. Documentation Entities

### 1.1 DocumentationNode

- **Description**: Logical representation of a single Markdown file in the repository.
- **Attributes**:
  - `path`: repository-relative path (e.g., `[docs/README.md](docs/README.md)`)
  - `audience`: one of `root`, `docs`, `spec`, `other`
  - `version_tag`: textual indicator of v1.0.0 applicability or legacy status
  - `linked_from`: list of paths that reference this node
  - `links_to`: list of outgoing links (paths or URLs)
  - `is_orphan`: boolean flag if `linked_from` is empty after traversal
  - `is_excluded`: boolean for files like `[CONTRIBUTING.md](CONTRIBUTING.md)`, `[SECURITY.md](SECURITY.md)`, `[CHANGELOG.md](CHANGELOG.md)`, `LICENSE.txt` (content refactor excluded)

### 1.2 ReferenceEdge

- **Description**: Directed link from one documentation node to another path or URL.
- **Attributes**:
  - `source_path`: origin Markdown file
  - `target`: either a repo-relative `.md` path or external URL
  - `link_text`: visible label in the Markdown
  - `status`: one of `ok`, `missing`, `external_ok`, `external_warning`
  - `normalized`: boolean indicating whether it has been converted to `[text](target)` form

### 1.3 InventoryReport

- **Description**: Aggregated view of all discovered documentation nodes and references.
- **Attributes**:
  - `nodes`: collection of `DocumentationNode`
  - `edges`: collection of `ReferenceEdge`
  - `summary`: counts (total docs, by audience, orphan count, broken link count)
  - `generated_at`: timestamp

### 1.4 IndexGateway

- **Description**: Entry-point docs that must provide coverage for all non-archived docs.
- **Attributes**:
  - `path`: one of `[README.md](README.md)`, `[docs/README.md](docs/README.md)`, `[spec/README.md](spec/README.md)`
  - `sections`: logical sections that group links (e.g., “Releases”, “Architecture”)
  - `coverage`: list of `DocumentationNode.path` entries reachable from this gateway

### 1.5 DuplicateBlock

- **Description**: A block of text found in more than one Markdown file after normalization.
- **Attributes**:
  - `content_hash`: hash of normalized block contents
  - `occurrences`: list of `(path, line_range)` locations
  - `classification`: one of `harmless`, `needs_review`

## 2. Validation Rules

1. Each `DocumentationNode.path` must be unique.
2. Any `ReferenceEdge` with a `.md` target inside the repo must resolve to a `DocumentationNode`.
3. `DocumentationNode.is_orphan` is `false` for all non-archived docs; true only when intentionally archived and linked from an “Archived” section.
4. For `DocumentationNode.audience = "spec"`, long narrative paragraphs should not appear; associated `DuplicateBlock` instances crossing `/docs` and `/spec` are flagged as `needs_review`.
5. Index gateways must collectively provide coverage of all non-archived nodes.


