package com.truth.training.client.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.truth.training.client.data.database.TestDatabaseHelper
import com.truth.training.client.data.database.TruthDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for schema parity (User Story 2).
 * 
 * Verifies that Android database schema matches Desktop SQLite schema structure.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SchemaParityTest {
    
    private lateinit var context: Context
    private lateinit var database: TruthDatabase
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = TestDatabaseHelper.createInMemoryDatabase()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    /**
     * Verifies database schema matches Desktop SQLite schema structure.
     */
    @Test
    fun testSchemaParity() {
        val db = database.openHelper.readableDatabase
        
        // Verify all canonical tables exist
        val requiredTables = listOf(
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
        
        TestDatabaseHelper.assertCanonicalTablesExist(db, requiredTables)
        
        // Verify legacy tables are absent
        TestDatabaseHelper.assertLegacyTablesAbsent(db)
    }
    
    /**
     * Verifies truth_events table structure matches canonical schema.
     */
    @Test
    fun testTruthEventsTableStructure() {
        val db = database.openHelper.readableDatabase
        
        // Query table info
        db.query("PRAGMA table_info(truth_events)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(1) // name column
                columns.add(columnName)
            }
            
            // Verify required columns exist
            assertTrue("truth_events should have id column", columns.contains("id"))
            assertTrue("truth_events should have description column", columns.contains("description"))
            assertTrue("truth_events should have category_id column", columns.contains("category_id"))
            assertTrue("truth_events should have forma_id column", columns.contains("forma_id"))
            assertTrue("truth_events should have cause_id column", columns.contains("cause_id"))
            assertTrue("truth_events should have develop_id column", columns.contains("develop_id"))
            assertTrue("truth_events should have effect_id column", columns.contains("effect_id"))
        }
    }
}

