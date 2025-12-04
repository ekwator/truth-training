package com.truth.training.client.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test for legacy tables (User Story 2).
 * 
 * Verifies that legacy tables are absent after database initialization.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TruthDatabaseSchemaTest {
    
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
     * Regression test: Verifies legacy tables are absent.
     * 
     * This test should always pass - if it fails, legacy tables have been reintroduced.
     */
    @Test
    fun testLegacyTablesAbsent() {
        val db = database.openHelper.readableDatabase
        
        // Query sqlite_master for legacy table names
        val foundLegacyTables = mutableListOf<String>()
        
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN (?, ?, ?, ?)",
            arrayOf("events", "impacts", "summaries", "logs")
        ).use { cursor ->
            while (cursor.moveToNext()) {
                foundLegacyTables.add(cursor.getString(0))
            }
        }
        
        // Assert no legacy tables found
        assertEquals(
            "Legacy tables should be absent after initialization. Found: ${foundLegacyTables.joinToString(", ")}",
            0,
            foundLegacyTables.size
        )
    }
    
    /**
     * Verifies canonical tables exist.
     */
    @Test
    fun testCanonicalTablesExist() {
        val db = database.openHelper.readableDatabase
        
        TestDatabaseHelper.assertCanonicalTablesExist(db)
    }
}

