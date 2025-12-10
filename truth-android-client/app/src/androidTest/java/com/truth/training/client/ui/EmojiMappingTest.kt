package com.truth.training.client.ui

import com.truth.training.client.utils.EmojiMapping
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Unit tests for EmojiMapping utility.
 * Verifies emoji mapping functionality matches Desktop implementation.
 * 
 * Contract: emoji-mapping-contract.md
 * User Story: User Story 2 (Priority: P1)
 */
@RunWith(AndroidJUnit4::class)
class EmojiMappingTest {

    /**
     * TC-001: Valid category and key returns correct emoji
     * Test getEmoji() function returns correct emoji for valid category/key pairs
     */
    @Test
    fun testGetEmoji_ValidCategoryAndKey_ReturnsCorrectEmoji() {
        // Screens category
        assertEquals("🏠", EmojiMapping.getEmoji("screens", "dashboard"))
        assertEquals("➕", EmojiMapping.getEmoji("screens", "newEvent"))
        assertEquals("📝", EmojiMapping.getEmoji("screens", "contextEditor"))
        assertEquals("📋", EmojiMapping.getEmoji("screens", "events"))
        assertEquals("⚖️", EmojiMapping.getEmoji("screens", "judgments"))
        assertEquals("📊", EmojiMapping.getEmoji("screens", "overallSummary"))
        assertEquals("📈", EmojiMapping.getEmoji("screens", "trainingResults"))
        assertEquals("⚙️", EmojiMapping.getEmoji("screens", "settings"))

        // Actions category
        assertEquals("💾", EmojiMapping.getEmoji("actions", "save"))
        assertEquals("❌", EmojiMapping.getEmoji("actions", "cancel"))
        assertEquals("🗑️", EmojiMapping.getEmoji("actions", "delete"))
        assertEquals("✏️", EmojiMapping.getEmoji("actions", "edit"))
        assertEquals("➕", EmojiMapping.getEmoji("actions", "create"))
        assertEquals("✅", EmojiMapping.getEmoji("actions", "submit"))
        assertEquals("🔄", EmojiMapping.getEmoji("actions", "refresh"))
        assertEquals("🔄", EmojiMapping.getEmoji("actions", "sync"))
        assertEquals("⬅️", EmojiMapping.getEmoji("actions", "back"))
        assertEquals("➡️", EmojiMapping.getEmoji("actions", "next"))

        // Fields category
        assertEquals("📝", EmojiMapping.getEmoji("fields", "name"))
        assertEquals("📄", EmojiMapping.getEmoji("fields", "description"))
        assertEquals("🏷️", EmojiMapping.getEmoji("fields", "category"))
        assertEquals("📐", EmojiMapping.getEmoji("fields", "forma"))
        assertEquals("🔍", EmojiMapping.getEmoji("fields", "cause"))
        assertEquals("📈", EmojiMapping.getEmoji("fields", "develop"))
        assertEquals("💥", EmojiMapping.getEmoji("fields", "effect"))
        assertEquals("📅", EmojiMapping.getEmoji("fields", "startDate"))
        assertEquals("📅", EmojiMapping.getEmoji("fields", "endDate"))
        assertEquals("⚖️", EmojiMapping.getEmoji("fields", "assessment"))
        assertEquals("📊", EmojiMapping.getEmoji("fields", "confidence"))
        assertEquals("💭", EmojiMapping.getEmoji("fields", "reasoning"))

        // Status category
        assertEquals("🟢", EmojiMapping.getEmoji("status", "online"))
        assertEquals("🔴", EmojiMapping.getEmoji("status", "offline"))
        assertEquals("🔄", EmojiMapping.getEmoji("status", "syncing"))
        assertEquals("❌", EmojiMapping.getEmoji("status", "error"))
        assertEquals("✅", EmojiMapping.getEmoji("status", "success"))
        assertEquals("⚠️", EmojiMapping.getEmoji("status", "warning"))

        // Navigation category
        assertEquals("🏠", EmojiMapping.getEmoji("navigation", "home"))
        assertEquals("📋", EmojiMapping.getEmoji("navigation", "events"))
        assertEquals("⚖️", EmojiMapping.getEmoji("navigation", "judgments"))
        assertEquals("📝", EmojiMapping.getEmoji("navigation", "templates"))
        assertEquals("📊", EmojiMapping.getEmoji("navigation", "summary"))
        assertEquals("📈", EmojiMapping.getEmoji("navigation", "training"))
        assertEquals("⚙️", EmojiMapping.getEmoji("navigation", "settings"))
    }

    /**
     * TC-002: Test structure matches Desktop emojiMapping.ts
     * Verify all categories and keys are present matching Desktop structure
     */
    @Test
    fun testStructure_MatchesDesktop_AllCategoriesAndKeysPresent() {
        // Verify all categories exist
        val categories = listOf("screens", "actions", "fields", "status", "navigation")
        for (category in categories) {
            // Verify category is accessible by checking a known key
            val testKey = when (category) {
                "screens" -> "dashboard"
                "actions" -> "save"
                "fields" -> "name"
                "status" -> "online"
                "navigation" -> "home"
                else -> ""
            }
            assertTrue("Category '$category' should be accessible", 
                EmojiMapping.getEmoji(category, testKey).isNotEmpty())
        }

        // Verify all screen keys
        val screenKeys = listOf("dashboard", "newEvent", "contextEditor", "events", 
            "judgments", "overallSummary", "trainingResults", "settings")
        for (key in screenKeys) {
            assertTrue("Screen key '$key' should exist", 
                EmojiMapping.getEmoji("screens", key).isNotEmpty())
        }

        // Verify all action keys
        val actionKeys = listOf("save", "cancel", "delete", "edit", "create", "submit", 
            "refresh", "sync", "back", "next")
        for (key in actionKeys) {
            assertTrue("Action key '$key' should exist", 
                EmojiMapping.getEmoji("actions", key).isNotEmpty())
        }

        // Verify all field keys
        val fieldKeys = listOf("name", "description", "category", "forma", "cause", "develop", 
            "effect", "startDate", "endDate", "assessment", "confidence", "reasoning")
        for (key in fieldKeys) {
            assertTrue("Field key '$key' should exist", 
                EmojiMapping.getEmoji("fields", key).isNotEmpty())
        }

        // Verify all status keys
        val statusKeys = listOf("online", "offline", "syncing", "error", "success", "warning")
        for (key in statusKeys) {
            assertTrue("Status key '$key' should exist", 
                EmojiMapping.getEmoji("status", key).isNotEmpty())
        }

        // Verify all navigation keys
        val navigationKeys = listOf("home", "events", "judgments", "templates", 
            "summary", "training", "settings")
        for (key in navigationKeys) {
            assertTrue("Navigation key '$key' should exist", 
                EmojiMapping.getEmoji("navigation", key).isNotEmpty())
        }
    }

    /**
     * TC-003: Invalid category returns empty string
     * Test graceful degradation for invalid inputs
     */
    @Test
    fun testGetEmoji_InvalidCategory_ReturnsEmptyString() {
        assertEquals("", EmojiMapping.getEmoji("invalid", "dashboard"))
        assertEquals("", EmojiMapping.getEmoji("", "dashboard"))
        assertEquals("", EmojiMapping.getEmoji("nonexistent", "save"))
    }

    /**
     * TC-004: Invalid key returns empty string
     * Test graceful degradation for invalid keys
     */
    @Test
    fun testGetEmoji_InvalidKey_ReturnsEmptyString() {
        assertEquals("", EmojiMapping.getEmoji("screens", "invalid"))
        assertEquals("", EmojiMapping.getEmoji("actions", ""))
        assertEquals("", EmojiMapping.getEmoji("fields", "nonexistent"))
        assertEquals("", EmojiMapping.getEmoji("status", "invalid"))
        assertEquals("", EmojiMapping.getEmoji("navigation", "missing"))
    }

    /**
     * TC-005: All Desktop emojis present and matching
     * Verify all emoji values match Desktop emojiMapping.ts exactly
     * This is a contract test ensuring 100% parity
     */
    @Test
    fun testDesktopParity_AllEmojiValuesMatch_Exactly() {
        // Desktop mapping reference values
        val desktopScreens = mapOf(
            "dashboard" to "🏠",
            "newEvent" to "➕",
            "contextEditor" to "📝",
            "events" to "📋",
            "judgments" to "⚖️",
            "overallSummary" to "📊",
            "trainingResults" to "📈",
            "settings" to "⚙️"
        )

        val desktopActions = mapOf(
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

        val desktopFields = mapOf(
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

        val desktopStatus = mapOf(
            "online" to "🟢",
            "offline" to "🔴",
            "syncing" to "🔄",
            "error" to "❌",
            "success" to "✅",
            "warning" to "⚠️"
        )

        val desktopNavigation = mapOf(
            "home" to "🏠",
            "events" to "📋",
            "judgments" to "⚖️",
            "templates" to "📝",
            "summary" to "📊",
            "training" to "📈",
            "settings" to "⚙️"
        )

        // Verify screens match Desktop
        for ((key, expectedEmoji) in desktopScreens) {
            val actualEmoji = EmojiMapping.getEmoji("screens", key)
            assertEquals("Screen '$key' emoji must match Desktop exactly", expectedEmoji, actualEmoji)
        }

        // Verify actions match Desktop
        for ((key, expectedEmoji) in desktopActions) {
            val actualEmoji = EmojiMapping.getEmoji("actions", key)
            assertEquals("Action '$key' emoji must match Desktop exactly", expectedEmoji, actualEmoji)
        }

        // Verify fields match Desktop
        for ((key, expectedEmoji) in desktopFields) {
            val actualEmoji = EmojiMapping.getEmoji("fields", key)
            assertEquals("Field '$key' emoji must match Desktop exactly", expectedEmoji, actualEmoji)
        }

        // Verify status match Desktop
        for ((key, expectedEmoji) in desktopStatus) {
            val actualEmoji = EmojiMapping.getEmoji("status", key)
            assertEquals("Status '$key' emoji must match Desktop exactly", expectedEmoji, actualEmoji)
        }

        // Verify navigation match Desktop
        for ((key, expectedEmoji) in desktopNavigation) {
            val actualEmoji = EmojiMapping.getEmoji("navigation", key)
            assertEquals("Navigation '$key' emoji must match Desktop exactly", expectedEmoji, actualEmoji)
        }
    }

    /**
     * Test language independence: same emoji returned regardless of language context
     */
    @Test
    fun testLanguageIndependence_ReturnsSameEmoji_RegardlessOfLanguage() {
        // Emoji should be constant regardless of any locale/language settings
        val emoji1 = EmojiMapping.getEmoji("screens", "dashboard")
        val emoji2 = EmojiMapping.getEmoji("screens", "dashboard")
        
        // Should always return same emoji (language-independent)
        assertEquals("🏠", emoji1)
        assertEquals("🏠", emoji2)
        assertEquals(emoji1, emoji2)
        
        // Verify other emojis are also constant
        assertEquals("💾", EmojiMapping.getEmoji("actions", "save"))
        assertEquals("📝", EmojiMapping.getEmoji("fields", "name"))
    }
}

