# Feature Specification: Unified Node Discovery & Address Exchange

**Feature Branch**: `008-specify-md`  
**Created**: November 17, 2025  
**Status**: Draft  
**Input**: User description: "Complete plan to implement, validate, and integrate a unified Node Discovery and Address Exchange system across all Truth Training modules."

## Execution Flow (main)
```
1. Establish canonical node schema and validation criteria shared by CLI, Server, Desktop UI, and Android client.
2. Design periodic discovery cadence spanning LAN, Wi-Fi, and global endpoints, including TTL-based cleanup and merge logic.
3. Define advertisement, handshake, and synchronization behaviors that keep repositories and network layers aligned.
4. Outline verification steps that confirm schema parity, migration integrity, and cross-platform readability.
5. Specify testing strategy (unit, integration, instrumentation, end-to-end CLI validation) that proves correctness before rollout.
6. Document open questions and required clarifications to finalize the plan.
```

## User Scenarios & Testing *(mandatory)*

### Primary User Story
Truth Training operators need every module (CLI, server, desktop UI, Android app) to share a consistent, up-to-date list of reachable nodes so collaborative training sessions and data sync continue even as devices move between local networks and the internet.

### Acceptance Scenarios
1. **Given** each module starts with a canonical schema snapshot, **When** the discovery cycle runs, **Then** node lists are merged, deduplicated, stale entries are retired, and new reachable endpoints are broadcast across all modules without manual intervention.
2. **Given** the Android client applies a Room migration, **When** the resulting database is opened by the CLI or server, **Then** all node records remain readable and categorized by type (LAN, Wi-Fi, Global, Relay/Server, Client App).
3. **Given** a node becomes unreachable past its TTL, **When** cleanup executes, **Then** dependent connection attempts stop and dashboards reflect the removal within a single sync interval.
4. **Given** a new relay advertises itself on a shared network, **When** desktop and mobile modules detect it, **Then** they add the record, initiate handshake, and schedule data exchange attempts via their respective sync services.

### Edge Cases
- How are conflicting TTLs or priority flags resolved when the same node appears from LAN and global discovery sources?
- How does the platform behave when schema validation fails on one module but passes on others—does discovery pause or switch to a safe mode?
- What is the fallback behavior when local broadcasts are blocked (e.g., captive portals) but internet discovery still works?

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: Define and document a canonical node record schema (IDs, address, last_seen, reachability status, node_type, source, priority, TTL) that all modules consume and persist without divergence.
- **FR-002**: Provide automated checks that verify SQLite/Room schemas, migrations, and repositories across CLI, server, desktop UI, and Android clients before discovery logic executes.
- **FR-003**: Implement a periodic discovery cadence covering LAN, Wi-Fi, open local networks, and global endpoints, merging results into a unified list while deduplicating by node identity.
- **FR-004**: Apply TTL and health heuristics that retire stale or unreachable nodes automatically and trigger downstream components to halt futile connection attempts.
- **FR-005**: Synchronize merged node lists across modules so updates in one environment propagate to all others within the next sync window.
- **FR-006**: Ensure advertisement and handshake flows broadcast newly reachable public endpoints and initiate data exchange attempts when fresh nodes are discovered.
- **FR-007**: Provide API or messaging adjustments that allow server/client communication layers to consume updated node inventories, including node type distinctions.
- **FR-008**: Deliver module-specific corrections (CLI, server, desktop, Android) so their database access layers, network services, and sync tasks read/write the canonical node schema.
- **FR-009**: Update automated tests—unit, integration, instrumentation, and CLI end-to-end—to cover schema validation, merge logic, cleanup behaviors, and cross-module interoperability.
- **FR-010**: Offer a CLI validation command that confirms schema parity, migration success, discovery status, and most recent sync across all connected modules.
- **FR-011**: Document merge conflict resolution rules for nodes reported by multiple sources: when duplicate addresses appear, prefer local (LAN/Wi-Fi) records over Global entries; if two local sources collide, use the most recent `last_seen`, and finally lexicographic address order as a tie-breaker.
- **FR-012**: Specify acceptable discovery intervals and TTL durations per node type to balance freshness with network overhead. [NEEDS CLARIFICATION: What interval/TTL targets should each platform meet?]

### Key Entities *(include if feature involves data)*
- **Node Record**: Represents any discoverable server/client endpoint with attributes for identifiers, network addresses, node type categories (LAN, Wi-Fi, Global, Relay/Server, Client App), discovery source, reachability metrics, TTL/expiry, and last handshake state.
- **Discovery Source**: Categorizes how a node was found (local broadcast, Wi-Fi scan, internet registry, manual input) and stores metadata such as signal strength or trust level to inform merge priority.
- **Sync Cycle**: Defines the periodic task configuration (frequency, scope, cleanup rules, merge order) each module runs to keep node lists aligned; includes timers, retry budgets, and propagation channels.
- **Validation Report**: Captures results of schema comparisons, migration checks, and cross-module compatibility audits; used by QA and operators to confirm readiness before enabling discovery.

## Clarifications

### Session 2025-11-17
- Q: How should merge priority handle duplicate nodes reported from multiple sources? → A: Prefer local (LAN/Wi-Fi) over Global

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [ ] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status
*Updated by main() during processing*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed

---
