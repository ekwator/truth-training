## v1.0.0 Baseline — First Stable Release

**Context Fields Embedded in Events**

This is the first stable baseline release of Truth Training, marking a significant milestone with the integration of context fields directly into events and the introduction of a comprehensive Context Template System.

### Major Features

#### Context Fields Embedded
- Events now store context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) directly
- Removed dependency on `context_id` foreign key lookups
- Improved query performance (no JOINs required for context information)

#### Context Template System
- New **Context Editor UI screen** for creating and managing reusable context templates
- Template selection auto-prefills event form fields, streamlining event entry
- NULL-aware duplicate detection (compares only non-NULL fields)
- NULL-aware template matching for display

#### API Enhancements
- Updated `POST /events` accepts embedded fields, rejects `context_id`
- New `GET /contexts` endpoint for listing templates
- New `POST /contexts` endpoint for creating templates with duplicate detection
- New `GET /contexts/by-name/{name}` endpoint
- New `POST /contexts/match` endpoint for template matching
- New `POST /contexts/from-event` endpoint for creating templates from events

#### Validation & Data Integrity
- Foreign key validation rejects invalid references immediately (400 error)
- Duplicate template detection prevents creation (409 Conflict)
- All FK references validated before persistence

#### UI Improvements
- Context template dropdown selector on NewEvent page
- Field prefilling from templates (modifiable before save)
- Context name display in event list when template matched
- New ContextEditor page for template creation and editing

### Breaking Changes

⚠️ **Manual database migration required** (no automatic migrations)

- Removed `context_id` foreign key from `truth_events` table
- Added five embedded fields: `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`
- Existing events with `context_id` must be migrated manually
- See [CHANGELOG.md](../CHANGELOG.md) for detailed migration notes

### Version Bump

All crates and components bumped to v1.0.0:
- `core_lib`: v1.0.0
- `truth_core`: v1.0.0
- `app`: v1.0.0
- `truth-ui-desktop`: v1.0.0

### Full Changelog

See [CHANGELOG.md](../CHANGELOG.md) or the [GitHub history](https://[github.com/ekwator/truth-training/blob/master/CHANGELOG.md](github.com/ekwator/truth-training/blob/master/CHANGELOG.md)#100--first-stable-baseline--context-fields-embedded) for complete details.


