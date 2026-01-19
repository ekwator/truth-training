# Database Migration Documentation

## Overview
This document provides comprehensive details about the database migration process for the Truth Training application, including schema version tracking mechanisms, migration procedures, compatibility considerations, and rollback strategies.

## Schema Version Tracking
Both databases "truth_training.sqlite" and "discovery_nodes.sqlite" for the desktop application include "schema_version" tables for migration tracking. These tables are essential for managing database evolution and ensuring compatibility between different versions of the application.

### Schema Version Table Structure
```sql
CREATE TABLE schema_version (
    version TEXT PRIMARY KEY,
    applied_at INTEGER NOT NULL,
    description TEXT
);
```

## Migration Process
Database migrations in Truth Training follow a structured approach to ensure data integrity and compatibility. The migration process is handled automatically during database initialization through the `run_migrations` function in `core/src/storage.rs`.

### Migration Execution Flow
1. Check current schema version
2. Apply incremental migrations if needed
3. Update schema version tracking
4. Validate migrated schema

### Migration Types
The system handles several types of migrations:

#### Column Modifications
Migrations may add, remove, or modify columns in existing tables to support new features, improve data consistency, or restructure data. Examples include:
- Adding `global_id`, `signature`, `participant_id`, and `collective_score` to the `truth_events` table
- Adding `signature` and `public_key` fields to various tables for cryptographic verification
- Removing deprecated columns and transferring or aggregating their information to new columns

#### Table Creations
New tables may be created to support additional functionality.

#### Table Renaming
Tables can be renamed to provide additional functionality for clarity.

Before:
- Nodes management (`nodes`)
- Node discovery (`node_discovery`)
- Synchronization logs (`sync_log`, `sync_logs`)
- Node metrics (`node_metrics`)
- Peer history (`peer_history`)

After:
- `nodes` → `discovery_nodes` - Contains information about network discovery nodes for peer-to-peer connectivity
- `node_discovery` → `discovery_history` - Tracks changes in network node discovery for auditing and analyzing
- `sync_log` → `sync_operations` - Records details of synchronization operations between nodes
- `sync_logs` → `sync_attempts` - Logs all synchronization attempts including success/failure status
- `node_metrics` → `node_performance` - Tracks performance and health metrics of individual nodes in the network
- `peer_history` → `peer_synchronization` - Maintains historical record of peer synchronization activities

#### Index Updates
Performance-related migrations may add indexes to optimize query performance.

## Migration Procedures

### Checking for Required Migrations
The system checks if migrations are needed by:
1. Examining existing table structures
2. Comparing with the expected schema
3. Identifying missing columns or tables

### Migration Safety
Each migration follows safety protocols:
- Backups are recommended before major migrations
- Migrations are designed to be idempotent where possible
- Validation occurs after each migration step

## Compatibility Considerations

### Backward Compatibility
Backward compatibility is not automatically maintained as tables and fields may be removed during migrations. To ensure data preservation:
- Users must create manual backups of their databases before migrations
- Migration procedures may involve removal of deprecated tables and fields
- Database reservation copies should be created following the backup procedures described in the documentation ([see backup instructions in Deployment guide](Deployment.md) and [Android-specific backup procedures](quickstart_android.md))
- In case of issues, restore from the latest backup and consult the migration documentation

### Forward Compatibility
The system prepares for future migrations by:
- Using flexible data types where appropriate
- Implementing proper indexing strategies
- Maintaining clear schema version tracking

## Rollback Strategies

### Automated Rollbacks
The system does not implement automatic rollbacks. Instead, it focuses on:
- Thorough validation before applying migrations
- Maintaining data integrity throughout the process
- Providing clear error messages if migrations fail

### Manual Rollback Procedures
In case of migration failure:
1. Stop the application immediately
2. Restore from the latest backup if available
3. Contact system administrators for assistance

## Specific Migration Examples

### v1.0.0 Migration
The v1.0.0 migration introduced several key changes:
- Added schema version tracking
- Enhanced truth_events table with cryptographic fields
- Improved synchronization capabilities

### v1.1.0 Migration - Table Renaming
The v1.1.0 migration included renaming several tables for clarity and better organization:
- `nodes` → `discovery_nodes`
- `node_discovery` → `discovery_history`
- `node_metrics` → `node_performance`
- `sync_log` → `sync_operations`
- `sync_logs` → `sync_attempts`
- `peer_history` → `peer_synchronization`

These renames were part of an effort to make the database schema more intuitive and descriptive of each table's purpose in the system.

### Column Addition Logic
The migration system uses helper functions to safely add columns:

```rust
fn has_column(conn: &Connection, table: &str, column: &str) -> Result<bool, rusqlite::Error> {
    let mut stmt = conn.prepare(&format!("PRAGMA table_info('{}')", table))?;
    let mut rows = stmt.query([])?;
    while let Some(row) = rows.next()? {
        let name: String = row.get(1)?; // 1 = name
        if name == column {
            return Ok(true);
        }
    }
    Ok(false)
}
```

This ensures that columns are only added if they don't already exist.

## Best Practices

### For Developers
- Always test migrations on copies of production data
- Follow the established migration patterns
- Include proper error handling in migration code
- Update documentation when adding new migrations

### For Administrators
- Maintain regular database backups before updates
- Monitor migration logs for errors
- Plan maintenance windows for major migrations
- Verify data integrity after migrations complete

## Troubleshooting

### Common Migration Issues
- Insufficient disk space for migration operations
- Permission issues accessing database files
- Network interruptions during distributed migrations

### Error Recovery
Most migration errors are recoverable by restoring from backup and addressing the underlying issue before retrying.

## Future Migration Planning

The migration system is designed to accommodate future schema changes while maintaining the core principles of the Truth Training application. Planned enhancements include:
- More sophisticated migration dependency management
- Improved monitoring and alerting for migration processes