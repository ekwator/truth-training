package com.truth.training.client.ui.integration

import com.truth.training.client.utils.EmojiMapping
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Integration tests for EmojiMapping utility.
 * Verifies that all UI components can access EmojiMapping utility correctly.
 * 
 * Contract: emoji-mapping-contract.md
 * User Story: User Story 2 (Priority: P1)
 */
@RunWith(AndroidJUnit4::class)
class EmojiMappingIntegrationTest {

    /**
     * Test all UI components can access EmojiMapping utility.
     * Verifies that EmojiMapping.getEmoji() can be called from UI context.
     */
    @Test
    fun testEmojiMapping_CanBeAccessed_FromUIComponents() {
        // Verify utility is accessible
        assertNotNull("EmojiMapping object should not be null", EmojiMapping)
        
        // Verify getEmoji function exists and works
        val dashboardEmoji = EmojiMapping.getEmoji("screens", "dashboard")
        assertNotNull("Dashboard emoji should not be null", dashboardEmoji)
        assertTrue("Dashboard emoji should not be empty", dashboardEmoji.isNotEmpty())
        
        // Verify all categories are accessible
        val screensEmoji = EmojiMapping.getEmoji("screens", "dashboard")
        val actionsEmoji = EmojiMapping.getEmoji("actions", "save")
        val fieldsEmoji = EmojiMapping.getEmoji("fields", "name")
        val statusEmoji = EmojiMapping.getEmoji("status", "online")
        val navigationEmoji = EmojiMapping.getEmoji("navigation", "home")
        
        assertTrue("Screens emoji should be accessible", screensEmoji.isNotEmpty())
        assertTrue("Actions emoji should be accessible", actionsEmoji.isNotEmpty())
        assertTrue("Fields emoji should be accessible", fieldsEmoji.isNotEmpty())
        assertTrue("Status emoji should be accessible", statusEmoji.isNotEmpty())
        assertTrue("Navigation emoji should be accessible", navigationEmoji.isNotEmpty())
    }

    /**
     * Verify EmojiMapping structure matches Desktop categories and key names exactly.
     * This ensures 100% parity with Desktop emojiMapping.ts.
     */
    @Test
    fun testEmojiMapping_StructureMatchesDesktop_Exactly() {
        // Verify all Desktop categories exist
        val desktopCategories = listOf("screens", "actions", "fields", "status", "navigation")
        for (category in desktopCategories) {
            // Verify category is accessible by checking a known key
            val testKey = when (category) {
                "screens" -> "dashboard"
                "actions" -> "save"
                "fields" -> "name"
                "status" -> "online"
                "navigation" -> "home"
                else -> ""
            }
            val emoji = EmojiMapping.getEmoji(category, testKey)
            assertTrue("Category '$category' should be accessible and return emoji", emoji.isNotEmpty())
        }
        
        // Verify all Desktop screen keys exist
        val desktopScreenKeys = listOf("dashboard", "newEvent", "contextEditor", "events", 
            "judgments", "overallSummary", "trainingResults", "settings")
        for (key in desktopScreenKeys) {
            val emoji = EmojiMapping.getEmoji("screens", key)
            assertTrue("Screen key '$key' should exist", emoji.isNotEmpty())
        }
        
        // Verify all Desktop action keys exist
        val desktopActionKeys = listOf("save", "cancel", "delete", "edit", "create", "submit", 
            "refresh", "sync", "back", "next")
        for (key in desktopActionKeys) {
            val emoji = EmojiMapping.getEmoji("actions", key)
            assertTrue("Action key '$key' should exist", emoji.isNotEmpty())
        }
        
        // Verify all Desktop field keys exist
        val desktopFieldKeys = listOf("name", "description", "category", "forma", "cause", "develop", 
            "effect", "startDate", "endDate", "assessment", "confidence", "reasoning")
        for (key in desktopFieldKeys) {
            val emoji = EmojiMapping.getEmoji("fields", key)
            assertTrue("Field key '$key' should exist", emoji.isNotEmpty())
        }
        
        // Verify all Desktop status keys exist
        val desktopStatusKeys = listOf("online", "offline", "syncing", "error", "success", "warning")
        for (key in desktopStatusKeys) {
            val emoji = EmojiMapping.getEmoji("status", key)
            assertTrue("Status key '$key' should exist", emoji.isNotEmpty())
        }
        
        // Verify all Desktop navigation keys exist
        val desktopNavigationKeys = listOf("home", "events", "judgments", "templates", 
            "summary", "training", "settings")
        for (key in desktopNavigationKeys) {
            val emoji = EmojiMapping.getEmoji("navigation", key)
            assertTrue("Navigation key '$key' should exist", emoji.isNotEmpty())
        }
    }
}

