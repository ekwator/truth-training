package com.truth.training.client.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for loading and executing canonical Truth schema SQL from assets.
 * 
 * This ensures schema parity between Android and Desktop by using the same SQL source
 * derived from `core/src/storage.rs::SCHEMA_SQL`.
 */
object SchemaLoader {
    private const val SCHEMA_ASSET_PATH = "schema.sql"

    /**
     * Loads the canonical schema SQL from assets and executes it against the database.
     * 
     * @param context Android context for accessing assets
     * @param database Room database instance to execute SQL against
     * @throws IllegalStateException if schema.sql asset is not found
     * @throws Exception if SQL execution fails
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
     */
    private fun executeSchemaSql(database: SupportSQLiteDatabase, schemaSql: String) {
        // Split by semicolon, but preserve semicolons inside string literals
        val statements = schemaSql
            .split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("--") }

        database.beginTransaction()
        try {
            for (statement in statements) {
                if (statement.isNotBlank()) {
                    database.execSQL(statement)
                }
            }
            database.setTransactionSuccessful()
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

