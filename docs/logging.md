# Logging Guide

This document describes how logging works in Truth Training, where log files are located, how to read them, and how to clear them.

## Overview

Truth Training uses different logging mechanisms depending on the platform:

- **Desktop UI**: Logs stored in SQLite database (`logs` table)
- **Android**: System Logcat
- **Server**: Structured logging via `env_logger` (Rust) with `RUST_LOG` environment variable
- **CLI**: Logs stored in SQLite database, accessible via `truthctl logs` commands

## Desktop UI Logging

### Location

Desktop UI stores logs in the SQLite database alongside application data:

- **Linux**: `${XDG_DATA_HOME:-~/.local/share}/TruthTraining/truth_training.sqlite` (table: `logs`)
- **macOS**: `~/Library/Application Support/TruthTraining/truth_training.sqlite` (table: `logs`)
- **Windows**: `%APPDATA%\TruthTraining\truth_training.sqlite` (table: `logs`)

### Reading Logs

#### Via UI

1. Open the Desktop application
2. Navigate to **Logs** page in the navigation menu
3. Logs are displayed in a paginated table (35 entries per page by default)
4. Each log entry shows:
   - **ID**: Unique identifier
   - **Timestamp**: When the log was created
   - **Source**: Component that generated the log
   - **Level**: Log level (INFO, WARN, ERROR, DEBUG)
   - **Message**: Log message content

#### Via Tauri Command (Developer)

```bash
# Using Tauri CLI (if available)
tauri invoke list_logs --page 1
```

#### Via SQLite (Direct Access)

```bash
# Linux/macOS
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "SELECT id, timestamp, source, level, message FROM logs ORDER BY datetime(timestamp) DESC LIMIT 50;"

# macOS
sqlite3 ~/Library/Application\ Support/TruthTraining/truth_training.sqlite \
  "SELECT id, timestamp, source, level, message FROM logs ORDER BY datetime(timestamp) DESC LIMIT 50;"

# Windows (PowerShell)
sqlite3 "$env:APPDATA\TruthTraining\truth_training.sqlite" \
  "SELECT id, timestamp, source, level, message FROM logs ORDER BY datetime(timestamp) DESC LIMIT 50;"
```

### Clearing Logs

#### Via UI

1. Open the Desktop application
2. Navigate to **Logs** page
3. Click the **Clear** button
4. Confirm the action

#### Via Tauri Command (Developer)

```bash
tauri invoke clear_logs
```

#### Via SQLite (Direct Access)

```bash
# Linux/macOS
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite "DELETE FROM logs;"

# macOS
sqlite3 ~/Library/Application\ Support/TruthTraining/truth_training.sqlite "DELETE FROM logs;"

# Windows (PowerShell)
sqlite3 "$env:APPDATA\TruthTraining\truth_training.sqlite" "DELETE FROM logs;"
```

### Log Structure

The `logs` table schema:

```sql
CREATE TABLE logs (
    id TEXT PRIMARY KEY,
    timestamp TEXT NOT NULL,
    source TEXT NOT NULL,
    level TEXT NOT NULL,
    message TEXT NOT NULL
);
```

## Android Logging

### Location

Android uses the system Logcat for all logging. Logs are not stored in files by default but can be captured via `adb`.

### Reading Logs

#### Via ADB (Recommended)

```bash
# View all logs
adb logcat

# Filter by tag (e.g., TruthTrainingApplication)
adb logcat -s TruthTrainingApplication:D MainActivity:D

# Filter by log level
adb logcat *:E  # Errors only
adb logcat *:W  # Warnings and above
adb logcat *:I  # Info and above

# Save logs to file
adb logcat > android_logs.txt

# Clear logcat buffer
adb logcat -c
```

#### Via Android Studio

1. Open Android Studio
2. Connect device or start emulator
3. Open **Logcat** window (View → Tool Windows → Logcat)
4. Filter by package: `com.truth.training.client`
5. Filter by log level using dropdown

### Log Tags

Common log tags used in Android app:

- `TruthTrainingApplication`: Application lifecycle and database initialization
- `MainActivity`: Main activity lifecycle and UI initialization
- `TruthDatabase`: Database operations and migrations
- `ContextPicker`: Context picker component operations
- `EventRepository`: Event data operations
- `P2PMessageHandler`: P2P message handling
- `P2PSyncManager`: P2P synchronization
- `NodeSyncWorker`: Background sync worker

### Log Levels

- `Log.v()`: Verbose (DEBUG)
- `Log.d()`: Debug
- `Log.i()`: Info
- `Log.w()`: Warning
- `Log.e()`: Error

### Clearing Logs

```bash
# Clear logcat buffer
adb logcat -c
```

Note: This only clears the buffer, not persistent logs. Android system logs are managed by the OS.

## Server Logging

### Location

Server logs are output to stdout/stderr by default. When running as a service, logs may be redirected to system log files:

- **Linux (systemd)**: `journalctl -u truth-core-server` or `/var/log/truth-core/service.log` (if configured)
- **macOS (LaunchAgent)**: `~/Library/Logs/truth-core-server.log` (if configured)
- **Windows (WinSW)**: `%BASE%\logs\` directory (see service XML configuration)

### Reading Logs

#### Linux (systemd)

```bash
# View recent logs
journalctl -u truth-core-server -n 100

# Follow logs in real-time
journalctl -u truth-core-server -f

# View logs since boot
journalctl -u truth-core-server -b

# View logs for specific time range
journalctl -u truth-core-server --since "2024-01-01 00:00:00" --until "2024-01-02 00:00:00"
```

#### macOS (LaunchAgent)

```bash
# View logs if configured to file
tail -f ~/Library/Logs/truth-core-server.log

# Or via Console.app
open -a Console
# Then search for "truth-core-server"
```

#### Windows (WinSW)

```powershell
# View log files
Get-Content "$env:PROGRAMFILES\TruthCoreServer\logs\*.log" -Tail 100

# Or via Event Viewer
eventvwr.msc
# Navigate to: Windows Logs → Application
# Filter by source: "Truth Core Server"
```

### Log Level Configuration

Server logging is controlled via the `RUST_LOG` environment variable:

```bash
# Set log level (from least to most verbose)
export RUST_LOG=error    # Errors only
export RUST_LOG=warn     # Warnings and errors
export RUST_LOG=info     # Info, warnings, and errors (default)
export RUST_LOG=debug    # Debug, info, warnings, and errors
export RUST_LOG=trace    # All logs (very verbose)

# Set per-module log levels
export RUST_LOG=truth_core=debug,actix_web=info

# Run server with log level
RUST_LOG=debug truth_core_server
```

### Service Configuration

#### Linux (systemd)

Edit `/etc/systemd/user/truth-core-server.service`:

```ini
[Service]
Environment=RUST_LOG=info
```

Then reload and restart:

```bash
systemctl --user daemon-reload
systemctl --user restart truth-core-server
```

#### macOS (LaunchAgent)

Edit `~/Library/LaunchAgents/com.truth.training.server.plist`:

```xml
<key>EnvironmentVariables</key>
<dict>
    <key>RUST_LOG</key>
    <string>info</string>
</dict>
```

#### Windows (WinSW)

Edit `truth_core_server.xml`:

```xml
<service>
    <env name="RUST_LOG" value="info"/>
</service>
```

### Clearing Logs

#### Linux (systemd)

```bash
# Clear journal logs (requires root)
journalctl --vacuum-time=1d  # Keep last 1 day
journalctl --vacuum-size=100M  # Keep last 100MB
```

#### macOS/Windows

Delete log files manually or configure log rotation in service configuration.

## CLI Logging

### Location

CLI logs are stored in the SQLite database specified via `--db` flag (default: `truth.db` in current directory).

### Reading Logs

#### Via CLI Command

```bash
# Show recent logs (default: 50 entries)
truthctl logs show

# Show specific number of entries
truthctl logs show --limit 100

# Show logs with verbose output
truthctl logs show --verbose
```

#### Via SQLite (Direct Access)

```bash
# If using default database
sqlite3 truth.db "SELECT * FROM logs ORDER BY timestamp DESC LIMIT 50;"

# If using custom database
sqlite3 /path/to/custom.db "SELECT * FROM logs ORDER BY timestamp DESC LIMIT 50;"
```

### Clearing Logs

#### Via CLI Command

```bash
# Clear all logs
truthctl logs clear
```

#### Via SQLite (Direct Access)

```bash
sqlite3 truth.db "DELETE FROM logs;"
```

## Telemetry Events

In addition to standard logging, Truth Training emits telemetry events for observability:

### Desktop UI Telemetry

Events are logged via `console.warn()` and `console.log()` in the browser console:

- `context_picker.load.success`: Context picker loaded successfully
- `context_picker.load.failure`: Context picker failed to load
- `context_picker.validation.failure`: Invalid context ID entered
- `translation.missing`: Missing translation key
- `locale.change`: Locale changed

**Viewing Desktop Telemetry:**

1. Open Desktop application
2. Open browser DevTools (F12 or Cmd+Option+I)
3. Navigate to **Console** tab
4. Filter by event name or use search

### Android Telemetry

Events are logged via `android.util.Log`:

- Same event names as Desktop
- Logged with appropriate log levels (WARN for failures, INFO for success)

**Viewing Android Telemetry:**

```bash
# Filter telemetry events
adb logcat -s TruthTrainingApplication:I MainActivity:I ContextPicker:I
```

## Log Rotation and Management

### Desktop UI

Logs are stored in SQLite and do not automatically rotate. To prevent database growth:

1. Regularly clear logs via UI or SQLite
2. Set up periodic cleanup (manual or scripted)

Example cleanup script:

```bash
#!/bin/bash
# Clear logs older than 30 days
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM logs WHERE datetime(timestamp) < datetime('now', '-30 days');"
```

### Server

Configure log rotation in service configuration:

#### Linux (systemd)

Use `journalctl` rotation (automatic) or configure external log rotation tool.

#### macOS/Windows

Configure log rotation in service XML/plist or use external tools.

### Android

Logcat buffer is managed by Android OS. No manual rotation needed.

## Troubleshooting

### Desktop UI: Logs Not Appearing

1. Check database file exists and is accessible
2. Verify `logs` table exists: `sqlite3 <db_path> ".tables" | grep logs`
3. Check application permissions to write to database location

### Android: Logs Not Visible

1. Ensure device is connected: `adb devices`
2. Check logcat buffer is not full: `adb logcat -c` to clear
3. Verify app is running and generating logs
4. Check log level filters are not too restrictive

### Server: Logs Not Showing

1. Verify `RUST_LOG` environment variable is set correctly
2. Check service is running: `systemctl --user status truth-core-server` (Linux)
3. Verify log output destination (stdout vs file)
4. Check service user has write permissions to log location

### CLI: Logs Command Fails

1. Verify database file exists and is accessible
2. Check database contains `logs` table: `sqlite3 <db_path> ".tables" | grep logs`
3. Ensure database is not locked by another process

## Best Practices

1. **Regular Cleanup**: Clear old logs periodically to prevent database/file growth
2. **Log Level**: Use appropriate log levels (INFO for production, DEBUG for development)
3. **Sensitive Data**: Never log sensitive information (passwords, keys, tokens)
4. **Performance**: Be mindful of log volume in production environments
5. **Monitoring**: Set up log monitoring/alerting for critical errors

## Related Documentation

- [Install Paths By OS](Install_Paths_By_OS.md) - File locations by platform
- [CLI Usage](CLI_Usage.md) - CLI commands including `logs` subcommands
- [Troubleshooting](troubleshooting.md) - General troubleshooting guide
- [UI Desktop](UI_Desktop.md) - Desktop UI features including Logs page

