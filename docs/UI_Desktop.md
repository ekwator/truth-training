## Current Version
Truth UI Desktop v1.0.0
Compatible with Core/Server v1.0.0

> Note: This UI uses a text-only interface (no graphical icons) for all functionality unless otherwise stated.

> **For comprehensive functional specification, see:** [spec/23-function_desktop.md](../spec/23-function_desktop.md)

## Desktop UI Updates (Tauri) - Text-Only Interface

This document summarizes the Desktop UI (Tauri) implementation with text-only interface design.

### Key Features
- **Text-Only Interface**: No icons, emojis, or graphical assets - pure text and structured layout
- **SQLite Persistence**: Events, impacts, summaries, and judgments stored in SQLite (rusqlite 0.31, bundled)
- **Offline-First**: Local-wins conflict resolution with background sync when online
- **Knowledge Base Integration**: Dynamic context selection from [docs/Data_Schema.md](Data_Schema.md)
- **Context Template System (v1.0.0)**: Reusable context templates with template selection, matching, and duplicate detection

### Data Models
- **Events**: `id`, `title`, `description`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` (all optional), `start_date`, `end_date`, `created_at`, `updated_at`, `status`
- **Context Templates**: `id`, `name`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`, `description` (all context fields optional)
- **Impacts**: `id`, `event_id`, `impact_level` (1-5), `notes`, `created_at`
- **Summaries**: `id`, `event_id`, `summary_text`, `recommendations`, `updated_at` (1:1 with events)
- **Judgments**: `id`, `event_id`, `assessment` ('true'|'false'|'uncertain'), `confidence_level` (0-1), `reasoning`, `submitted_at`

### Tauri Commands
- **Events**: `create_event_fast`, `get_event_fast`, `list_events_fast` (with pagination)
- **Impacts**: `add_impact` (with validation 1-5 range)
- **Judgments**: `submit_judgment_fast`, `judgments_list_fast`, `get_judgment_stats`
- **Knowledge Base**: `knowledge_base_list` (parses [Data_Schema.md](Data_Schema.md))
- **Context Templates (v1.0.0)**: `list_contexts`, `get_context_by_name`, `create_context`, `match_context`, `create_context_from_event`
- **Summary**: `get_overall_metrics`, `list_event_rows`, `export_overall_summary_txt`
- **Configuration**: `get_app_config`, `save_app_config`, `core_status`, `test_http_connection`

### Navigation & Shortcuts
- **Top Menu Bar**: [Home] | [New Event] | [Context Editor] | [Event Summary] | [Overall Summary] | [Training Results] | [Settings]
- **Keyboard Shortcuts**: Alt+1 (Home), Alt+2 (New Event), Alt+3 (Context Editor), Alt+4 (Event Summary), Alt+5 (Overall Summary), Alt+6 (Training Results), Alt+8 (Settings)
- **Text-Only Design**: No icons, emojis, or graphical assets - pure text and structured layout

### Context Editor Screen (v1.0.0)

The Context Editor screen allows users to create and manage context templates:

- **Template Form**: Fields for `name`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`, `description` (all context fields optional)
- **Template Selection**: On the NewEvent page, a dropdown selector allows selecting a template to prefill event form fields
- **Field Prefilling**: When a template is selected, the five context fields are prefilled but remain modifiable before event creation
- **Duplicate Detection**: Attempting to create a template with identical non-NULL fields to an existing template displays "Template already exists" error and prevents creation
- **Create from Event**: Events without matching templates show a "[Create Template]" button that opens ContextEditor with fields prefilled from the event's embedded context
- **Template Matching**: Event list displays template name when event's embedded fields match a template (non-NULL field comparison)

### Context Picker Component (v1.0.0+)

The Context Picker is a searchable combobox component used for selecting context IDs (category, forma, cause, develop, effect) on the NewEvent page:

**Desktop Implementation:**
- **Searchable Dropdown**: Type to filter contexts by name or ID
- **Manual Entry**: Enter context ID directly; validation ensures ID exists in dataset
- **Validation**: Invalid IDs are blocked with inline error message "Unknown context ID"
- **Caching**: Context data is cached in localStorage for offline use (24h TTL)
- **Stale Data Warning**: Shows warning banner when cached data is >24h old
- **Error Handling**: Retry button on fetch failure; falls back to cached data when available
- **Telemetry**: Emits `context_picker.load.success|failure` and `context_picker.validation.failure` events for observability
- **Accessibility**: ARIA attributes, keyboard navigation (arrow keys, Enter, ESC)

**Android Implementation (Parity):**
- **Dropdown UI**: Uses `ExposedDropdownMenuBox` with human-readable labels from embedded Room database
- **Manual Entry**: Enter context ID directly; validation ensures ID exists in lookup tables
- **Validation**: Invalid IDs are blocked with inline error states, submission prevented
- **Data Source**: Contexts loaded from embedded Room database via `ContextTemplateRepository.getAllTemplatesFlow()`
- **Error Handling**: Shows error state card when context data is unavailable, allows retry
- **Logging**: Logcat logging for context loading errors and validation failures (telemetry optional)
- **Accessibility**: Material3 accessibility attributes, keyboard navigation support

### Template Selection Workflow

1. **On NewEvent Page**:
   - User can select a context template from dropdown (legacy template selection)
   - Template selection prefills five context fields (category, forma, cause, develop, effect)
   - Each context field uses a ContextPicker component for validation and search
   - User can modify prefilled fields before saving
   - Invalid context IDs are blocked with inline error states
   - Form submission sends embedded fields instead of `context_id`

2. **On Events List Page**:
   - System matches each event's embedded fields to context templates
   - If match found (non-NULL fields match), displays template name
   - If no match, shows "[Create Template]" button
   - Clicking button opens ContextEditor with event's fields prefilled

### App Settings
- **Connection Mode Toggle**: Choose between Core (Local) and HTTP API modes
- **Server Configuration**: IP address and port settings for HTTP mode
- **Language Selection (v1.0.0+)**: RU/EN toggle in top-right navigation and Settings → General
  - Supported locales: English (en), Russian (ru)
  - Locale persists in `AppConfig.locale` and `localStorage` key `truth-locale`
  - UI updates instantly without reload on locale change
  - Missing translations fallback to English with console warning
  - Telemetry: `locale.change` events logged with `{from, to, success}`

### Android Localization Status
- **Current Status**: **English-only (EN)**
- **Language Switching**: Not implemented in Android client
- **Parity Note**: Desktop UI supports RU/EN switching; Android localization parity is planned for a future release
- **String Consistency**: Android English strings should match Desktop English strings for consistency
- **Validation Rules**: IP format validation (`^\d{1,3}(\.\d{1,3}){3}$`), port range (1-65535)
- **Test Connection**: Test Core or HTTP connections with real-time feedback
- **Persistence**: Configuration saved to `~/.truth-training/config.json`
- **Configuration Schema**:
  ```json
  {
    "mode": "core" | "http",
    "server_ip": "127.0.0.1",
    "server_port": 8080,
    "locale": "en" | "ru"
  }
  ```

### Offline Queue & Sync
- **Local-Wins Strategy**: Local changes take precedence over remote changes
- **Background Sync**: Automatic sync when connection restored
- **Retry Mechanism**: Configurable max retries (default: 3)
- **Real-time Status**: Live sync status updates in UI

### Validation Rules
- **Impact Level**: Must be integer between 1-5
- **Date Order**: Start date must be before or equal to end date
- **Context Fields (v1.0.0+)**: All context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) are optional. When provided, must reference existing records in their respective tables. Invalid FK references are blocked by ContextPicker validation with inline error states. Invalid IDs emit `context_picker.validation.failure` telemetry events.
- **Template Duplicate Detection**: Context templates with identical non-NULL fields cannot be created (409 Conflict). NULL values are ignored in comparison.
- **Confidence Level**: Must be between 0.0-1.0
- **Assessment**: Must be 'true', 'false', or 'uncertain'
- **Event Title**: Required, max 200 characters

### Performance Targets
- **Navigation**: < 100ms between screens
- **Pagination**: < 100ms for 35-item pages
- **Search**: < 50ms for 1000-item datasets
- **Memory**: No significant leaks during navigation

### Testing
- **Unit Tests**: Validation rules, offline queue, knowledge base integration
- **Performance Tests**: Navigation speed, pagination efficiency, memory usage
- **Integration Tests**: End-to-end event creation, judgment submission, offline sync

### Files Structure
- **UI Components**: `ui/desktop/src/pages/`, `ui/desktop/src/components/`
- **Services**: `ui/desktop/src/services/` (API, offline queue, validation)
- **Tauri Backend**: `ui/desktop/src-tauri/src/` (commands, storage, main)
- **Tests**: `ui/desktop/src/services/__tests__/`, `ui/desktop/tests/performance/`

### CI Status
- ✅ Desktop builds: Linux/Windows/macOS (rusqlite bundled)
- ✅ Mobile builds: Android/iOS (truth_core only)
- ✅ Cross-platform artifacts: libtruth_core-desktop per OS


