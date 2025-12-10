# Implementation Plan: Desktop Impacts, Judgments, and Network Nodes UI

**Branch**: `020-desktop-impacts-judgments-nodes` | **Date**: 2025-01-XX | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/020-desktop-impacts-judgments-nodes/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Реализация функциональности добавления Impacts, отправки Judgments и просмотра деталей Network Nodes для Desktop UI, используя структуру экранов Desktop (React/TypeScript с Tauri). Функциональность должна соответствовать Android реализации, но адаптирована под Desktop UI структуру и компоненты.

**Primary Requirement**: Реализовать три основные функции:
1. Добавление Impacts к событиям с уровнем 1-5 и опциональными заметками
2. Отправка Judgments с оценкой (true/false/uncertain), уверенностью (0.0-1.0) и опциональным обоснованием
3. Просмотр детальной информации о Network Nodes с отображением типов Hub/Leaf

**Technical Approach**: Использовать существующие компоненты Desktop UI (EventSummary, NodesPanel), добавить модальные окна для форм, реализовать утилиты для маппинга (ImpactLevelMapper, NodeTypeMapper), интегрировать с ApiService для offline-first стратегии.

## Technical Context

**Language/Version**: TypeScript 5.2+, React 18.2+, Rust 1.75+ (Tauri backend)  
**Primary Dependencies**: 
- Frontend: React, TypeScript, Tailwind CSS, Headless UI, Zustand
- Backend: Tauri 2.0, Rust, core_lib, truth_core
- Testing: Jest, React Testing Library, Playwright  
**Storage**: SQLite через Tauri backend (core_lib storage)  
**Testing**: Jest для unit тестов, React Testing Library для component тестов, Playwright для E2E тестов  
**Target Platform**: Linux/Windows/macOS Desktop (Tauri application)  
**Project Type**: Desktop application (Tauri + React)  
**Performance Goals**: 
- Form submission < 200ms (local)
- List updates < 100ms (reactive)
- Modal open/close < 50ms
- 60fps UI interactions  
**Constraints**: 
- Offline-first strategy (local storage with sync)
- Bilingual localization (EN/RU)
- Emoji support in all UI elements (Rule 8)
- Desktop UI parity with Android implementation
- Must work without network connection  
**Scale/Scope**: 
- 3 new UI components (AddImpactModal, SubmitJudgmentModal, NodeDetailView)
- 2 utility functions (ImpactLevelMapper, NodeTypeMapper)
- Updates to 2 existing screens (EventSummary, NodesPanel)
- ~500-800 lines of new code
- Full test coverage required

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Rule 1 — Cross-Platform Scope & Parity
✅ **PASS**: Desktop UI implementation will match Android implementation patterns for impacts, judgments, and node details, ensuring feature parity across platforms.

### Rule 2 — Source Documents as Authority
✅ **PASS**: Implementation follows quickstart_desktop.md requirements and matches Android implementation documented in specs/018 and specs/019.

### Rule 3 — Releases, Installation & Automation
✅ **PASS**: Desktop UI is part of existing release process. No changes to release automation required.

### Rule 4 — Dependency, Vulnerability & Platform Safeguards
✅ **PASS**: Using existing dependencies (React, Tauri, etc.). No new dependencies required.

### Rule 5 — Database & Schema Integrity
✅ **PASS**: Using existing database schema. No schema changes required. Impacts and judgments use existing tables.

### Rule 6 — CI, Tooling & Automation Discipline
✅ **PASS**: Existing CI/CD workflows will cover new code. Tests required for all new components.

### Rule 7 — Documentation Discipline
✅ **PASS**: This plan and spec.md document the feature. Quickstart documentation will be updated.

### Rule 8 — UI Desktop Emoji Accessibility Requirement
✅ **PASS**: All new UI elements MUST include emojis using existing `@/utils/emojiMapping` utility, matching Android implementation and Desktop UI patterns.

### Rule 9 — Bilingual Localization
✅ **PASS**: All text labels MUST be localized using Desktop i18n system (English/Russian), matching Android string resources approach.

## Project Structure

### Documentation (this feature)

```text
specs/020-desktop-impacts-judgments-nodes/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
ui/desktop/
├── src/
│   ├── components/
│   │   ├── impacts/
│   │   │   └── AddImpactModal.tsx          # NEW: Modal for adding impacts
│   │   ├── judgments/
│   │   │   └── SubmitJudgmentModal.tsx     # NEW: Modal for submitting judgments
│   │   ├── nodes/
│   │   │   └── NodeDetailView.tsx          # NEW: Detail view for network nodes
│   │   └── NodesPanel.tsx                  # MODIFY: Add click handler for node details
│   ├── pages/
│   │   └── EventSummary.tsx                # MODIFY: Add impacts/judgments sections and buttons
│   ├── services/
│   │   └── api.ts                          # MODIFY: Add methods for impacts/judgments if needed
│   ├── utils/
│   │   ├── impactLevelMapper.ts            # NEW: Map impact levels 1-5 to boolean
│   │   └── nodeTypeMapper.ts               # NEW: Map technical types to Hub/Leaf
│   ├── types/
│   │   ├── impacts.ts                     # NEW: Impact type definitions
│   │   └── nodes.ts                       # MODIFY: Add node detail types if needed
│   └── stores/
│       └── [existing stores]               # MODIFY: Add state management if needed
│
└── src-tauri/
    └── src/
        └── commands/
            └── [existing commands]         # MODIFY: Add Tauri commands if needed

tests/
├── unit/
│   ├── impactLevelMapper.test.ts           # NEW: Unit tests for ImpactLevelMapper
│   └── nodeTypeMapper.test.ts              # NEW: Unit tests for NodeTypeMapper
├── component/
│   ├── AddImpactModal.test.tsx             # NEW: Component tests
│   ├── SubmitJudgmentModal.test.tsx        # NEW: Component tests
│   └── NodeDetailView.test.tsx             # NEW: Component tests
└── integration/
    └── impacts-judgments-nodes.test.ts     # NEW: Integration tests
```

**Structure Decision**: Desktop UI follows existing React/TypeScript structure with component-based architecture. New components will be added to appropriate directories (`components/impacts`, `components/judgments`, `components/nodes`). Utilities will be added to `utils/` directory. Existing screens (EventSummary, NodesPanel) will be modified to integrate new functionality.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations detected. Implementation follows existing Desktop UI patterns and structure.

## Implementation Phases

### Phase 0: Research
- Analyze Android implementation (specs/018, specs/019)
- Review existing Desktop UI components (EventSummary, NodesPanel)
- Review ApiService methods for impacts/judgments/nodes
- Review emojiMapping utility usage
- Review i18n localization system
- Document technical decisions and constraints

### Phase 1: Design
- Design data model for impacts, judgments, nodes
- Design API contracts (Tauri commands)
- Design component interfaces and props
- Design utility function interfaces
- Create quickstart scenarios
- Document contracts

### Phase 2: Tasks
- Break down implementation into tasks
- Define task dependencies
- Assign priorities
- Create tasks.md

### Phase 3: Implementation
- Create utility functions (ImpactLevelMapper, NodeTypeMapper)
- Create modal components (AddImpactModal, SubmitJudgmentModal)
- Create node detail view (NodeDetailView)
- Update EventSummary screen
- Update NodesPanel component
- Integrate with ApiService
- Add emoji support
- Add localization

### Phase 4: Testing
- Write unit tests for utilities
- Write component tests
- Write integration tests
- Test offline-first behavior
- Test localization (EN/RU)
- Test emoji display

### Phase 5: Polish
- Code review and linting
- Performance optimization
- Documentation updates
- Quickstart validation

## Progress Tracking

- [X] Phase 0: Research - **COMPLETE** (research.md generated)
- [X] Phase 1: Design - **COMPLETE** (data-model.md, contracts/, quickstart.md generated)
- [ ] Phase 2: Tasks - **PENDING** (run `/speckit.tasks` to generate tasks.md)
- [ ] Phase 3: Implementation - **NOT STARTED**
- [ ] Phase 4: Testing - **NOT STARTED**
- [ ] Phase 5: Polish - **NOT STARTED**

## Next Steps

1. Run `/speckit.plan` command to execute Phase 0 (Research) and Phase 1 (Design)
2. Review generated research.md, data-model.md, quickstart.md, and contracts/
3. Run `/speckit.tasks` command to generate tasks.md
4. Begin implementation following tasks.md

## References

- Android Impacts/Judgments Spec: `specs/018-android-impacts-judgments/spec.md`
- Android Node Details Spec: `specs/019-android-node-details/spec.md`
- Desktop Quickstart: `docs/quickstart_desktop.md:306-332`
- Desktop EventSummary: `ui/desktop/src/pages/EventSummary.tsx`
- Desktop NodesPanel: `ui/desktop/src/components/NodesPanel.tsx`
- Desktop ApiService: `ui/desktop/src/services/api.ts`
- Desktop EmojiMapping: `ui/desktop/src/utils/emojiMapping.ts`
- Constitution: `.specify/memory/constitution.md`
