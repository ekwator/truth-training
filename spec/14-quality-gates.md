# Quality Gates
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 14

## Quality Gates

- Lint: cargo fmt + clippy (workspace-wide).
- Build: cargo check (requires Rust with edition2024 support or downgrade to 2021).
- Test: unit tests for API signature verification, storage schema init, expert heuristic cases.
- Docs: Spec Kit must be referenced in README and kept up to date in PRs.

Note: Current environment cargo 1.82.0 lacks edition2024; CI should use nightly or set edition=2021 temporarily.
