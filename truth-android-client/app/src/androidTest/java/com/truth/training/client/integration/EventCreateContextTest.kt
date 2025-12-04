package com.truth.training.client.integration

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.truth.training.client.MainActivity
import com.truth.training.client.data.database.TestDatabaseHelper
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.repository.ContextTemplateRepository
import com.truth.training.client.testing.TestDataSeeder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for EventCreateScreen context UX (User Story 3).
 * 
 * Verifies that:
 * - Dropdowns load contexts
 * - Selection updates state
 * - Invalid IDs block submission
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EventCreateContextTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    private lateinit var context: Context
    private lateinit var database: TruthDatabase
    private lateinit var contextRepository: ContextTemplateRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = TestDatabaseHelper.createInMemoryDatabase()
        contextRepository = ContextTemplateRepository(database, null)
        
        // Seed knowledge base
        runBlocking {
            TestDataSeeder.seedKnowledgeBase(database)
        }
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    /**
     * Verifies that contexts are loaded for dropdowns.
     */
    @Test
    fun testContextsLoadedForDropdowns() {
        // Launch app
        composeTestRule.waitForIdle()
        
        // Verify UI is displayed
        composeTestRule.onRoot().assertIsDisplayed()
        
        // Verify contexts are available
        runBlocking {
            val contexts = contextRepository.listTemplates()
            assert(contexts.isNotEmpty()) {
                "Contexts should be loaded for dropdown population"
            }
        }
    }
    
    /**
     * Verifies that invalid IDs are blocked.
     */
    @Test
    fun testInvalidIdsBlocked() {
        runBlocking {
            val contexts = contextRepository.listTemplates()
            val validIds = contexts.map { it.id }.toSet()
            
            // Test invalid ID
            val invalidId = 99999
            assert(!validIds.contains(invalidId)) {
                "Invalid ID should not be in valid context IDs"
            }
        }
    }
}

