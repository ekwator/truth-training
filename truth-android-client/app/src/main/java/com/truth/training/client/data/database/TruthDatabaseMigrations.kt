package com.truth.training.client.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations for Truth Training Android.
 * 
 * Migration 1->2: Added knowledge base entities (category, cause, develop, effect, forma, impact_type, progress_metrics)
 * and updated EventEntity to use truth_events table with embedded context fields.
 */
object TruthDatabaseMigrations {
    /**
     * Migration from version 1 to version 2.
     * 
     * Changes:
     * - Added knowledge base tables: category, cause, develop, effect, forma, impact_type
     * - Added progress_metrics table
     * - EventEntity now uses truth_events table (instead of events) with embedded context fields
     * - Added foreign keys from truth_events to knowledge base tables
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create knowledge base tables
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `category` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `cause` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `develop` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `effect` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `forma` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `impact_type` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `progress_metrics` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `event_id` INTEGER NOT NULL,
                    `metric_name` TEXT NOT NULL,
                    `metric_value` REAL NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    FOREIGN KEY(`event_id`) REFERENCES `truth_events`(`id`) ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create truth_events table (new structure with embedded context fields)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `truth_events` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `description` TEXT NOT NULL,
                    `category_id` INTEGER,
                    `forma_id` INTEGER,
                    `cause_id` INTEGER,
                    `develop_id` INTEGER,
                    `effect_id` INTEGER,
                    `vector` INTEGER NOT NULL,
                    `detected` INTEGER,
                    `corrected` INTEGER NOT NULL DEFAULT 0,
                    `timestamp_start` INTEGER NOT NULL,
                    `timestamp_end` INTEGER,
                    `code` INTEGER NOT NULL DEFAULT 1,
                    `collective_score` REAL,
                    FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`forma_id`) REFERENCES `forma`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`cause_id`) REFERENCES `cause`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`develop_id`) REFERENCES `develop`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`effect_id`) REFERENCES `effect`(`id`) ON DELETE SET NULL
                )
            """.trimIndent())
            
            // Create indices for truth_events
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_category_id` ON `truth_events`(`category_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_forma_id` ON `truth_events`(`forma_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_cause_id` ON `truth_events`(`cause_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_develop_id` ON `truth_events`(`develop_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_effect_id` ON `truth_events`(`effect_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_timestamp_start` ON `truth_events`(`timestamp_start`)")
            
            // Migrate data from old events table to truth_events if it exists
            // Note: This is a simplified migration - in production, you may need more complex data transformation
            database.execSQL("""
                INSERT OR IGNORE INTO `truth_events` (
                    `id`, `description`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`,
                    `vector`, `detected`, `corrected`, `timestamp_start`, `timestamp_end`, `code`, `collective_score`
                )
                SELECT 
                    CAST(`id` AS INTEGER),
                    COALESCE(`title`, '') || COALESCE(' ' || `description`, ''),
                    `category_id`,
                    `forma_id`,
                    `cause_id`,
                    `develop_id`,
                    `effect_id`,
                    0, -- default vector
                    NULL, -- detected
                    0, -- corrected
                    CAST(strftime('%s', COALESCE(`start_date`, `created_at`)) AS INTEGER),
                    CASE WHEN `end_date` IS NOT NULL THEN CAST(strftime('%s', `end_date`) AS INTEGER) ELSE NULL END,
                    1, -- default code
                    NULL -- collective_score
                FROM `events`
                WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='events')
            """.trimIndent())
        }
    }
}

