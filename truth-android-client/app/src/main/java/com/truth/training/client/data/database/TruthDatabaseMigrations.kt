package com.truth.training.client.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TruthDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            createKnowledgeBaseTables(database)
            createContextTable(database)
            recreateTruthEvents(database)
            recreateImpactTable(database)
            ensureLegacyRelations(database)
            createProgressMetricsTable(database)
        }

        private fun createKnowledgeBaseTables(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `category` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cause` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `quality` INTEGER NOT NULL,
                    `description` TEXT
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `develop` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `quality` INTEGER NOT NULL,
                    `description` TEXT
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `effect` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `quality` INTEGER NOT NULL,
                    `description` TEXT
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `forma` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `quality` INTEGER NOT NULL,
                    `description` TEXT
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `impact_type` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `description` TEXT
                )
                """.trimIndent()
            )
        }

        private fun createContextTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `context` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `name` TEXT NOT NULL,
                    `category_id` INTEGER,
                    `forma_id` INTEGER,
                    `cause_id` INTEGER,
                    `develop_id` INTEGER,
                    `effect_id` INTEGER,
                    `description` TEXT,
                    FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`forma_id`) REFERENCES `forma`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`cause_id`) REFERENCES `cause`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`develop_id`) REFERENCES `develop`(`id`) ON DELETE SET NULL,
                    FOREIGN KEY(`effect_id`) REFERENCES `effect`(`id`) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_context_name` ON `context`(`name`)")
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_context_category_id_forma_id_cause_id_develop_id_effect_id`
                ON `context`(`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`)
                """.trimIndent()
            )

            if (tableExists(database, "context_templates")) {
                database.execSQL(
                    """
                    INSERT INTO `context` (`name`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`, `description`)
                    SELECT `name`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`, `description`
                    FROM `context_templates`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE IF EXISTS `context_templates`")
            }
        }

        private fun recreateTruthEvents(database: SupportSQLiteDatabase) {
            if (tableExists(database, "truth_events")) {
                database.execSQL("ALTER TABLE `truth_events` RENAME TO `truth_events_legacy`")
            }

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `truth_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
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
                """.trimIndent()
            )

            when {
                tableExists(database, "truth_events_legacy") -> {
                    database.execSQL(
                        """
                        INSERT INTO `truth_events` (
                            `id`, `description`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`,
                            `vector`, `detected`, `corrected`, `timestamp_start`, `timestamp_end`, `code`, `collective_score`
                        )
                        SELECT 
                            CASE WHEN TRIM(`id`) GLOB '-?[0-9]*' THEN CAST(`id` AS INTEGER) ELSE NULL END,
                            `description`,
                            `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`,
                            CASE WHEN `vector` IN (1, '1', 'true', 'TRUE') THEN 1 ELSE 0 END,
                            CASE WHEN `detected` IS NULL THEN NULL ELSE CASE WHEN `detected` IN (1, '1', 'true', 'TRUE') THEN 1 ELSE 0 END END,
                            CASE WHEN `corrected` IN (1, '1', 'true', 'TRUE') THEN 1 ELSE 0 END,
                            `timestamp_start`,
                            `timestamp_end`,
                            `code`,
                            `collective_score`
                        FROM `truth_events_legacy`
                        """.trimIndent()
                    )
                    database.execSQL("DROP TABLE IF EXISTS `truth_events_legacy`")
                }
                tableExists(database, "events") -> {
                    database.execSQL(
                        """
                        INSERT INTO `truth_events` (
                            `description`, `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`,
                            `vector`, `detected`, `corrected`, `timestamp_start`, `timestamp_end`, `code`, `collective_score`
                        )
                        SELECT 
                            COALESCE(`description`, `title`, ''),
                            `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`,
                            1,
                            NULL,
                            CASE WHEN `status` = 'corrected' THEN 1 ELSE 0 END,
                            CASE
                                WHEN `start_date` IS NOT NULL THEN CAST(strftime('%s', `start_date`) AS INTEGER)
                                WHEN `created_at` IS NOT NULL THEN CAST(strftime('%s', `created_at`) AS INTEGER)
                                ELSE CAST(strftime('%s', 'now') AS INTEGER)
                            END,
                            CASE WHEN `end_date` IS NOT NULL THEN CAST(strftime('%s', `end_date`) AS INTEGER) ELSE NULL END,
                            1,
                            NULL
                        FROM `events`
                        """.trimIndent()
                    )
                }
            }

            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_category_id` ON `truth_events`(`category_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_forma_id` ON `truth_events`(`forma_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_cause_id` ON `truth_events`(`cause_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_develop_id` ON `truth_events`(`develop_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_effect_id` ON `truth_events`(`effect_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_truth_events_timestamp_start` ON `truth_events`(`timestamp_start`)")
        }

        private fun recreateImpactTable(database: SupportSQLiteDatabase) {
            if (tableExists(database, "impact")) {
                database.execSQL("ALTER TABLE `impact` RENAME TO `impact_legacy`")
            }

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `impact` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `event_id` INTEGER NOT NULL,
                    `type_id` INTEGER NOT NULL,
                    `value` INTEGER NOT NULL,
                    `notes` TEXT,
                    `created_at` INTEGER NOT NULL,
                    FOREIGN KEY(`event_id`) REFERENCES `truth_events`(`id`) ON DELETE CASCADE,
                    FOREIGN KEY(`type_id`) REFERENCES `impact_type`(`id`) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            if (tableExists(database, "impact_legacy")) {
                database.execSQL(
                    """
                    INSERT INTO `impact` (`id`, `event_id`, `type_id`, `value`, `notes`, `created_at`)
                    SELECT 
                        `id`,
                        CASE WHEN TRIM(`event_id`) GLOB '-?[0-9]*' THEN CAST(`event_id` AS INTEGER) ELSE NULL END,
                        `type_id`,
                        `value`,
                        `notes`,
                        `created_at`
                    FROM `impact_legacy`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE IF EXISTS `impact_legacy`")
            }

            database.execSQL("CREATE INDEX IF NOT EXISTS `index_impact_event_id` ON `impact`(`event_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_impact_type_id` ON `impact`(`type_id`)")
        }

        private fun ensureLegacyRelations(database: SupportSQLiteDatabase) {
            if (tableExists(database, "judgments")) {
                database.execSQL("ALTER TABLE `judgments` RENAME TO `judgments_legacy`")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `judgments` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `event_id` INTEGER NOT NULL,
                        `assessment` TEXT NOT NULL,
                        `confidence_level` REAL NOT NULL,
                        `reasoning` TEXT,
                        `submitted_at` TEXT NOT NULL,
                        FOREIGN KEY(`event_id`) REFERENCES `truth_events`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO `judgments` (`id`, `event_id`, `assessment`, `confidence_level`, `reasoning`, `submitted_at`)
                    SELECT 
                        `id`,
                        CASE WHEN TRIM(`event_id`) GLOB '-?[0-9]*' THEN CAST(`event_id` AS INTEGER) ELSE NULL END,
                        `assessment`,
                        `confidence_level`,
                        `reasoning`,
                        `submitted_at`
                    FROM `judgments_legacy`
                    """.trimIndent()
                )

                database.execSQL("DROP TABLE IF EXISTS `judgments_legacy`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_judgments_event_id` ON `judgments`(`event_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_judgments_submitted_at` ON `judgments`(`submitted_at`)")
            }

            if (tableExists(database, "summaries")) {
                database.execSQL("ALTER TABLE `summaries` RENAME TO `summaries_legacy`")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `summaries` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `event_id` INTEGER NOT NULL,
                        `summary_text` TEXT,
                        `recommendations` TEXT,
                        `updated_at` TEXT NOT NULL,
                        FOREIGN KEY(`event_id`) REFERENCES `truth_events`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO `summaries` (`id`, `event_id`, `summary_text`, `recommendations`, `updated_at`)
                    SELECT 
                        `id`,
                        CASE WHEN TRIM(`event_id`) GLOB '-?[0-9]*' THEN CAST(`event_id` AS INTEGER) ELSE NULL END,
                        `summary_text`,
                        `recommendations`,
                        `updated_at`
                    FROM `summaries_legacy`
                    """.trimIndent()
                )

                database.execSQL("DROP TABLE IF EXISTS `summaries_legacy`")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_summaries_event_id` ON `summaries`(`event_id`)")
            }
        }

        private fun createProgressMetricsTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `progress_metrics` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `timestamp` INTEGER NOT NULL,
                    `total_events` INTEGER NOT NULL,
                    `total_events_group` INTEGER NOT NULL,
                    `total_positive_impact` REAL NOT NULL,
                    `total_positive_impact_group` REAL NOT NULL,
                    `total_negative_impact` REAL NOT NULL,
                    `total_negative_impact_group` REAL NOT NULL,
                    `trend` REAL NOT NULL,
                    `trend_group` REAL NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}

private fun tableExists(database: SupportSQLiteDatabase, tableName: String): Boolean {
    database.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use {
        return it.moveToFirst()
    }
}

