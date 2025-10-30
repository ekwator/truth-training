# Feature Specification: Anonymous Trust Propagation Protocol (ATPP)

**Feature Branch**: `004-anonymous-trust-propagation`  
**Created**: 2025-10-30  
**Status**: Draft  
**Input**: User description: "Enable anonymous, verifiable propagation of encrypted events across a distributed peer network, without direct sender–receiver identification or explicit acknowledgements."

---

## Execution Flow (main)
```
1. Parse user description from Input
2. Extract key concepts: Anonymous event propagation, indirect confirmation, semantic trust via network echo
3. Ambiguity noted: How does a user/device determine 'context-matched' decryption?
4. User Scenarios: Created (see below)
5. Requirements: Drafted to cover propagation, validation, trust assessment, anonymity
6. Entities: Event object, Node
7. Review checklist: To be completed after stakeholder review
```
---

## ⚡ Quick Guidelines
- ✅ Focus: What – Specify ATPP for decentralized, anonymous encrypted event propagation with indirect, collective confirmation.
- ❌ Avoid tech stack specifics, no protocol syntax, code, or crypto API names.
- 👥 Audience: Business/product/design stakeholders in trust networks, research, P2P.
- 🧠 Alignment: Supports collective intelligence-based trust emergence and truth training principles.

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
A user wishes to share an encrypted “signal” (event) into a distributed, peer-to-peer trust network—without knowing (and without anyone knowing) who receives it. The user’s confidence that the signal was accepted emerges by observing that it continues to appear in the network over time, even if relayed by unknown parties.

### Acceptance Scenarios
1. **Given** an operational mesh network and an active user device, **When** the user creates and injects an event, **Then** the event propagates through multiple nodes, с отображением и автообновлением ETF, и если TTL истёк без возврата (эхо), в UI отображается сообщение: «Cобытие не было подтверждено/доверено».
2. **Given** a node configured for ATPP, **When** it receives an unknown event, **Then** it can independently validate origin (via cryptographic signature), optionally decrypt (if context matches), and choose to retransmit without learning sender/recipient identity.

### Edge Cases
- What happens if network partition occurs (event never returns; does this mean rejection or just probability)?
- How does system handle a malicious node injecting many events (spam, denial of trust propagation)? Не допускается: каждый узел ограничивает частоту публикации и ретрансляции событий, предотвращая массовое "заливание" (anti-spam via rate limiting).
- What if event's TTL expires before reflecting? UI обязан показать сообщение: «Cобытие не было подтверждено/доверено».
- How are node privacy and resistance to intersection attacks (inferring sender/receiver via network graph) guaranteed? Применяется только базовое сквозное шифрование и полное сокрытие маршрутизации и идентификаторов отправителя/получателя; дополнительных мер не предусмотрено (архитектурный компромисс).
- Are there minimum/maximum propagation counts enforced, or does emergent trust derive purely statistically?
- Rate limit (anti-spam): лимит публикации/ретрансляции событий по умолчанию — 1 событие в 30 секунд, параметр конфигурируемый. Попытка превысить лимит приводит к временной блокировке данного узла на публикацию последующих событий.

---

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: The protocol MUST enable creation, encryption, and injection of new events by any node, without disclosing node identity.
- **FR-002**: The system MUST transmit events via indirect, anonymous relay (no address-based routing; content-addressed via event hash/id).
- **FR-003**: Every event MUST be signed by its origin node for integrity, but signature MUST NOT reveal sender identity to other network parties.
- **FR-004**: Each received event MUST support independent local validation (hash, signature, timestamp, TTL).
- **FR-005**: Retransmitting nodes MUST increment propagation count and may (optionally, not always) append witness signature to propagation state.
- **FR-006**: Each event MUST automatically expire after its TTL elapses, ensuring natural fade-out and ephemerality.
- **FR-007**: Users MUST be able to observe whether previously injected events are being reflected and updated in the network, without identifying who reflected it.
- **FR-008**: The protocol MUST derive “Emergent Trust Factor” (ETF) from propagation stability, witness diversity, and activity duration—purely from network behavior, not direct confirmation.
- **FR-009**: The protocol MUST enforce full content encryption at event payload level, restrictable so only context-matched nodes can decrypt.
- **FR-010**: The protocol MUST not transmit or embed any explicit sender or receiver identifiers in any propagated data.
- **FR-011**: The system MUST support easy, verifiable auditing of the propagation state, while maintaining full node anonymity.
- **FR-012**: System MUST prevent or resist replay attacks and passive tracking over time, и реализовывать механизм ограничения частоты публикации/ретрансляции событий на каждом узле (rate limiting): по умолчанию не чаще 1 события в 30 секунд на узел; лимит должен быть конфигурируемым для администраторов сети. Для приватности/анонимности применяются только сквозное шифрование и запрет любой идентификации и маршрутизации по узлам. Дополнительные меры против intersection/graфовых атак в данной версии отсутствуют (см. Clarifications; архитектурный компромисс).
- **FR-013**: Any node MAY act as origin, witness, or relay at any time, without special privileges or configuration.
- **FR-014**: Система ДОЛЖНА предоставлять пользователю в интерфейсе (UI) актуальное значение ETF (Emergent Trust Factor) для каждого события и обновлять его в реальном времени или при любых изменениях показателя.
- **FR-015**: Если срок TTL события истёк, а событие не было отражено для отправителя, UI обязан явно информировать пользователя сообщением: «Cобытие не было подтверждено/доверено».

### Key Entities
- **Event Object**: Represents an encrypted signal, with these key attributes:
  - id (unique hash of content + signature)
  - payload (encrypted binary data)
  - signature (cryptographic, proofs origin but not identity)
  - ttl (time-to-live/Lifetime)
  - propagation_state (count, witness list, last_seen timestamp)
- **Node**: A participant in the P2P network, capable of creating, receiving, validating, relaying, and observing events. Each node holds unique crypto keys and operates fully anonymously in the protocol.

---

## Review & Acceptance Checklist
*GATE: Automated and team review*

### Content Quality
- [ ] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain (see above for open points)
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status
*Updated by AI output & reviewers*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed

---

## Clarifications
### Session 2025-10-30
- Q: Какой основной механизм устойчивости к спаму и предотвращения атаки "заливания" сети событиями должен применяться в ATPP? → A: Ограничение частоты публикации/пересылки событий на узел
- Q: Каким образом протокол ATPP должен обеспечивать устойчивость к атаке на приватность (intersection attack, анализ траектории или "сетевой граф раскрытия")? → A: Нет специальных мер, кроме минимальных шифрований и сокрытия маршрутизации.
- Q: Каким образом требуется отслеживать и предоставлять пользователю сведения о “Emergent Trust Factor” (ETF) каждого события? → A: ETF показывается и обновляется в пользовательском UI
- Q: Если TTL события истёк, но оно не было отражено обратно отправителю, какую реакцию должен предложить UI пользователю? → A: Сообщение: «Cобытие не было подтверждено/доверено»
- Q: Какой лимит частоты публикации/ретрансляции на узел? → A: По умолчанию 1 событие в 30 секунд, параметр должен быть конфигурируемым
