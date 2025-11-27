# Contract: doc_refactor CLI

**Command**: `python scripts/doc_refactor/main.py run [options]`

## Inputs
- Repository root with Markdown tree (`docs/`, `spec/`, `specs/`)
- Python 3.11 environment with dependencies from `requirements.txt`
- Optional env vars:
  - `DOC_REFACTOR_REPORT_DIR` – override report output directory
  - `DOC_REFACTOR_EXCLUDE` – comma-separated paths to skip (defaults already include CONTRIBUTING/Licenses/Security/Changelog)

## Subcommands & Options
| Flag | Description |
|------|-------------|
| `--phases <list>` | Comma-separated phases (`inventory`, `link_discovery`, `validation`, `file_creation`, `version_sync`, `restructuring`, `dedupe`, `spec_opt`, `all`). |
| `--resume` | Restart from last successful phase using `RunArtifact`. |
| `--dry-run` | Execute without writing files (reports still generated). |
| `--fail-fast` | Abort on first validation failure instead of continuing. |
| `--report-dir PATH` | Explicit report directory (overrides env var). |

## Expected Outputs
- Reports under `reports/doc_refactor/<phase>/...` (JSON, CSV, Markdown summary).
- Modified files inside `/docs`, `/spec`, `/README.md`, `/docs/archive`.  
- `reports/doc_refactor/run_summary.json` containing completion stats.

## Exit Codes
| Code | Meaning | Operator Action |
|------|---------|-----------------|
| 0 | Success (all phases pass validators) | Review diff, run tests, open PR |
| 1 | Recoverable validation failure (e.g., broken external link, missing spec compression) | Inspect validation report, fix issue, rerun starting at failed phase |
| 2 | CLI misuse or missing dependencies | Fix environment, rerun |
| 3+ | Unexpected error (trace logged) | File bug with `reports/doc_refactor/*.log` attached |

## Phase Contract Summary
| Phase | Preconditions | Postconditions |
|-------|---------------|----------------|
| `inventory` | Repo accessible; exclusion list loaded | `inventory.json` written; `DocumentationFile` list complete |
| `link_discovery` | Inventory complete | `link_graph.json` produced with edges + orphan list |
| `validation` | Link graph available | Plain paths converted, broken links list emitted |
| `file_creation` | Validation run | Missing doc/spec files created from templates, ledger logged |
| `version_sync` | File creation done | Every active Markdown file declares `v1.0.0` |
| `restructuring` | Version sync done | README compressed, detail files relocated, archive moves recorded |
| `dedupe` | Restructure done | Duplicate clusters resolved or flagged |
| `spec_opt` | Dedupe done | `/spec` compressed, directives injected |

## Logging & Observability
- Progress logged per phase with timestamps + memory usage snapshot.  
- Validation summaries printed to stdout and saved to `run_summary.json`.  
- Each phase writes `RunArtifact` (phase, start/end, status) enabling resume.

## Safety Guarantees
- No destructive deletes; archival moves instead of rm.  
- Dry-run mode available for inspection.  
- Scripts enforce git-clean check unless `--allow-dirty` (future option) is set.

