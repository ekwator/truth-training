# Feature Specification: Android Impacts and Judgments UI Implementation

**Feature Branch**: `018-android-impacts-judgments`  
**Created**: 2025-12-10  
**Status**: Draft  
**Input**: User description: "@docs/quickstart_android.md:372-387 Этот функционал и соответствующие экраны не реализованы в приложении. Необходимо реализовать в соответствии со спецификацией проекта."

## Clarifications

### Session 2025-12-10

- Q: Should impacts use level 1-5 slider as described in quickstart_android.md? → A: Yes, impact level must be 1-5 as specified. The Android API uses `value: Boolean` but we need to map impact_level (1-5) to the boolean value. For now, impact_level > 3 maps to positive (true), <= 3 maps to negative (false), matching Desktop implementation pattern.
- Q: Should judgment submission be inline in EventDetailScreen or separate screen? → A: According to spec/24-function_mobile_android.md, judgment submission should be handled inline in Event Summary screen (EventDetailScreen) matching Desktop UI pattern. However, JudgmentSubmissionScreen.kt already exists as separate screen. We should integrate it into EventDetailScreen as a modal/dialog or inline form, or use navigation to existing screen.
- Q: Should impact addition be inline or separate screen? → A: According to spec/24-function_mobile_android.md, impact addition should be handled inline in Event Summary screen (EventDetailScreen) matching Desktop UI pattern. We should add an "Add Impact" button that opens a dialog/modal or navigates to a form screen.
- Q: How should impacts and judgments be displayed in EventDetailScreen? → A: According to spec/24-function_mobile_android.md, Event Summary Screen should display impacts list and judgments list. We need to add sections showing existing impacts and judgments for the event.
- Q: Should we follow Desktop UI implementation for impacts and judgments? → A: Yes, we should match Desktop UI patterns. Desktop uses inline forms/modals in EventSummary screen. Android should follow the same pattern for consistency.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Adding Impacts to Events (Priority: P1)

As an Android user, I want to add impacts to events with impact level (1-5) and optional notes, so that I can record the impact assessment for each event.

**Why this priority**: This is a core feature described in quickstart_android.md and required by spec/24-function_mobile_android.md. Without this functionality, users cannot complete the impact assessment workflow.

**Independent Test**: Can be fully tested by navigating to Event Detail screen, tapping "Add Impact" button, setting impact level 1-5 using slider, adding optional notes, and verifying impact is saved and displayed in impacts list.

**Acceptance Scenarios**:

1. **Given** I am viewing an event detail screen, **When** I tap "Add Impact" button, **Then** an impact form appears (dialog/modal or navigation to form screen) with impact level slider (1-5) and notes field
2. **Given** I am filling the impact form, **When** I set impact level using slider (1-5) and add optional notes, **Then** the form validates the input and enables Save button
3. **Given** I submit the impact form, **When** the impact is saved, **Then** the impact appears in the impacts list on Event Detail screen and the form closes
4. **Given** I view the impacts list, **When** I examine the list, **Then** each impact displays impact level, notes (if provided), and creation timestamp

---

### User Story 2 - Submitting Judgments for Events (Priority: P1)

As an Android user, I want to submit judgments for events with assessment (true/false/uncertain), confidence level (0.0-1.0), and optional reasoning, so that I can contribute to collective intelligence assessment.

**Why this priority**: This is a core feature described in quickstart_android.md and required by spec/24-function_mobile_android.md. Without this functionality, users cannot complete the judgment submission workflow.

**Independent Test**: Can be fully tested by navigating to Event Detail screen, tapping "Submit Judgment" button, selecting assessment, setting confidence level, adding optional reasoning, and verifying judgment is saved and displayed in judgments list.

**Acceptance Scenarios**:

1. **Given** I am viewing an event detail screen, **When** I tap "Submit Judgment" button, **Then** a judgment form appears (dialog/modal or navigation to form screen) with assessment selection (true/false/uncertain), confidence level slider (0.0-1.0), and reasoning field
2. **Given** I am filling the judgment form, **When** I select assessment and set confidence level, **Then** the form validates the input and enables Submit button
3. **Given** I submit the judgment form, **When** the judgment is saved, **Then** the judgment appears in the judgments list on Event Detail screen and the form closes
4. **Given** I view the judgments list, **When** I examine the list, **Then** each judgment displays assessment, confidence level, reasoning (if provided), and submission timestamp

---

### User Story 3 - Displaying Impacts and Judgments in Event Detail (Priority: P1)

As an Android user, I want to view all impacts and judgments for an event in the Event Detail screen, so that I can see the complete assessment history for the event.

**Why this priority**: This is required by spec/24-function_mobile_android.md. Event Summary Screen must display impacts list and judgments list. Without this, users cannot see existing assessments.

**Independent Test**: Can be fully tested by navigating to Event Detail screen and verifying that impacts list and judgments list are displayed with all relevant information (level, notes, assessment, confidence, reasoning, timestamps).

**Acceptance Scenarios**:

1. **Given** an event has impacts, **When** I view the Event Detail screen, **Then** the impacts list section displays all impacts with level, notes, and timestamp
2. **Given** an event has judgments, **When** I view the Event Detail screen, **Then** the judgments list section displays all judgments with assessment, confidence, reasoning, and timestamp
3. **Given** an event has no impacts or judgments, **When** I view the Event Detail screen, **Then** empty state messages are displayed for impacts and judgments sections
4. **Given** impacts or judgments are added, **When** I view the Event Detail screen, **Then** the lists update automatically to show new items

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: EventDetailScreen MUST display "Add Impact" button that opens impact form (dialog/modal or navigation).
- **FR-002**: Impact form MUST include impact level slider (1-5) and optional notes field.
- **FR-003**: Impact level MUST be validated to be in range 1-5.
- **FR-004**: Impact form MUST map impact_level (1-5) to boolean value: impact_level > 3 → positive (true), impact_level <= 3 → negative (false), matching Desktop implementation.
- **FR-005**: EventDetailScreen MUST display "Submit Judgment" button that opens judgment form (dialog/modal or navigation).
- **FR-006**: Judgment form MUST include assessment selection (true/false/uncertain), confidence level slider (0.0-1.0), and optional reasoning field.
- **FR-007**: Assessment MUST be validated to be "true", "false", or "uncertain".
- **FR-008**: Confidence level MUST be validated to be between 0.0 and 1.0.
- **FR-009**: EventDetailScreen MUST display impacts list section showing all impacts for the event.
- **FR-010**: EventDetailScreen MUST display judgments list section showing all judgments for the event.
- **FR-011**: Impacts list MUST display impact level (1-5), notes (if provided), and creation timestamp.
- **FR-012**: Judgments list MUST display assessment, confidence level, reasoning (if provided), and submission timestamp.
- **FR-013**: EventDetailScreen MUST display empty state messages when no impacts or judgments exist.
- **FR-014**: Impacts and judgments lists MUST update automatically when new items are added.
- **FR-015**: All UI elements MUST include appropriate emojis matching Desktop UI implementation (constitutional requirement Rule 8).
- **FR-016**: All text labels MUST be localized (English/Russian) using Android string resources.
- **FR-017**: Impact addition MUST use ImpactRepository.addImpact() for offline-first storage.
- **FR-018**: Judgment submission MUST use JudgmentRepository.submitJudgment() for offline-first storage.
- **FR-019**: EventDetailViewModel MUST observe impacts and judgments flows for reactive updates.

### Key Entities *(include if feature involves data)*

- **ImpactEntity**: Represents a recorded impact for an event. Key attributes: id, eventId, typeId, value (Boolean), notes, createdAt. Impact level (1-5) is mapped to typeId and value.
- **JudgmentEntity**: Represents a user's assessment of an event. Key attributes: id, eventId, assessment ("true"/"false"/"uncertain"), confidenceLevel (0.0-1.0), reasoning, submittedAt.
- **ImpactRepository**: Manages impact data with offline-first strategy. Key methods: getImpactsForEventFlow(), addImpact(), listImpactsForEvent().
- **JudgmentRepository**: Manages judgment data with offline-first strategy. Key methods: getJudgmentsForEventFlow(), submitJudgment(), getJudgmentStats().
- **EventDetailViewModel**: Manages event detail screen state. Must be extended to observe impacts and judgments flows, and provide methods for adding impacts and submitting judgments.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of Event Detail screens display "Add Impact" button with appropriate emoji matching Desktop UI (verified by visual inspection).
- **SC-002**: 100% of Event Detail screens display "Submit Judgment" button with appropriate emoji matching Desktop UI (verified by visual inspection).
- **SC-003**: Impact form successfully saves impacts with level 1-5 and optional notes (verified by testing all impact levels and verifying database storage).
- **SC-004**: Judgment form successfully saves judgments with assessment, confidence level, and optional reasoning (verified by testing all assessment types and confidence levels, verifying database storage).
- **SC-005**: Impacts list displays all impacts for event with level, notes, and timestamp (verified by adding multiple impacts and verifying display).
- **SC-006**: Judgments list displays all judgments for event with assessment, confidence, reasoning, and timestamp (verified by submitting multiple judgments and verifying display).
- **SC-007**: Empty state messages display when no impacts or judgments exist (verified by viewing event with no impacts/judgments).
- **SC-008**: Impacts and judgments lists update automatically when new items are added (verified by adding items and observing list updates).
- **SC-009**: All UI elements include appropriate emojis matching Desktop UI (verified by comparing with Desktop EventSummary screen).
- **SC-010**: All text labels are localized (English/Russian) (verified by switching language and verifying text changes while emojis remain constant).

---

_Version: v1.0.0_

