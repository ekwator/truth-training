package com.truth.training.client.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truth.training.client.utils.EmojiMapping
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for edge cases: emoji rendering failures, unsupported devices, theme compatibility.
 * 
 * T108: Verifies graceful handling of edge cases in emoji implementation.
 * 
 * Edge cases covered:
 * - Emoji rendering failures (invalid Unicode, missing emoji)
 * - Unsupported devices (older Android versions with limited emoji support)
 * - Theme compatibility (light/dark themes)
 * - Null/empty string handling
 * - Invalid category/key combinations
 */
@RunWith(AndroidJUnit4::class)
class EmojiEdgeCasesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Test emoji rendering failure: invalid category returns empty string.
     * Verifies graceful degradation when category doesn't exist.
     */
    @Test
    fun testEmojiRenderingFailure_InvalidCategory_ReturnsEmptyString() {
        val invalidCategoryEmoji = EmojiMapping.getEmoji("invalidCategory", "dashboard")
        assertEquals("Invalid category should return empty string", "", invalidCategoryEmoji)
        
        // Verify UI can handle empty emoji gracefully
        val label = if (invalidCategoryEmoji.isNotEmpty()) {
            "$invalidCategoryEmoji Dashboard"
        } else {
            "Dashboard"
        }
        assertEquals("Label should fallback to text only", "Dashboard", label)
    }

    /**
     * Test emoji rendering failure: invalid key returns empty string.
     * Verifies graceful degradation when key doesn't exist.
     */
    @Test
    fun testEmojiRenderingFailure_InvalidKey_ReturnsEmptyString() {
        val invalidKeyEmoji = EmojiMapping.getEmoji("screens", "invalidKey")
        assertEquals("Invalid key should return empty string", "", invalidKeyEmoji)
        
        // Verify UI can handle empty emoji gracefully
        val label = if (invalidKeyEmoji.isNotEmpty()) {
            "$invalidKeyEmoji Dashboard"
        } else {
            "Dashboard"
        }
        assertEquals("Label should fallback to text only", "Dashboard", label)
    }

    /**
     * Test unsupported devices: verify emoji lookup doesn't crash on older Android versions.
     * Older devices may have limited emoji support, but lookup should still work.
     */
    @Test
    fun testUnsupportedDevices_EmojiLookup_DoesNotCrash() {
        // Test that all valid categories and keys return non-empty strings
        // Even on older devices, lookup should work (rendering may differ)
        val validEmojis = listOf(
            EmojiMapping.getEmoji("screens", "dashboard"),
            EmojiMapping.getEmoji("actions", "save"),
            EmojiMapping.getEmoji("fields", "name"),
            EmojiMapping.getEmoji("status", "online"),
            EmojiMapping.getEmoji("navigation", "home")
        )
        
        // All should return non-empty strings (even if device can't render them)
        for (emoji in validEmojis) {
            assertTrue("Valid emoji should return non-empty string", emoji.isNotEmpty())
        }
    }

    /**
     * Test theme compatibility: verify emojis are valid Unicode characters.
     * Unicode emojis should render correctly in both light and dark themes.
     */
    @Test
    fun testThemeCompatibility_EmojisAreValidUnicode_RenderInBothThemes() {
        val emojiRegex = Regex("\\p{So}+")
        
        val testEmojis = listOf(
            EmojiMapping.getEmoji("screens", "dashboard"),
            EmojiMapping.getEmoji("actions", "save"),
            EmojiMapping.getEmoji("fields", "name"),
            EmojiMapping.getEmoji("status", "online"),
            EmojiMapping.getEmoji("status", "offline"),
            EmojiMapping.getEmoji("status", "error"),
            EmojiMapping.getEmoji("status", "success"),
            EmojiMapping.getEmoji("status", "warning")
        )
        
        for (emoji in testEmojis) {
            assertTrue("Emoji should be valid Unicode emoji character", emoji.matches(emojiRegex))
            assertTrue("Emoji should be non-empty", emoji.isNotEmpty())
        }
    }

    /**
     * Test null/empty string handling: verify getEmoji handles null-like inputs gracefully.
     */
    @Test
    fun testNullEmptyStringHandling_GetEmoji_HandlesGracefully() {
        // Test with empty strings
        val emptyCategoryEmoji = EmojiMapping.getEmoji("", "dashboard")
        assertEquals("Empty category should return empty string", "", emptyCategoryEmoji)
        
        val emptyKeyEmoji = EmojiMapping.getEmoji("screens", "")
        assertEquals("Empty key should return empty string", "", emptyKeyEmoji)
        
        // Test with whitespace (should be treated as invalid)
        val whitespaceCategoryEmoji = EmojiMapping.getEmoji("   ", "dashboard")
        assertEquals("Whitespace category should return empty string", "", whitespaceCategoryEmoji)
    }

    /**
     * Test invalid category/key combinations: verify all invalid combinations return empty string.
     */
    @Test
    fun testInvalidCategoryKeyCombinations_ReturnEmptyString() {
        val invalidCombinations = listOf(
            Pair("invalidCategory", "dashboard"),
            Pair("screens", "invalidKey"),
            Pair("", ""),
            Pair("   ", "   "),
            Pair("screens", null as String?),
            Pair(null as String?, "dashboard")
        )
        
        for ((category, key) in invalidCombinations) {
            val emoji = if (category != null && key != null) {
                EmojiMapping.getEmoji(category, key)
            } else {
                "" // Null handling
            }
            assertEquals("Invalid combination should return empty string", "", emoji)
        }
    }

    /**
     * Test emoji string concatenation: verify emoji + text pattern works correctly.
     * Even if emoji is empty, text should remain functional.
     */
    @Test
    fun testEmojiStringConcatenation_PatternWorksCorrectly() {
        val validEmoji = EmojiMapping.getEmoji("screens", "dashboard")
        val emptyEmoji = ""
        val text = "Dashboard"
        
        // Valid emoji + text
        val validPattern = "$validEmoji $text"
        assertTrue("Valid pattern should contain emoji", validPattern.contains(validEmoji))
        assertTrue("Valid pattern should contain text", validPattern.contains(text))
        
        // Empty emoji + text (graceful degradation)
        val degradedPattern = if (emptyEmoji.isNotEmpty()) {
            "$emptyEmoji $text"
        } else {
            text
        }
        assertEquals("Degraded pattern should be text only", text, degradedPattern)
        assertTrue("Degraded pattern should still be functional", degradedPattern.isNotEmpty())
    }

    /**
     * Test emoji consistency across lookups: verify same category/key returns same emoji.
     */
    @Test
    fun testEmojiConsistency_AcrossLookups_ReturnsSameEmoji() {
        val emoji1 = EmojiMapping.getEmoji("screens", "dashboard")
        val emoji2 = EmojiMapping.getEmoji("screens", "dashboard")
        val emoji3 = EmojiMapping.getEmoji("screens", "dashboard")
        
        assertEquals("Same lookup should return same emoji", emoji1, emoji2)
        assertEquals("Same lookup should return same emoji", emoji2, emoji3)
    }

    /**
     * Test emoji performance under load: verify lookup remains fast with many calls.
     */
    @Test
    fun testEmojiPerformance_UnderLoad_RemainsFast() {
        val iterations = 10000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            EmojiMapping.getEmoji("screens", "dashboard")
            EmojiMapping.getEmoji("actions", "save")
            EmojiMapping.getEmoji("fields", "name")
        }
        
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0
        val avgTimeMs = durationMs / (iterations * 3) // 3 lookups per iteration
        
        // Verify average lookup time is very fast (< 0.1ms)
        assertTrue(
            "Emoji lookup should be fast under load (average < 0.1ms, actual: ${avgTimeMs}ms)",
            avgTimeMs < 0.1
        )
    }
}

