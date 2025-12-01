## Deployment: Desktop UI with Background Server Service

This document describes how the Desktop UI bundles and installs the background service `truth-training-server` and how services are installed and verified across OSes.

### Server Binary

- Built by CI alongside the Tauri Desktop app:
  - Linux/macOS: `target/release/truth-training-server`
  - Windows: `target/release/truth-training-server.exe`

### Linux (Deb/RPM)

- Tauri bundler is configured to include:
  - Binary to `/usr/local/bin/truth-training-server`
  - systemd unit: `/lib/systemd/system/truth-training-server.service`
  - Post-install script: `/DEBIAN/postinst` (Deb)

- Post-install script performs:
  - `systemctl daemon-reload`
  - `systemctl enable truth-training-server`
  - `systemctl start truth-training-server`

- Verify:
  - `systemctl status truth-training-server`

### Windows (NSIS/MSI)

- CI downloads WinSW into `packaging/windows/winsw.exe` and installs a Windows service using:
  - Config: `packaging/windows/truth-training-server.xml`
  - Postinstall: `packaging/windows/postinstall.ps1`

- Verify:
  - `sc query TruthTrainingServer`

### macOS (App/DMG)

- LaunchDaemon plist: `packaging/macos/com.truth.training.server.plist`
- Postinstall script loads and starts daemon:
  - Copies plist to `/Library/LaunchDaemons/`
  - `launchctl load /Library/LaunchDaemons/com.truth.training.server.plist`
  - `launchctl start com.truth.training.server`

- Verify:
  - `launchctl list | grep truth.training.server`

### Desktop UI Integration

- The UI should be able to connect to the local server at `http://127.0.0.1:8080` (default) when in HTTP mode. Settings screen allows switching modes and testing connectivity.

---

## Database Migration Notes (v1.0.0)

**⚠️ IMPORTANT**: This release includes breaking changes to the database schema. **No automatic migrations are executed**. Manual migration is required for existing databases.

### Migration Requirements

1. **Backup Database**: Before migration, create a backup of your existing database.

2. **Schema Changes**:
   - Remove `context_id` column from `truth_events` table
   - Add five new nullable columns: `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`
   - Add foreign key constraints and indexes for new columns

3. **Data Migration**:
   - For existing events with `context_id`, extract context template values from `context` table
   - Populate embedded fields in `truth_events` based on template values
   - Ensure all FK references are valid before completing migration

4. **Migration Script**:
   A manual migration script should be provided or executed by database administrators. The script should:
   - Preserve existing event data
   - Map `context_id` → embedded fields based on template data
   - Validate all FK references after migration
   - Report any orphaned or invalid data

5. **Verification**:
   After migration, verify:
   - All events have valid embedded fields or NULL values
   - No orphaned FK references exist
   - Indexes are created for query performance
   - Template matching works correctly

For detailed migration instructions, see [[docs/Data_Schema.md](docs/Data_Schema.md)](Data_Schema.md) section "Migration Notes".



