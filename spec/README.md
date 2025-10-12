# Truth Training – GitHub Spec Kit

Purpose: a single source of truth for requirements, architecture, API, data, and delivery, kept close to code. Use this index to navigate specs and link them in issues/PRs.

## Core Specifications

- **01 Product Vision**: `spec/01-product-vision.md` — FidoNet-inspired decentralized truth verification
- **02 Requirements**: `spec/02-requirements.md` — Functional/Non-functional requirements
- **03 Architecture**: `spec/03-architecture.md` — FidoNet-inspired network model, hub/leaf roles, trust propagation
- **04 Data Model**: `spec/04-data-model.md` — SQLite schema, models, relationships
- **05 HTTP API**: `spec/05-api.md` — REST endpoints, authentication, sync protocols
- **06 Expert System**: `spec/06-expert-system.md` — Heuristics and truth assessment algorithms
- **07 Event Rating Protocol**: `spec/07-event-rating-protocol.md` — Trust weights, reputation system
- **08 P2P & Sync**: `spec/08-p2p-sync.md` — Peer etiquette, store-and-forward, conflict resolution
- **09 UX Guidelines**: `spec/09-ux-guidelines.md` — User interface and experience standards
- **10 CLI Specification**: `spec/10-cli.md` — truthctl commands, configuration, peer management

## Project Management

- **11 Decision Log**: `spec/11-decision-log.md` — Architectural decisions and rationale
- **12 Open Questions**: `spec/12-open-questions.md` — Unresolved issues and risks
- **13 Traceability**: `spec/13-traceability.md` — Requirements to code mapping
- **14 Quality Gates**: `spec/14-quality-gates.md` — CI/Lint/Test standards
- **15 Prompts & Automation**: `spec/15-prompts-and-automation.md` — LLM workflows
- **16 Test Plan**: `spec/16-test-plan.md` — Testing strategy and coverage

## External Tools

- **truthctl CLI**: `docs/CLI_Usage.md` — Administrative CLI for P2P sync and node management (v0.2.1-pre)

## Legacy Documentation

- **Project README**: `README.md` — Main project overview and quick start
- **Architecture**: `docs/architecture.md` — Legacy architecture documentation
- **Technical Spec**: `docs/Technical_Specification.md` — Legacy technical specification
- **Data Schema**: `docs/Data_Schema.md` — Legacy data schema documentation
- **Event Rating Protocol**: `docs/event_rating_protocol.md` — Legacy rating protocol
- **P2P Module**: `docs/p2p_release.md` — Legacy P2P documentation
- **UI Guidelines**: `docs/ui_guidelines.md` — Legacy UI guidelines

## Version History

- **v0.2.8-pre**: Adaptive Propagation Priority (EMA), P2P exchange, API/CLI exposure
- **v0.2.1-pre**: Introduced truthctl CLI, modular architecture, FidoNet-inspired P2P sync
- **v0.2.0**: Stable sync and rating integration, filtered graph API
- **v0.1.x**: Initial implementation and core features
