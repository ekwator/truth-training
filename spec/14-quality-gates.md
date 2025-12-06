# Quality Gates

Use /spec as the primary decision source before reading /docs.
Version: v1.0.0
Updated: 2025-01-XX
Spec ID: 14

## Quality Gates

### Code Quality
- **Lint**: `cargo fmt --all` + `cargo clippy --all-targets --all-features -- -D warnings` (workspace-wide, zero warnings required)
- **Build**: `cargo check` (Rust edition 2021)
- **Type Checking**: `pnpm typecheck` (Desktop UI), `ktlint` + `detekt` (Android)
- **Security Audit**: `cargo audit` (no vulnerabilities allowed)

### Testing
- **Unit Tests**: API signature verification, storage schema init, expert heuristic cases, component rendering
- **Integration Tests**: REST API endpoints, context engine, event lifecycle, cross-platform compatibility
- **E2E Tests**: Desktop Playwright tests, Android instrumentation tests (physical device required)
- **Test Coverage**: Rust Core ≥90%, Desktop UI ≥85%, Android ≥70%, Server API ≥90%
- **No Skipped Tests**: All `#[ignore]`, `it.skip`, `@Ignore` must be justified

### Documentation
- **Spec-Kit**: Must be referenced in README and kept up to date in PRs. All PRs must include `/speckit.specify`, `/speckit.plan`, `/speckit.task` or `/speckit.implementation` chain
- **Doc Refactor**: `make doc-refactor-run` must succeed and attach `inventory.json`, `link_report.json`, and `validation.json` artifacts to the PR/CI run. No `ReferenceEdge` may remain in `status: "missing"`

### CI Requirements
- **Cross-Platform Build**: Linux, Windows, macOS, Android, Desktop UI
- **All Tests Pass**: Rust, Desktop, Android (both CI emulator and physical device)
- **Static Analysis**: Clean (no warnings, no errors)
- **Performance**: No regressions (Desktop navigation <200ms, pagination <100ms, Android cold start <1.3s)

### PR Acceptance
- **Spec-Kit Chain**: Required for all PRs (PRs without Spec-Kit workflow will be closed)
- **Review Approvals**: Minimum 2 maintainers (3 for security-sensitive changes)
- **No TODOs**: All TODOs must be resolved before merge
- **Documentation**: Updated when needed, CHANGELOG.md updated

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed requirements.

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.
