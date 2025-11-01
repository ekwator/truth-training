# Feature Specification: Context Fields Embedded in Events — UI Template Editor Screen

**Feature Branch**: `006-context-fields-embedded`  
**Created**: 2025-01-27  
**Status**: Draft  
**Input**: User description: "# Context fields embedded in events — UI Template Editor screen (initial 1.0.0 baseline)"

## Execution Flow (main)
```
1. Parse user description from Input
   → If empty: ERROR "No feature description provided"
2. Extract key concepts from description
   → Identify: actors, actions, data, constraints
3. For each unclear aspect:
   → Mark with [NEEDS CLARIFICATION: specific question]
4. Fill User Scenarios & Testing section
   → If no clear user flow: ERROR "Cannot determine user scenarios"
5. Generate Functional Requirements
   → Each requirement must be testable
   → Mark ambiguous requirements
6. Identify Key Entities (if data involved)
7. Run Review Checklist
   → If any [NEEDS CLARIFICATION]: WARN "Spec has uncertainties"
   → If implementation details found: ERROR "Remove tech details"
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers
- 🧠 Align with collective intelligence principles and truth training methodology

### Section Requirements
- **Mandatory sections**: Must be completed for every feature
- **Optional sections**: Include only when relevant to the feature
- When a section doesn't apply, remove it entirely (don't leave as "N/A")

### For AI Generation
When creating this spec from a user prompt:
1. **Mark all ambiguities**: Use [NEEDS CLARIFICATION: specific question] for any assumption you'd need to make
2. **Don't guess**: If the prompt doesn't specify something (e.g., "login system" without auth method), mark it
3. **Think like a tester**: Every vague requirement should fail the "testable and unambiguous" checklist item
4. **Common underspecified areas**:
   - User types and permissions
   - Data retention/deletion policies  
   - Performance targets and scale
   - Error handling behaviors
   - Integration requirements
   - Security/compliance needs

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
As a user creating events, I want to select a context template by name so that the system automatically fills in the five context fields (category, forma, cause, develop, effect) into my event. When I view events, I want to see the context name if it matches a template, or have the option to create a new template from an event's field values.

### Acceptance Scenarios

1. **Given** a user is creating a new event, **When** they select a context template by name from a dropdown/selector, **Then** the five context fields (category_id, forma_id, cause_id, develop_id, effect_id) are automatically prefilled with values from that template, and the event is saved with these embedded fields (not a context_id reference).

2. **Given** a user views an event in the list or detail view, **When** the event's non-NULL context field values match an existing context template (matching compares only non-NULL fields), **Then** the context template name is displayed; otherwise, **Then** a "[Create Template]" option is shown that opens the Context Editor screen.

3. **Given** a user views an event that doesn't match any existing template, **When** they click "[Create Template]", **Then** the Context Editor screen opens with all five context fields and event description prefilled, **And** they can modify fields and create a new template with a name and optional description.

4. **Given** a user is creating a new context template in the Context Editor, **When** they submit a template with field values that exactly match an existing template, **Then** the system shows "Template already exists" message and prevents duplicate creation.

5. **Given** a user is working with events that have embedded context fields, **When** they perform any event-related operations (view, edit, filter, search), **Then** the system uses the embedded fields directly without requiring a context_id lookup.

### Edge Cases
- **Template modification**: When a user selects a context template but then manually changes one or more of the five embedded fields before saving, the event saves with the modified fields. Template matching for display purposes compares only non-NULL fields, so if the modified event's non-NULL fields no longer match any template, it will show "[Create Template]" option in the event list.
- How does the system handle events that were created before this change (with context_id)? [Clarified: User specified "no database migrations executed automatically" — assume manual data migration or backward compatibility handling will be addressed separately]
- **Invalid foreign key references**: If a user provides an invalid FK reference (e.g., category_id=99999 pointing to a non-existent record), the system MUST reject the request immediately with an error message and prevent saving the event or template. This ensures data integrity and prevents orphaned references.
- **Duplicate template detection**: Duplicate detection compares only non-NULL fields. If all non-NULL field values (category_id, forma_id, cause_id, develop_id, effect_id) match an existing template, the system considers it a duplicate and prevents creation. NULL values are ignored in the comparison.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to select a context template by name when creating an event, and automatically populate the five context fields (category_id, forma_id, cause_id, develop_id, effect_id) from that template.

- **FR-002**: System MUST store events with the five context fields embedded directly in the event record, removing dependency on context_id foreign key reference.

- **FR-003**: System MUST display the context template name in event list and detail views when the event's embedded fields match an existing template. Matching compares only non-NULL fields: if all non-NULL field values match a template, the template name is displayed.

- **FR-004**: System MUST show a "[Create Template]" option in event views when the event's embedded fields do not match any existing template.

- **FR-005**: System MUST provide a Context Editor screen with fields for: name, category_id, forma_id, cause_id, develop_id, effect_id, and description.

- **FR-006**: System MUST allow users to create a new context template from an event's field values via the Context Editor, with all fields prefilled from the event.

- **FR-007**: System MUST detect duplicate templates when a user attempts to create a template with field values that exactly match an existing template (comparing only non-NULL fields), and display "Template already exists" message.

- **FR-008**: System MUST prevent creation of duplicate templates. Duplicate detection compares only non-NULL fields: if all non-NULL field values match an existing template, it is considered a duplicate.

- **FR-009**: System MUST validate foreign key references for category_id, forma_id, cause_id, develop_id, and effect_id when creating events or templates. If any FK reference points to a non-existent record, the system MUST reject the request immediately with an error message and prevent saving.

- **FR-010**: System MUST maintain backward compatibility or provide a migration path for existing events that use context_id [User note: no automatic migrations, manual handling expected].

### Key Entities *(include if feature involves data)*

- **Event**: Represents a truth/truth-training event. Contains embedded context fields (category_id, forma_id, cause_id, develop_id, effect_id) directly instead of referencing context_id. Must validate foreign key references to category, forma, cause, develop, and effect tables.

- **Context Template**: A reusable template that defines a named set of context field values (category_id, forma_id, cause_id, develop_id, effect_id, plus name and description). Users can select templates when creating events to auto-fill embedded fields. Templates must be unique by field combination.

- **Category**: Reference entity for event categorization (existing entity, no changes required).

- **Forma**: Reference entity for logical form classification (existing entity, no changes required).

- **Cause**: Reference entity for cause classification (existing entity, no changes required).

- **Develop**: Reference entity for manifestation classification (existing entity, no changes required).

- **Effect**: Reference entity for consequence classification (existing entity, no changes required).

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Clarifications

### Session 2025-01-27
- Q: What should happen when a user selects a template but then modifies one of the five embedded fields before saving the event? → A: Event saves with modified fields; template matching ignores this event (shows "[Create Template]" in list)
- Q: If a user provides an invalid FK reference (e.g., category_id=99999 pointing to a non-existent record), what should the system do? → A: Reject the request immediately with error message; do not save the event/template
- Q: How should duplicate template detection handle NULL values when comparing the five context fields? → A: NULL values are ignored in duplicate detection (only non-NULL fields are compared for duplicates)
- Q: For template matching (displaying context name in event list), should NULL values be handled the same way as duplicate detection (only compare non-NULL fields), or require exact matches including NULL positions? → A: only compare non-NULL fields

## Execution Status
*Updated by main() during processing*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
