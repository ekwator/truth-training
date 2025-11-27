# Quickstart: Documentation Refactor Operator Guide

**Audience**: Release steward / documentation maintainer  
**Prereqs**: Python 3.11, virtualenv, repo checkout, clean working tree snapshot

## Setup
1. `cd /home/ekwator/Code/truth-training`
2. `python3 -m venv .venv && source .venv/bin/activate`
3. `pip install -r requirements.txt`
4. (Optional) `export DOC_REFACTOR_REPORT_DIR=reports/doc_refactor/$(date +%Y%m%d-%H%M%S)`

## Scenario 1 – Generate inventory & depth report
1. `python scripts/doc_refactor/main.py run --phases inventory,link_discovery`
2. Inspect `reports/doc_refactor/inventory.json` for exclusion list and depth.  
3. Fail the run if README depth != 0 or excluded files appear.

## Scenario 2 – Convert plaintext paths & validate links
1. `python scripts/doc_refactor/main.py run --phases validation`
2. Confirm `reports/doc_refactor/validation.json.plain_paths == []`.  
3. For any broken external URL, either replace with local reference or add `<!-- verified: reachable -->` comment per rules.

## Scenario 3 – README compression & relocation
1. `python scripts/doc_refactor/main.py run --phases restructuring`
2. Ensure `README.md` word count is reported between 500 and 700 words.  
3. Spot-check that detailed sections now live under `/docs/...` and README keeps summaries/links only.

## Scenario 4 – Orphan remediation & archival
1. `python scripts/doc_refactor/main.py run --phases file_creation`
2. Review `reports/doc_refactor/file_creation.json` for relocated `docs/archive/` entries.  
3. Confirm `reports/doc_refactor/link_graph.json.orphans == []` (non-archived set).

## Scenario 5 – Spec compression enforcement
1. `python scripts/doc_refactor/main.py run --phases spec_opt`
2. Validate each `/spec/*.md` now contains: Goals, Constraints, Success Criteria, "Use /spec before /docs" directive, and sub-80-word paragraphs.  
3. Inspect `spec/README.md` for the AI guidance block.

## Scenario 6 – Full pipeline & final report
1. `python scripts/doc_refactor/main.py run --phases all`
2. Verify `reports/doc_refactor/run_summary.json` declares: zero broken links, zero plaintext `.md`, 100% coverage, README compliance, dedupe actions applied, spec compression pass.  
3. Push branch + open PR only after diff shows `/docs` contains detail, `/spec` is compressed, and `docs/archive/` holds historical material.

## Rollback / Safety
- CLI never deletes files permanently; archived files move under `docs/archive/`.  
- Always commit before rerunning phases that move content.  
- To abort, restore from git and rerun `inventory` for a clean ledger.

