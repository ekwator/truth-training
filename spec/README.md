# Truth Training – Specification Index v1.0.0

> **For AI Agents**: This `/spec` directory is the **primary decision source** for Truth Training. Consult `/spec` first for all architectural, API, and implementation decisions. Use `/docs` for narrative context, examples, and migration guides only after understanding the structural truth defined here.

## AI Agent Usage Guidelines

**Decision-Making Workflow:**
1. **Start with `/spec`**: All structural truth, API contracts, data models, and requirements are defined here
2. **Reference `/docs` for context**: Use `/docs` for narrative explanations, migration stories, and operational guides
3. **Follow traceability**: Use [`spec/13-traceability.md`](13-traceability.md) to understand feature relationships

**Key Principles:**
- `/spec` contains **compressed, decision-ready** specifications
- `/docs` contains **narrative, human-readable** documentation
- When in doubt, `/spec` is the canonical source for system behavior

---

## Version Information

| Component       | Current Version |
|-----------------|-----------------|
| Core Library    | v1.0.0          |
| Server          | v1.0.0          |
| CLI (truthctl)  | v1.0.0          |
| Desktop UI      | v1.0.0          |
| Spec Document   | v1.0.0          |

> The Spec Document version refers only to documentation and does not determine software versions.  
> For canonical version mapping, see [`docs/VERSION_REGISTRY.md`](../docs/VERSION_REGISTRY.md).

---

## Core Specifications

### Foundation
- [`01-product-vision.md`](01-product-vision.md) — Product vision and core principles
- [`02-requirements.md`](02-requirements.md) — Functional and non-functional requirements
- [`03-architecture.md`](03-architecture.md) — Architecture overview (network roles, FIDONet-inspired rules)
- [`04-data-model.md`](04-data-model.md) — Data model and schema

### APIs & Protocols
- [`05-api.md`](05-api.md) — HTTP API specification (canonical source)
- [`07-event-rating-protocol.md`](07-event-rating-protocol.md) — Event rating protocol
- [`08-p2p-sync.md`](08-p2p-sync.md) — P2P synchronization protocol

### Implementation
- [`10-cli.md`](10-cli.md) — CLI specification (`truthctl` commands and config)
- [`06-expert-system.md`](06-expert-system.md) — Expert system heuristics
- [`09-ux-guidelines.md`](09-ux-guidelines.md) — UX guidelines

### Cross-Platform
- [`18-cross-platform-architecture.md`](18-cross-platform-architecture.md) — Cross-platform architecture specification
- [`19-build-instructions.md`](19-build-instructions.md) — Cross-platform build instructions
- [`20-cargo-configuration.md`](20-cargo-configuration.md) — Cargo.toml configuration

### Quality & Governance
- [`13-traceability.md`](13-traceability.md) — Traceability matrix (feature relationships)
- [`14-quality-gates.md`](14-quality-gates.md) — Quality gates and standards
- [`16-test-plan.md`](16-test-plan.md) — Test plan and validation criteria
- [`17-offline-reliability.md`](17-offline-reliability.md) — Offline reliability and data integrity

### Process & Planning
- [`11-decision-log.md`](11-decision-log.md) — Decision log (ADR)
- [`12-open-questions.md`](12-open-questions.md) — Open questions and risks
- [`15-prompts-and-automation.md`](15-prompts-and-automation.md) — Prompts and automation
- [`21-roadmap.md`](21-roadmap.md) — Roadmap (high-level)

---

## Complete Specification Index

| ID | File | Title |
|----|------|-------|
| 01 | [01-product-vision.md](01-product-vision.md) | Product Vision |
| 02 | [02-requirements.md](02-requirements.md) | Requirements |
| 03 | [03-architecture.md](03-architecture.md) | Architecture Overview |
| 04 | [04-data-model.md](04-data-model.md) | Data Model |
| 05 | [05-api.md](05-api.md) | HTTP API (current implementation) |
| 06 | [06-expert-system.md](06-expert-system.md) | Expert System (Heuristics) |
| 07 | [07-event-rating-protocol.md](07-event-rating-protocol.md) | Event Rating Protocol |
| 08 | [08-p2p-sync.md](08-p2p-sync.md) | P2P & Sync |
| 09 | [09-ux-guidelines.md](09-ux-guidelines.md) | UX Guidelines |
| 10 | [10-cli.md](10-cli.md) | CLI Specification (truthctl) |
| 11 | [11-decision-log.md](11-decision-log.md) | Decision Log (ADR) |
| 12 | [12-open-questions.md](12-open-questions.md) | Open Questions & Risks |
| 13 | [13-traceability.md](13-traceability.md) | Traceability Matrix |
| 14 | [14-quality-gates.md](14-quality-gates.md) | Quality Gates |
| 15 | [15-prompts-and-automation.md](15-prompts-and-automation.md) | Prompts & Automation |
| 16 | [16-test-plan.md](16-test-plan.md) | Test Plan |
| 17 | [17-offline-reliability.md](17-offline-reliability.md) | Offline Reliability and Data Integrity |
| 18 | [18-cross-platform-architecture.md](18-cross-platform-architecture.md) | Cross-Platform Architecture Specification |
| 19 | [19-build-instructions.md](19-build-instructions.md) | Cross-Platform Build Instructions |
| 20 | [20-cargo-configuration.md](20-cargo-configuration.md) | Cross-Platform Cargo.toml Configuration |
| 21 | [21-roadmap.md](21-roadmap.md) | Roadmap (High-level) |

---

## Related Documentation

### Human-Readable Guides (in `/docs`)
- [API Reference](../docs/api_reference/API_REFERENCE.md) — Human-readable API guide
- [CLI Usage](../docs/CLI_Usage.md) — CLI user guide
- [Architecture Guide](../docs/architecture.md) — Architecture narrative
- [Technical Specification](../docs/Technical_Specification.md) — Detailed technical documentation
- [Design Index](../docs/DESIGN_INDEX.md) — Cross-reference for all design documents

### Platform-Specific Documentation
- [Android Migration](../docs/ANDROID_MIGRATION.md) — Android migration guide
- [Android Test Report](../docs/TEST_REPORT_ANDROID_v1.0.0.md) — Android test report
- [Cross-Platform Comparison](../docs/Truth-training/Truth-training.md) — Android vs Desktop comparison

### Integration Guides
- [Android Integration](../integration/android/README_INTEGRATION.md) — JNI setup and JSON verification
- [iOS Integration](../integration/ios/README_INTEGRATION.md) — Swift bindings and FFI setup
- [Desktop Integration](../integration/desktop/README_INTEGRATION.md) — HTTP API and CLI usage

---

## Quick Reference

**For Build Instructions**: See [`19-build-instructions.md`](19-build-instructions.md)  
**For API Contracts**: See [`05-api.md`](05-api.md) (canonical) or [`docs/api_reference/API_REFERENCE.md`](../docs/api_reference/API_REFERENCE.md) (human-readable)  
**For Data Model**: See [`04-data-model.md`](04-data-model.md)  
**For CLI Commands**: See [`10-cli.md`](10-cli.md)  
**For Version Mapping**: See [`docs/VERSION_REGISTRY.md`](../docs/VERSION_REGISTRY.md)

---

*This spec index is maintained as part of the Truth Training v1.0.0 documentation baseline. All specifications reflect the v1.0.0 system state.*
