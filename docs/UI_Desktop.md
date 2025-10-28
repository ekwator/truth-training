## Desktop UI Updates (Tauri) - Text-Only Interface

This document summarizes the Desktop UI (Tauri) implementation with text-only interface design.

### Key Features
- **Text-Only Interface**: No icons, emojis, or graphical assets - pure text and structured layout
- **SQLite Persistence**: Events, impacts, summaries, and judgments stored in SQLite (rusqlite 0.31, bundled)
- **Offline-First**: Local-wins conflict resolution with background sync when online
- **Knowledge Base Integration**: Dynamic context selection from `docs/Data_Schema.md`

### Data Models
- **Events**: `id`, `title`, `description`, `context_id` (required), `start_date`, `end_date`, `created_at`, `updated_at`, `status`
- **Impacts**: `id`, `event_id`, `impact_level` (1-5), `notes`, `created_at`
- **Summaries**: `id`, `event_id`, `summary_text`, `recommendations`, `updated_at` (1:1 with events)
- **Judgments**: `id`, `event_id`, `assessment` ('true'|'false'|'uncertain'), `confidence_level` (0-1), `reasoning`, `submitted_at`
- **Logs**: `id`, `timestamp`, `source`, `level`, `message` (paginated at 35 lines/page)

### Tauri Commands
- **Events**: `create_event_fast`, `get_event_fast`, `list_events_fast` (with pagination)
- **Impacts**: `add_impact` (with validation 1-5 range)
- **Judgments**: `submit_judgment_fast`, `judgments_list_fast`, `get_judgment_stats`
- **Knowledge Base**: `knowledge_base_list` (parses Data_Schema.md)
- **Logs**: `list_logs`, `clear_logs` (35 lines/page pagination)
- **Summary**: `get_overall_metrics`, `list_event_rows`, `export_overall_summary_txt`

### Navigation & Shortcuts
- **Top Menu Bar**: [Home] | [New Event] | [Event Summary] | [Overall Summary] | [Training Results] | [Logs]
- **Keyboard Shortcuts**: Alt+1 (Home), Alt+2 (New Event), Alt+3 (Event Summary), Alt+4 (Overall Summary), Alt+5 (Training Results), Alt+6 (Logs)
- **Text-Only Design**: No icons, emojis, or graphical assets - pure text and structured layout

### Offline Queue & Sync
- **Local-Wins Strategy**: Local changes take precedence over remote changes
- **Background Sync**: Automatic sync when connection restored
- **Retry Mechanism**: Configurable max retries (default: 3)
- **Real-time Status**: Live sync status updates in UI

### Validation Rules
- **Impact Level**: Must be integer between 1-5
- **Date Order**: Start date must be before or equal to end date
- **Context ID**: Required, must match format `kb:[a-z0-9_-]+`
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

