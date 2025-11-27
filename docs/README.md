# Truth Training Documentation Hub (v1.0.0)

Use this index to navigate every human-oriented document in the repository. Follow the depth = detail rule: the root `README.md` gives narrative context, these `/docs` pages hold implementation guidance, and AI agents should prefer `/spec`.

## How to Read This Tree

- Start here for step-by-step guides, operational playbooks, and release collateral.
- Jump to [spec/README.md](../spec/README.md) when you need compressed, decision-ready specs.
- Historical or niche content lives in the Archive section at the end of this file.

---

## Core System Guides

- [Technical_Specification.md](Technical_Specification.md) — End-to-end description of the Truth Training platform.
- [architecture.md](architecture.md) — System architecture, runtime boundaries, and deployment layouts.
- [Data_Schema.md](Data_Schema.md) — Database schema, migrations, and entity contracts.
- [CLI_Usage.md](CLI_Usage.md) — truthctl reference, commands, and workflow examples.
- [Concept_Collective_Intelligence.md](Concept_Collective_Intelligence.md) — Conceptual model for crowd-sourced truth signals.
- [event_rating_protocol.md](event_rating_protocol.md) — Calculation rules for scoring events and propagating trust.
- [SPEC_SUMMARY.md](SPEC_SUMMARY.md) — Human-readable digest of the AI-oriented `/spec` directory.

## Platform & Integration Guides

- [UI_Desktop.md](UI_Desktop.md) — Desktop UX flows, navigation, and component rules.
- [ui_guidelines.md](ui_guidelines.md) — Consolidated UI/UX guidance across CLI, Desktop, and Android.
- [ANDROID_MIGRATION.md](ANDROID_MIGRATION.md) — Migration plan for Android client parity with desktop.
- [ANDROID_TEST_FIX_SUGGESTIONS.md](ANDROID_TEST_FIX_SUGGESTIONS.md) — Known Android regressions plus remediation tactics.
- [android_discovery_architecture.md](android_discovery_architecture.md) — Discovery subsystem internals on Android.
- [cross_platform_discovery_compatibility.md](cross_platform_discovery_compatibility.md) — Format review that keeps discovery metadata aligned between platforms.
- [p2p_release.md](p2p_release.md) — Operational checklist for synchronizing P2P releases across devices.

## Operations, Deployment & Releases

- [Deployment.md](Deployment.md) — Environment preparation, secrets, and rollout flows.
- [build_instructions.md](build_instructions.md) — Build recipes for CLI, server, and UI targets.
- [Install_Paths_By_OS.md](Install_Paths_By_OS.md) — Default config/database paths for each OS.
- [VERSION_REGISTRY.md](VERSION_REGISTRY.md) — Version compatibility grid and upgrade notes.
- [CI_Workflows_Artifacts.md](CI_Workflows_Artifacts.md) — CI pipelines plus produced artifacts.
- [RELEASE_PREPARATION_SUMMARY.md](RELEASE_PREPARATION_SUMMARY.md) — End-to-end release readiness checklist.
- [RELEASE_v1.0.0_DRAFT.md](RELEASE_v1.0.0_DRAFT.md) — Narrative release notes for v1.0.0.
- [release-notes-v1.0.0-Baseline.md](release-notes-v1.0.0-Baseline.md) — Baseline feature set for the first stable release.
- [release-notes-v1.0.0-Release.md](release-notes-v1.0.0-Release.md) — Final release announcement collateral.
- [Constitution-Compliance.md](Constitution-Compliance.md) — Documentation of compliance checks and procedures.
- [post_integration_hardening.md](post_integration_hardening.md) — Hardening steps after major merges.

## Quality, Testing & Troubleshooting

- [cross_device_e2e_tests.md](cross_device_e2e_tests.md) — Cross-device scenario coverage and status.
- [device_e2e_test_report.md](device_e2e_test_report.md) — Device-by-device execution log.
- [TEST_REPORT_ANDROID_v1.0.0.md](TEST_REPORT_ANDROID_v1.0.0.md) — Android verification package for the release.
- [node_discovery_test_results.md](node_discovery_test_results.md) — Discovery test outcomes and telemetry.
- [final_validation.md](final_validation.md) — Pre-release validation gates and evidence.
- [troubleshooting.md](troubleshooting.md) — Known issues, mitigations, and escalation matrix.

## Additional References

- [documentation.md](documentation.md) — Compatibility alias that now forwards readers back to this index.
- [Truth-training/Truth-training.md](Truth-training/Truth-training.md) — Historical whitepaper for the platform.
- [api_reference/API_REFERENCE.md](api_reference/API_REFERENCE.md) — REST/FFI API reference docs.

## Archive & Research

These files remain available for audit trails, historical notes, or specification exercises:

- [DESIGN_INDEX.md](DESIGN_INDEX.md)
- [prompt/ekwator/specify1/plan/plan1.md](prompt/ekwator/specify1/plan/plan1.md)
- [prompt/ekwator/specify1/spec1.md](prompt/ekwator/specify1/spec1.md)
- [truth-android-client/CHANGELOG.md](../truth-android-client/CHANGELOG.md)
- [truth-android-client/docs/INTEGRATION_TRUTH_CORE.md](../truth-android-client/docs/INTEGRATION_TRUTH_CORE.md)
- [truth-android-client/truthcore_api/api_reference_link.md](../truth-android-client/truthcore_api/api_reference_link.md)
- [ui/desktop/CHANGELOG.md](../ui/desktop/CHANGELOG.md)
- [Legacy specs/010 research (GitHub)](https://github.com/ekwator/truth-training/blob/main/specs/010-documentation-refactoring-0-0/research.md) and companion planning files for the refactor itself (archived).

---

_Version: v1.0.0 — Maintainers update this hub whenever a new human-facing document is added or retired._
