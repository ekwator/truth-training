# Quickstart — Documentation Refactor v1.0.0

This guide explains how maintainers and Cursor agents can run the documentation refactor workflow for Truth Training v1.0.0.

## 1. Prerequisites

- Python 3.11 installed
- Git clone of the Truth Training repository
- Recommended: virtual environment for Python tooling

From the repository root:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

> Note: The `requirements.txt` already includes dependencies for `scripts/doc_refactor`.

## 2. Build the Documentation Inventory

Generate a complete list of Markdown files and their classification:

```bash
python scripts/doc_refactor/main.py run --phases inventory
```

This phase:
- Walks the repository and finds all `.md` files
- Classifies them as `root`, `docs`, `spec`, or `other`
- Produces a machine-readable report under `reports/doc_refactor/`

## 3. Validate Links and Normalize References

Run link discovery and validation:

```bash
python scripts/doc_refactor/main.py run --phases link_discovery,validation
```

This phase:
- Detects all Markdown links and bare `.md` path mentions
- Normalizes internal `.md` references to `[path](path)` form
- Checks that every internal link has a corresponding file
- Records external URL status as OK or warning

## 4. Restructure Documentation Hierarchy

Apply restructuring rules:

```bash
python scripts/doc_refactor/main.py run --phases restructuring
```

This phase:
- Moves verbose release and deep-dive sections from `[README.md](README.md)` and other top-level files into `docs/`
- Trims `[README.md](README.md)` to a short overview (≤400 words) with links into `docs/` and `spec`
- Ensures `/docs` contains full human-facing detail and `/spec` keeps compressed, AI-oriented content

## 5. Detect Duplicated Blocks Between `/docs` and `/spec`

Run the spec optimization pass:

```bash
python scripts/doc_refactor/main.py run --phases spec_opt
```

This phase:
- Scans for duplicated narrative blocks between `/docs` and `/spec`
- Flags duplicated paragraphs for review, especially when they appear in AI-facing specs
- Produces a report listing `DuplicateBlock` instances

## 6. End-to-End Run

To execute all phases in one go:

```bash
make doc-refactor-run
```

Then run the test suite for the doc-refactor tooling:

```bash
make doc-refactor-test
```

## 7. Expected Outcomes

After a successful run:

- All `.md` mentions are clickable hyperlinks.
- No internal `.md` links are broken.
- `[README.md](README.md)` is short and links to detailed `/docs` and `/spec` sections.
- `/docs` hosts complete human-oriented documentation.
- `/spec` hosts compressed, directive-style content for AI agents.
- Reports under `reports/doc_refactor/` describe inventory, link status, restructuring changes, and duplication findings.


