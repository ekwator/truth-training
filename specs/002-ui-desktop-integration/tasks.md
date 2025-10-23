# Tasks: UI Desktop Integration (Tauri)

Feature Dir: `/home/ekwator/Code/truth-training/specs/002-ui-desktop-integration`
Branch: `002-ui-desktop-integration`

Conventions:
- [P] = can run in parallel with other [P] tasks (different files/areas)
- Use absolute paths; create files if missing
- Follow TDD: write tests first, then implement

## Phase 3.1: Setup
1. [X] T001 Initialize desktop app skeleton (Tauri + React/TS)
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/`
   - Actions: scaffold Tauri app, add React/TS, configure `tauri.conf.json`
   - Dep: —
2. [X] T002 Configure workspace scripts and CI for desktop build
   - Path: `/home/ekwator/Code/truth-training/.github/workflows/desktop.yml`
   - Actions: build/test on Linux/macOS/Windows, cache Rust/Node
   - Dep: T001
3. [X] T003 Add project-level tooling configs [P]
   - Paths: `/home/ekwator/Code/truth-training/ui/desktop/package.json`, `tsconfig.json`, `.eslintrc.cjs`, `jest.config.ts`
   - Actions: lint, format, test scripts
   - Dep: T001
4. [X] T004 Add core HTTP base URL and env wiring [P]
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/.env.*`
   - Actions: `VITE_API_BASE=http://localhost:8080/api/v1`
   - Dep: T001

## Phase 3.2: Tests First (Contracts & Scenarios)
5. [X] T101 Create contract tests for /sync/status [P]
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/sync.test.ts`
   - Assert schema from `contracts/api.yaml#SyncStatus`
   - Dep: T003,T004
6. [X] T102 Create contract tests for /events GET [P]
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/events-list.test.ts`
   - Assert pagination + Event shape
   - Dep: T003,T004
7. [X] T103 Create contract tests for /events POST [P]
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/events-create.test.ts`
   - Assert 201 and Event response
   - Dep: T003,T004
8. [X] T104 Create contract tests for /events/{id} GET [P]
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/event-get.test.ts`
   - Assert EventDetails shape
   - Dep: T003,T004
9. [X] T105 Create contract tests for /judgments GET [P]
   - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/judgments-list.test.ts`
   - Assert Judgment list shape
   - Dep: T003,T004
10. [X] T106 Create contract tests for /judgments POST [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/judgments-create.test.ts`
    - Assert 201 and Judgment response
    - Dep: T003,T004
11. [X] T107 Create contract tests for /consensus/{eventId} GET [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/consensus-get.test.ts`
    - Assert Consensus schema
    - Dep: T003,T004
12. [X] T108 Create contract tests for /consensus/{eventId}/calculate POST [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/contract/consensus-calc.test.ts`
    - Assert 200 and Consensus schema
    - Dep: T003,T004
13. [X] T109 Create integration test for primary user story (dashboard) [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/integration/dashboard-flow.test.ts`
    - Steps: load dashboard, list events, show sync
    - Dep: T101..T108
14. [X] T110 Create integration test for create-event flow [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/integration/create-event-flow.test.ts`
    - Steps: create event, verify appears
    - Dep: T101..T108
15. [X] T111 Create integration test for submit-judgment flow [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/integration/judgment-flow.test.ts`
    - Steps: submit judgment, verify
    - Dep: T101..T108
16. [X] T112 Create integration test for offline queue/sync [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/integration/offline-sync-flow.test.ts`
    - Steps per quickstart offline scenarios
    - Dep: T101..T108
17. [X] T113 Performance tests (<200ms) [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/tests/performance/api-performance.test.ts`
    - Dep: T101..T108

## Phase 3.3: Core Implementation
18. [X] T201 Implement API client layer (axios instance, typing)
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/services/api.ts`
    - Endpoints: events, judgments, consensus, sync
    - Dep: T101..T108
19. [X] T202 Define TypeScript types per data-model [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/types/{events.ts,judgments.ts,api.ts}`
    - Dep: T201
20. [X] T203 State management stores with Zustand [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/stores/{events.ts,judgments.ts,sync.ts}`
    - Dep: T201,T202
21. [X] T204 Offline queue module [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/services/offline.ts`
    - Features: enqueue, retry, backoff, persistence
    - Dep: T203
22. [X] T205 Sync orchestration module [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/services/sync.ts`
    - Features: status polling, flush queue, error handling
    - Dep: T204
23. [X] T206 Tauri commands bridge (optional FFI path)
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src-tauri/src/commands/`
    - Commands: fast-path operations if required
    - Dep: T201
24. [X] T207 App shell + routing
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/pages/{Dashboard.tsx,Events.tsx,Judgments.tsx,Settings.tsx}`
    - Dep: T203
25. [X] T208 UI components [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/components/{Dashboard,EventCreation,JudgmentPanel,ProgressVisualization,SyncStatus}/`
    - Dep: T207
26. [X] T209 Error boundary + notifications [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/components/system/{ErrorBoundary.tsx,Toaster.tsx}`
    - Dep: T207
27. [X] T210 Accessibility and i18n scaffolding [P]
    - Path: `/home/ekwator/Code/truth-training/ui/desktop/src/i18n/`
    - Dep: T207

## Phase 3.4: Integration
28. [X] T301 Wire API services to stores and pages
    - Paths: pages + stores + services
    - Dep: T201..T208
29. [X] T302 Implement offline-first flows (optimistic UI, queue)
    - Paths: services/offline.ts, stores, components
    - Dep: T204,T205,T301
30. [X] T303 Implement comprehensive error handling per FR-010
    - Paths: api.ts, ErrorBoundary, stores
    - Dep: T201,T209
31. [X] T304 Implement performance optimizations (<200ms)
    - Paths: components, services; code splitting, memoization
    - Dep: T301
32. [X] T305 Theming and preferences
    - Paths: `src/stores/preferences.ts`, `src/components/system/ThemeProvider.tsx`
    - Dep: T207

## Phase 3.5: Polish & Docs
33. [X] T401 Unit tests for stores/services/components [P]
    - Paths: `src/**/__tests__/*.test.ts(x)`
    - Dep: T201..T210
34. [X] T402 E2E tests with Playwright [P]
    - Paths: `tests/integration/*.test.ts`
    - Dep: T207..T210
35. [X] T403 Update quickstart with final commands [P]
    - Path: `/home/ekwator/Code/truth-training/specs/002-ui-desktop-integration/quickstart.md`
    - Dep: All
36. [X] T404 Update spec references and API contracts if drift [P]
    - Paths: `contracts/api.yaml`, `data-model.md`
    - Dep: All
37. [X] T405 CI: add desktop test job and badges
    - Paths: `.github/workflows/desktop.yml`, `README.md`
    - Dep: T402

## Parallel Execution Guidance
- Group 1 [P]: T003,T004
- Group 2 [P]: T101..T108 (independent contract tests)
- Group 3 [P]: T202,T203,T204,T205,T208,T209,T210
- Group 4 [P]: T401,T402,T403,T404

## Notes
- UI must prioritize basic metrics only (consensus, participant count)
- Anonymous access only; no account screens
- Dual connectivity (HTTP API + optional FFI via Tauri commands)
- Enforce <200ms response UX target in performance tests
