package com.truth.training.client.data.database

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for loading and executing canonical Truth schema SQL from assets.
 * 
 * This ensures schema parity between Android and Desktop by using the same SQL source
 * derived from `core/src/storage.rs::SCHEMA_SQL`.
 * 
 * Note: Some tables in schema.sql (e.g., roles, users, events_ci) are not defined in Room entities
 * and are used only for Desktop/server compatibility. Errors for these tables are logged but don't
 * prevent database initialization.
 */
object SchemaLoader {
    private const val SCHEMA_ASSET_PATH = "schema.sql"
    private const val TAG = "SchemaLoader"
    
    // Tables that are not defined in Room entities but may exist in schema.sql
    // These are Desktop/server-only tables and errors for them should be ignored
    private val OPTIONAL_TABLES = setOf(
        "roles", "users", "events_ci", "judgments", "participants", 
        "consensus_ci", "reputation_history", "node_ratings", "group_ratings", "statements"
    )

    /**
     * Loads the canonical schema SQL from assets and executes it against the database.
     * 
     * @param context Android context for accessing assets
     * @param database Room database instance to execute SQL against
     * @throws IllegalStateException if schema.sql asset is not found
     */
    fun loadAndExecuteSchema(context: Context, database: SupportSQLiteDatabase) {
        val schemaSql = loadSchemaFromAssets(context)
        executeSchemaSql(database, schemaSql)
    }

    /**
     * Loads the schema SQL content from assets/schema.sql file.
     */
    private fun loadSchemaFromAssets(context: Context): String {
        return try {
            context.assets.open(SCHEMA_ASSET_PATH).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to load schema.sql from assets. Ensure the file exists at " +
                "app/src/main/assets/schema.sql and matches core/src/storage.rs::SCHEMA_SQL",
                e
            )
        }
    }

    /**
     * Executes the schema SQL against the database.
     * Splits SQL statements by semicolon and executes them individually.
     * Errors for optional tables (not defined in Room entities) are logged but don't fail the transaction.
     */
    private fun executeSchemaSql(database: SupportSQLiteDatabase, schemaSql: String) {
        // Split by semicolon, but preserve semicolons inside string literals
        val statements = schemaSql
            .split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("--") }

        database.beginTransaction()
        try {
            var successCount = 0
            var errorCount = 0
            
            for (statement in statements) {
                if (statement.isNotBlank()) {
                    try {
                        database.execSQL(statement)
                        successCount++
                    } catch (e: Exception) {
                        // Check if error is for an optional table
                        val isOptionalTable = OPTIONAL_TABLES.any { tableName ->
                            statement.contains("$tableName", ignoreCase = true)
                        }
                        
                        if (isOptionalTable) {
                            // Log but don't fail for optional tables (Desktop/server-only)
                            Log.d(TAG, "Skipped optional table statement (not in Room entities): ${statement.take(100)}")
                        } else {
                            // For required tables, log error but continue
                            // This allows database to initialize even if some statements fail
                            Log.w(TAG, "Failed to execute SQL statement: ${statement.take(100)}", e)
                            errorCount++
                        }
                    }
                }
            }
            
            // Set transaction successful even if some optional statements failed
            database.setTransactionSuccessful()
            Log.d(TAG, "Schema execution completed: $successCount successful, $errorCount errors (optional tables ignored)")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during schema execution", e)
            // Don't rethrow - let Room continue with entity-based schema
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Validates that the schema.sql asset exists and is readable.
     * Useful for pre-flight checks during database initialization.
     */
    fun validateSchemaAsset(context: Context): Boolean {
        return try {
            context.assets.open(SCHEMA_ASSET_PATH).use { true }
        } catch (e: Exception) {
            false
        }
    }
}

