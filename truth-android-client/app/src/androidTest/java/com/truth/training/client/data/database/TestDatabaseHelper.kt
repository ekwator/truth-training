package com.truth.training.client.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider

/**
 * Test harness for Room database testing with utilities for:
 * - In-memory database creation
 * - Schema assertions
 * - Legacy table seeding for regression tests
 * - Migration testing
 */
object TestDatabaseHelper {
    private const val TEST_DB_NAME = "test_truth_training.db"

    /**
     * Creates an in-memory Room database for testing.
     * Does not include migrations - use for testing schema creation directly.
     */
    fun createInMemoryDatabase(): TruthDatabase {
        return Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    /**
     * Creates a test database with migrations applied.
     * Useful for testing migration paths.
     */
    fun createTestDatabase(context: Context, name: String = TEST_DB_NAME): TruthDatabase {
        return Room.databaseBuilder(
            context,
            TruthDatabase::class.java,
            name
        )
            .allowMainThreadQueries()
            .build()
    }

    /**
     * Asserts that required canonical tables exist in the database.
     * 
     * @param database Database to check
     * @param requiredTables List of table names that must exist
     * @throws AssertionError if any required table is missing
     */
    fun assertCanonicalTablesExist(
        database: SupportSQLiteDatabase,
        requiredTables: List<String> = listOf(
            "truth_events",
            "impact",
            "progress_metrics",
            "context",
            "category",
            "cause",
            "develop",
            "effect",
            "forma",
            "impact_type",
            "nodes",
            "statements"
        )
    ) {
        val existingTables = getTableNames(database)
        val missingTables = requiredTables.filter { it !in existingTables }
        
        if (missingTables.isNotEmpty()) {
            throw AssertionError(
                "Missing required canonical tables: ${missingTables.joinToString(", ")}\n" +
                "Existing tables: ${existingTables.joinToString(", ")}"
            )
        }
    }

    /**
     * Asserts that legacy tables are absent from the database.
     * 
     * @param database Database to check
     * @param legacyTables List of legacy table names that must NOT exist
     * @throws AssertionError if any legacy table is found
     */
    fun assertLegacyTablesAbsent(
        database: SupportSQLiteDatabase,
        legacyTables: List<String> = listOf("events", "impacts", "summaries", "logs")
    ) {
        val existingTables = getTableNames(database)
        val foundLegacyTables = legacyTables.filter { it in existingTables }
        
        if (foundLegacyTables.isNotEmpty()) {
            throw AssertionError(
                "Found legacy tables that should be removed: ${foundLegacyTables.joinToString(", ")}\n" +
                "All existing tables: ${existingTables.joinToString(", ")}"
            )
        }
    }

    /**
     * Seeds legacy tables in the database for regression testing.
     * Creates tables that should be dropped during migration.
     */
    fun seedLegacyTables(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY,
                title TEXT,
                description TEXT,
                category_id INTEGER,
                created_at INTEGER
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS impacts (
                id INTEGER PRIMARY KEY,
                event_id INTEGER,
                type_id INTEGER,
                value INTEGER
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS summaries (
                id TEXT PRIMARY KEY,
                event_id INTEGER,
                summary_text TEXT
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS logs (
                id INTEGER PRIMARY KEY,
                message TEXT,
                timestamp INTEGER
            )
        """.trimIndent())
    }

    /**
     * Gets list of all table names in the database.
     */
    fun getTableNames(database: SupportSQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        database.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
            while (cursor.moveToNext()) {
                val tableName = cursor.getString(0)
                tables.add(tableName)
            }
        }
        return tables
    }

    /**
     * Creates a MigrationTestHelper for testing Room migrations.
     * 
     * Note: Requires androidx.room:room-testing dependency.
     * For now, migrations are tested via direct database operations.
     */
    // fun createMigrationTestHelper(): MigrationTestHelper {
    //     return MigrationTestHelper(
    //         InstrumentationRegistry.getInstrumentation(),
    //         TruthDatabase::class.java
    //     )
    // }

    /**
     * Drops all tables in the database (useful for cleanup between tests).
     */
    fun dropAllTables(database: SupportSQLiteDatabase) {
        val tables = getTableNames(database)
        database.execSQL("PRAGMA foreign_keys = OFF")
        for (table in tables) {
            database.execSQL("DROP TABLE IF EXISTS `$table`")
        }
        database.execSQL("PRAGMA foreign_keys = ON")
    }
}

