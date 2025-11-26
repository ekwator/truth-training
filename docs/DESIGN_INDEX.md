# Design Documentation Index

This index keeps cross-references for every design-focused document under `docs/` and `specs/`, ensuring no file is orphaned or "stuck" without inbound links.

## 📄 Architecture & Technical Documentation

### Core Architecture
- [`docs/architecture.md`](architecture.md) — System architecture overview
- [`docs/Technical_Specification.md`](Technical_Specification.md) — Detailed technical specification
- [`docs/Data_Schema.md`](Data_Schema.md) — Database schema documentation
- [`docs/event_rating_protocol.md`](event_rating_protocol.md) — Event rating protocol details
- [`docs/Concept_Collective_Intelligence.md`](Concept_Collective_Intelligence.md) — Collective intelligence concepts

### Platform Architecture
- [`docs/android_discovery_architecture.md`](android_discovery_architecture.md) — Android discovery architecture
- [`docs/cross_platform_discovery_compatibility.md`](cross_platform_discovery_compatibility.md) — Cross-platform discovery compatibility
- [`docs/UI_Desktop.md`](UI_Desktop.md) — Desktop UI documentation
- [`docs/ui_guidelines.md`](ui_guidelines.md) — UI design guidelines

### Network & P2P
- [`docs/p2p_release.md`](p2p_release.md) — P2P release documentation
- [`docs/node_discovery_test_results.md`](node_discovery_test_results.md) — Node discovery test results

## 📱 Platform-Specific Documentation

### Android
- [`docs/ANDROID_MIGRATION.md`](ANDROID_MIGRATION.md) — Android migration guide (v0.3.0 → v1.0.0)
- [`docs/TEST_REPORT_ANDROID_v1.0.0.md`](TEST_REPORT_ANDROID_v1.0.0.md) — Android test report v1.0.0
- [`docs/ANDROID_TEST_FIX_SUGGESTIONS.md`](ANDROID_TEST_FIX_SUGGESTIONS.md) — Android test fix suggestions
- [`docs/Truth-training/Truth-training.md`](Truth-training/Truth-training.md) — Android vs Desktop comparison
- [`truth-android-client/docs/INTEGRATION_TRUTH_CORE.md`](../truth-android-client/docs/INTEGRATION_TRUTH_CORE.md) — Truth Core integration guide

### Desktop
- [`docs/UI_Desktop.md`](UI_Desktop.md) — Desktop UI documentation

## 🧪 Testing & Validation

- [`docs/TEST_REPORT_ANDROID_v1.0.0.md`](TEST_REPORT_ANDROID_v1.0.0.md) — Android test report
- [`docs/cross_device_e2e_tests.md`](cross_device_e2e_tests.md) — Cross-device end-to-end test scenarios
- [`docs/device_e2e_test_report.md`](device_e2e_test_report.md) — Device-based E2E test report
- [`docs/final_validation.md`](final_validation.md) — Final validation report
- [`docs/post_integration_hardening.md`](post_integration_hardening.md) — Post-integration hardening

## 📦 Deployment & Operations

- [`docs/Deployment.md`](Deployment.md) — Deployment guide
- [`docs/troubleshooting.md`](troubleshooting.md) — Troubleshooting playbook
- [`docs/Install_Paths_By_OS.md`](Install_Paths_By_OS.md) — Installation paths by operating system
- [`docs/CI_Workflows_Artifacts.md`](CI_Workflows_Artifacts.md) — CI/CD workflows and artifacts
- [`docs/build_instructions.md`](build_instructions.md) — Build instructions

## 📚 API & CLI Documentation

- [`docs/api_reference/API_REFERENCE.md`](api_reference/API_REFERENCE.md) — Complete API reference
- [`docs/CLI_Usage.md`](CLI_Usage.md) — CLI usage guide

## 🔄 Release & Version Documentation

- [`docs/RELEASE_v1.0.0_DRAFT.md`](RELEASE_v1.0.0_DRAFT.md) — Release notes for v1.0.0
- [`docs/VERSION_REGISTRY.md`](VERSION_REGISTRY.md) — Version registry (canonical version map)

## ⚖️ Compliance & Governance

- [`docs/Constitution-Compliance.md`](Constitution-Compliance.md) — Constitution compliance documentation

## 🧩 Spec Kits (`specs/` tree)

### 001 – Documentation Refactoring v1.0.0
- [`specs/001-documentation-refactoring-0-0/spec.md`](../specs/001-documentation-refactoring-0-0/spec.md) — Feature specification
- [`specs/001-documentation-refactoring-0-0/plan.md`](../specs/001-documentation-refactoring-0-0/plan.md) — Implementation plan
- [`specs/001-documentation-refactoring-0-0/research.md`](../specs/001-documentation-refactoring-0-0/research.md) — Research findings
- [`specs/001-documentation-refactoring-0-0/data-model.md`](../specs/001-documentation-refactoring-0-0/data-model.md) — Data model
- [`specs/001-documentation-refactoring-0-0/contracts/STRUCTURE.md`](../specs/001-documentation-refactoring-0-0/contracts/STRUCTURE.md) — Structural contracts
- [`specs/001-documentation-refactoring-0-0/quickstart.md`](../specs/001-documentation-refactoring-0-0/quickstart.md) — Quickstart validation guide
- [`specs/001-documentation-refactoring-0-0/tasks.md`](../specs/001-documentation-refactoring-0-0/tasks.md) — Task list

---

## How to Use This Index

1. **For Design Reviews**: Use this index to find all design-related documentation
2. **For Cross-References**: When adding new design docs, update this index
3. **For Discovery**: All design docs should be reachable from this index within two clicks

## Maintenance

If you add or rename a design document, update this index so the file remains discoverable. Design documents include:
- Architecture and technical specifications
- Platform-specific guides and migration docs
- Test reports and validation documents
- Deployment and operational guides
- API and CLI documentation
- Release notes and version information
- Spec kits under `specs/`
