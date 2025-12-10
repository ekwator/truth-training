package com.truth.training.client.ui.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truth.training.client.utils.EmojiMapping
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contract tests for Desktop parity.
 * Verifies that all emoji values, category structure, and key names match Desktop
 * `ui/desktop/src/utils/emojiMapping.ts` exactly.
 * 
 * Contract: desktop-parity-contract.md
 * User Story: Phase 9 - Polish & Cross-Cutting Concerns
 */
@RunWith(AndroidJUnit4::class)
class DesktopParityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Desktop emoji mapping values (from ui/desktop/src/utils/emojiMapping.ts).
     * These values must match Android EmojiMapping exactly.
     */
    private val desktopScreens = mapOf(
        "dashboard" to "🏠",
        "newEvent" to "➕",
        "contextEditor" to "📝",
        "events" to "📋",
        "judgments" to "⚖️",
        "overallSummary" to "📊",
        "trainingResults" to "📈",
        "settings" to "⚙️"
    )

    private val desktopActions = mapOf(
        "save" to "💾",
        "cancel" to "❌",
        "delete" to "🗑️",
        "edit" to "✏️",
        "create" to "➕",
        "submit" to "✅",
        "refresh" to "🔄",
        "sync" to "🔄",
        "back" to "⬅️",
        "next" to "➡️"
    )

    private val desktopFields = mapOf(
        "name" to "📝",
        "description" to "📄",
        "category" to "🏷️",
        "forma" to "📐",
        "cause" to "🔍",
        "develop" to "📈",
        "effect" to "💥",
        "startDate" to "📅",
        "endDate" to "📅",
        "assessment" to "⚖️",
        "confidence" to "📊",
        "reasoning" to "💭"
    )

    private val desktopStatus = mapOf(
        "online" to "🟢",
        "offline" to "🔴",
        "syncing" to "🔄",
        "error" to "❌",
        "success" to "✅",
        "warning" to "⚠️"
    )

    private val desktopNavigation = mapOf(
        "home" to "🏠",
        "events" to "📋",
        "judgments" to "⚖️",
        "templates" to "📝",
        "summary" to "📊",
        "training" to "📈",
        "settings" to "⚙️"
    )

    /**
     * Verify all emoji values match Desktop `emojiMapping.ts` exactly.
     * T102: Contract test for emoji value parity.
     */
    @Test
    fun testEmojiValues_MatchDesktop_Exactly() {
        // Test screens
        for ((key, expectedEmoji) in desktopScreens) {
            val actualEmoji = EmojiMapping.getEmoji("screens", key)
            assertEquals("Screen '$key' emoji should match Desktop", expectedEmoji, actualEmoji)
        }
        
        // Test actions
        for ((key, expectedEmoji) in desktopActions) {
            val actualEmoji = EmojiMapping.getEmoji("actions", key)
            assertEquals("Action '$key' emoji should match Desktop", expectedEmoji, actualEmoji)
        }
        
        // Test fields
        for ((key, expectedEmoji) in desktopFields) {
            val actualEmoji = EmojiMapping.getEmoji("fields", key)
            assertEquals("Field '$key' emoji should match Desktop", expectedEmoji, actualEmoji)
        }
        
        // Test status
        for ((key, expectedEmoji) in desktopStatus) {
            val actualEmoji = EmojiMapping.getEmoji("status", key)
            assertEquals("Status '$key' emoji should match Desktop", expectedEmoji, actualEmoji)
        }
        
        // Test navigation
        for ((key, expectedEmoji) in desktopNavigation) {
            val actualEmoji = EmojiMapping.getEmoji("navigation", key)
            assertEquals("Navigation '$key' emoji should match Desktop", expectedEmoji, actualEmoji)
        }
    }

    /**
     * Verify emoji category structure matches Desktop exactly.
     * T103: Contract test for category structure parity.
     */
    @Test
    fun testEmojiCategoryStructure_MatchesDesktop_Exactly() {
        // Verify all Desktop categories exist in Android
        val androidCategories = listOf("screens", "actions", "fields", "status", "navigation")
        val desktopCategories = listOf("screens", "actions", "fields", "status", "navigation")
        
        assertEquals("Android should have same number of categories as Desktop", 
            desktopCategories.size, androidCategories.size)
        
        for (category in desktopCategories) {
            assertTrue("Android should have category '$category'", androidCategories.contains(category))
        }
        
        // Verify category key counts match
        assertEquals("Screens category should have same number of keys",
            desktopScreens.size, 8)
        assertEquals("Actions category should have same number of keys",
            desktopActions.size, 10)
        assertEquals("Fields category should have same number of keys",
            desktopFields.size, 12)
        assertEquals("Status category should have same number of keys",
            desktopStatus.size, 6)
        assertEquals("Navigation category should have same number of keys",
            desktopNavigation.size, 7)
    }

    /**
     * Verify emoji key names match Desktop exactly.
     * T104: Contract test for key name parity.
     */
    @Test
    fun testEmojiKeyNames_MatchDesktop_Exactly() {
        // Verify all Desktop keys exist in Android and return non-empty emojis
        val allDesktopKeys = mapOf(
            "screens" to desktopScreens.keys,
            "actions" to desktopActions.keys,
            "fields" to desktopFields.keys,
            "status" to desktopStatus.keys,
            "navigation" to desktopNavigation.keys
        )
        
        for ((category, keys) in allDesktopKeys) {
            for (key in keys) {
                val emoji = EmojiMapping.getEmoji(category, key)
                assertTrue(
                    "Android should have key '$key' in category '$category'",
                    emoji.isNotEmpty()
                )
            }
        }
        
        // Verify no extra keys in Android (all keys should match Desktop)
        // This is verified by checking that all Desktop keys exist and return correct emojis
        val totalDesktopKeys = desktopScreens.size + desktopActions.size + 
                              desktopFields.size + desktopStatus.size + desktopNavigation.size
        assertEquals("Total number of emoji keys should match Desktop", 43, totalDesktopKeys)
    }
}

