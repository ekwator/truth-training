package com.truth.training.client.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for SchemaLoader utility class.
 * 
 * Tests schema loading from assets and SQL execution.
 * 
 * Note: These are instrumented tests (require Android context) because SchemaLoader
 * needs to read from assets, which requires Android Context.
 */
@RunWith(AndroidJUnit4::class)
class SchemaLoaderTest {
    
    private lateinit var context: Context
    private lateinit var database: TruthDatabase
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            TruthDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testValidateSchemaAssetExists() {
        // Verify schema.sql asset exists
        val isValid = SchemaLoader.validateSchemaAsset(context)
        assertTrue("schema.sql asset should exist", isValid)
    }
    
    @Test
    fun testLoadAndExecuteSchema() {
        // Load and execute schema
        SchemaLoader.loadAndExecuteSchema(context, database.openHelper.writableDatabase)
        
        // Verify canonical tables were created
        val tables = mutableListOf<String>()
        database.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
        }
        
        // Verify key canonical tables exist
        assertTrue("truth_events table should exist", tables.contains("truth_events"))
        assertTrue("category table should exist", tables.contains("category"))
        assertTrue("context table should exist", tables.contains("context"))
        assertTrue("impact table should exist", tables.contains("impact"))
    }
    
    @Test
    fun testSchemaExecutionIsIdempotent() {
        // Execute schema twice
        SchemaLoader.loadAndExecuteSchema(context, database.openHelper.writableDatabase)
        SchemaLoader.loadAndExecuteSchema(context, database.openHelper.writableDatabase)
        
        // Should not throw exception (idempotent)
        val tables = mutableListOf<String>()
        database.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
        }
        
        // Verify tables still exist after second execution
        assertTrue("truth_events table should still exist after second execution", 
            tables.contains("truth_events"))
    }
    
    @Test
    fun testSchemaContainsCanonicalTables() {
        // Load and execute schema
        SchemaLoader.loadAndExecuteSchema(context, database.openHelper.writableDatabase)
        
        // Verify canonical tables exist
        val requiredTables = listOf(
            "truth_events",
            "category",
            "cause",
            "develop",
            "effect",
            "forma",
            "impact_type",
            "context",
            "impact",
            "progress_metrics",
            "nodes",
            "statements"
        )
        
        val existingTables = mutableListOf<String>()
        database.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) {
                existingTables.add(cursor.getString(0))
            }
        }
        
        // Verify all required tables exist
        requiredTables.forEach { tableName ->
            assertTrue("Required table '$tableName' should exist", 
                existingTables.contains(tableName))
        }
    }
}

