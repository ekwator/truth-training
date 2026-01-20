# Logging Guide

This document describes how logging works in Truth Training, specifically for the Node Discovery system, where log files are located, how to read them, and how to clear them.

## Overview

Truth Training uses structured logging for the Node Discovery system, storing logs in SQLite database tables. Unlike traditional logging approaches, there are no UI screens for viewing logs. All log access is performed through the CLI application `truthctl`.

The Node Discovery system maintains the following log tables in the SQLite database:

- `discovery_nodes` - Contains information about network discovery nodes for peer-to-peer connectivity
- `discovery_history` - Tracks changes in network node discovery for auditing and analyzing
- `sync_operations` - Records details of synchronization operations between nodes
- `sync_attempts` - Logs all synchronization attempts including success/failure status
- `node_performance` - Tracks performance and health metrics of individual nodes in the network
- `peer_synchronization` - Maintains historical record of peer synchronization activities

**Important**: There are no graphical user interface screens for viewing these logs. All log viewing and management must be done using the `truthctl` CLI application.

## CLI Logging Access

### Location

All Node Discovery logs are stored in the SQLite database specified via `--db` flag (default: `truth.db` in current directory).

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

#### View Specific Log Types

```bash
# View node discovery logs
truthctl logs show --table discovery_nodes

# View discovery history
truthctl logs show --table discovery_history

# View sync operations
truthctl logs show --table sync_operations

# View sync attempts
truthctl logs show --table sync_attempts

# View node performance logs
truthctl logs show --table node_performance

# View peer synchronization logs
truthctl logs show --table peer_synchronization
```

#### Via SQLite (Direct Access)

```bash
# If using default database
sqlite3 truth.db "SELECT * FROM discovery_nodes ORDER BY timestamp DESC LIMIT 50;"

# If using custom database
sqlite3 /path/to/custom.db "SELECT * FROM discovery_nodes ORDER BY timestamp DESC LIMIT 50;"

# Query specific tables
sqlite3 truth.db "SELECT * FROM discovery_history ORDER BY timestamp DESC LIMIT 50;"
sqlite3 truth.db "SELECT * FROM sync_operations ORDER BY timestamp DESC LIMIT 50;"
sqlite3 truth.db "SELECT * FROM sync_attempts ORDER BY timestamp DESC LIMIT 50;"
sqlite3 truth.db "SELECT * FROM node_performance ORDER BY timestamp DESC LIMIT 50;"
sqlite3 truth.db "SELECT * FROM peer_synchronization ORDER BY timestamp DESC LIMIT 50;"
```

### Clearing Logs

#### Via CLI Command

```bash
# Clear all logs
truthctl logs clear

# Clear specific log tables
truthctl logs clear --table discovery_nodes
truthctl logs clear --table discovery_history
truthctl logs clear --table sync_operations
truthctl logs clear --table sync_attempts
truthctl logs clear --table node_performance
truthctl logs clear --table peer_synchronization
```

#### Via SQLite (Direct Access)

```bash
# Clear all logs
sqlite3 truth.db "DELETE FROM discovery_nodes;"
sqlite3 truth.db "DELETE FROM discovery_history;"
sqlite3 truth.db "DELETE FROM sync_operations;"
sqlite3 truth.db "DELETE FROM sync_attempts;"
sqlite3 truth.db "DELETE FROM node_performance;"
sqlite3 truth.db "DELETE FROM peer_synchronization;"

# Or clear specific tables
sqlite3 truth.db "DELETE FROM discovery_nodes WHERE datetime(timestamp) < datetime('now', '-7 days');"
```

## System Information

In addition to log data, system information can be accessed through:

```bash
# View system status
truthctl status

# View node information
truthctl nodes list

# View peer connections
truthctl peers list
```

## Log Rotation and Management

Since logs are stored in SQLite, implement rotation strategies to prevent database growth:

1. Regularly clear old logs via CLI commands
2. Set up periodic cleanup (manual or scripted)

Example cleanup script:

```bash
#!/bin/bash
# Clear logs older than 30 days
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM discovery_nodes WHERE datetime(timestamp) < datetime('now', '-30 days');"
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM discovery_history WHERE datetime(timestamp) < datetime('now', '-30 days');"
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM sync_operations WHERE datetime(timestamp) < datetime('now', '-30 days');"
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM sync_attempts WHERE datetime(timestamp) < datetime('now', '-30 days');"
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM node_performance WHERE datetime(timestamp) < datetime('now', '-30 days');"
sqlite3 ~/.local/share/TruthTraining/truth_training.sqlite \
  "DELETE FROM peer_synchronization WHERE datetime(timestamp) < datetime('now', '-30 days');"
```

## Troubleshooting

### CLI: Logs Command Fails

1. Verify database file exists and is accessible
2. Check database exists and contains expected tables: `sqlite3 <db_path> ".tables"`
3. Ensure database is not locked by another process
4. Confirm `truthctl` is installed and in PATH

### Log Data Issues

1. Check that the Node Discovery system is running and generating logs
2. Verify database permissions allow read/write access
3. Ensure sufficient disk space for database growth

## Best Practices

1. **Regular Cleanup**: Clear old logs periodically to prevent database growth
2. **Monitoring**: Use `truthctl` commands to monitor node discovery and synchronization status
3. **Sensitive Data**: The system is designed to not store sensitive information in logs
4. **Performance**: Monitor database size and implement rotation strategies as needed

## System Logs for Development and Debugging

**Important Note**: The system logs described below are separate from the Node Discovery logging system and are primarily intended for development, debugging, and system administration purposes. The Node Discovery logs themselves are only accessible through the `truthctl` CLI application as described in the previous sections.

For development, debugging, and system administration purposes, additional system logs may be available:

### Desktop Application System Logs

Desktop applications may generate system-level logs depending on the platform:

**Linux:**
- Application logs may be available through the system journal: `journalctl -u <application-name>`
- Standard output/error streams can be captured when launching from terminal

**macOS:**
- Application logs may be found in Console.app
- Located at `~/Library/Logs/` for user-specific applications

**Windows:**
- Application logs may be accessible through Event Viewer
- Windows applications sometimes create log files in the installation directory

### Android System Logs

For Android applications, system logs can be accessed via:

#### Via ADB (Recommended)

```bash
# View all logs
adb logcat

# Filter by tag (e.g., application-specific tags)
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

## Related Documentation

- [CLI Usage](CLI_Usage.md) - CLI commands including `logs` subcommands
- [Troubleshooting](troubleshooting.md) - General troubleshooting guide
- [Node Discovery Architecture](android_discovery_architecture.md) - Technical details about node discovery

