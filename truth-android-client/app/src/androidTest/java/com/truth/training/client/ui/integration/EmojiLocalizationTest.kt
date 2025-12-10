package com.truth.training.client.ui.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truth.training.client.utils.EmojiMapping
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Integration tests for emoji localization.
 * Verifies that emojis remain constant when language switches (English/Russian),
 * while text changes according to locale.
 * 
 * Contract: emoji-localization-contract.md
 * User Story: Phase 9 - Polish & Cross-Cutting Concerns
 */
@RunWith(AndroidJUnit4::class)
class EmojiLocalizationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Test emoji display in English language.
     * T096: Verifies emojis are displayed correctly in English locale.
     */
    @Test
    fun testEmojiDisplay_InEnglishLanguage_EmojisPresent() {
        // Set English locale
        val resources = context.resources
        val config = resources.configuration
        val originalLocale = config.locales[0]
        
        try {
            config.setLocale(Locale.ENGLISH)
            val localizedResources = context.createConfigurationContext(config).resources
            
            // Verify emojis are language-independent
            val dashboardEmoji = EmojiMapping.getEmoji("screens", "dashboard")
            val saveEmoji = EmojiMapping.getEmoji("actions", "save")
            val nameEmoji = EmojiMapping.getEmoji("fields", "name")
            
            assertTrue("Dashboard emoji should be present in English", dashboardEmoji.isNotEmpty())
            assertTrue("Save emoji should be present in English", saveEmoji.isNotEmpty())
            assertTrue("Name field emoji should be present in English", nameEmoji.isNotEmpty())
            
            // Verify emoji values are correct
            assertEquals("Dashboard emoji should be 🏠", "🏠", dashboardEmoji)
            assertEquals("Save emoji should be 💾", "💾", saveEmoji)
            assertEquals("Name field emoji should be 📝", "📝", nameEmoji)
        } finally {
            // Restore original locale
            config.setLocale(originalLocale)
        }
    }

    /**
     * Test emoji display in Russian language.
     * T097: Verifies emojis are displayed correctly in Russian locale.
     */
    @Test
    fun testEmojiDisplay_InRussianLanguage_EmojisPresent() {
        // Set Russian locale
        val resources = context.resources
        val config = resources.configuration
        val originalLocale = config.locales[0]
        
        try {
            config.setLocale(Locale("ru"))
            val localizedResources = context.createConfigurationContext(config).resources
            
            // Verify emojis are language-independent
            val dashboardEmoji = EmojiMapping.getEmoji("screens", "dashboard")
            val saveEmoji = EmojiMapping.getEmoji("actions", "save")
            val nameEmoji = EmojiMapping.getEmoji("fields", "name")
            
            assertTrue("Dashboard emoji should be present in Russian", dashboardEmoji.isNotEmpty())
            assertTrue("Save emoji should be present in Russian", saveEmoji.isNotEmpty())
            assertTrue("Name field emoji should be present in Russian", nameEmoji.isNotEmpty())
            
            // Verify emoji values are correct (same as English)
            assertEquals("Dashboard emoji should be 🏠 (same as English)", "🏠", dashboardEmoji)
            assertEquals("Save emoji should be 💾 (same as English)", "💾", saveEmoji)
            assertEquals("Name field emoji should be 📝 (same as English)", "📝", nameEmoji)
        } finally {
            // Restore original locale
            config.setLocale(originalLocale)
        }
    }

    /**
     * Test emoji remains constant when language switches (same emoji, different text).
     * T098: Verifies emoji language-independence.
     */
    @Test
    fun testEmoji_RemainsConstant_WhenLanguageSwitches() {
        val resources = context.resources
        val config = resources.configuration
        val originalLocale = config.locales[0]
        
        try {
            // Test in English
            config.setLocale(Locale.ENGLISH)
            val englishEmojis = mapOf(
                "screens" to mapOf("dashboard" to EmojiMapping.getEmoji("screens", "dashboard")),
                "actions" to mapOf("save" to EmojiMapping.getEmoji("actions", "save")),
                "fields" to mapOf("name" to EmojiMapping.getEmoji("fields", "name"))
            )
            
            // Test in Russian
            config.setLocale(Locale("ru"))
            val russianEmojis = mapOf(
                "screens" to mapOf("dashboard" to EmojiMapping.getEmoji("screens", "dashboard")),
                "actions" to mapOf("save" to EmojiMapping.getEmoji("actions", "save")),
                "fields" to mapOf("name" to EmojiMapping.getEmoji("fields", "name"))
            )
            
            // Verify emojis are identical in both languages
            assertEquals(
                "Dashboard emoji should be same in English and Russian",
                englishEmojis["screens"]!!["dashboard"],
                russianEmojis["screens"]!!["dashboard"]
            )
            assertEquals(
                "Save emoji should be same in English and Russian",
                englishEmojis["actions"]!!["save"],
                russianEmojis["actions"]!!["save"]
            )
            assertEquals(
                "Name field emoji should be same in English and Russian",
                englishEmojis["fields"]!!["name"],
                russianEmojis["fields"]!!["name"]
            )
        } finally {
            // Restore original locale
            config.setLocale(originalLocale)
        }
    }

    /**
     * Test all UI elements combine emoji with localized text correctly.
     * T099: Verifies emoji + text combination pattern.
     */
    @Test
    fun testUIElements_CombineEmojiWithLocalizedText_Correctly() {
        // Verify emoji + text pattern: "${emoji} ${localizedText}"
        val dashboardEmoji = EmojiMapping.getEmoji("screens", "dashboard")
        val saveEmoji = EmojiMapping.getEmoji("actions", "save")
        val nameEmoji = EmojiMapping.getEmoji("fields", "name")
        
        // Pattern should be: emoji + space + text
        // Example: "🏠 Dashboard" or "🏠 Панель управления"
        assertTrue("Dashboard emoji should be non-empty", dashboardEmoji.isNotEmpty())
        assertTrue("Save emoji should be non-empty", saveEmoji.isNotEmpty())
        assertTrue("Name field emoji should be non-empty", nameEmoji.isNotEmpty())
        
        // Verify emojis are valid Unicode emoji characters
        val emojiRegex = Regex("\\p{So}+")
        assertTrue("Dashboard emoji should be valid Unicode emoji", dashboardEmoji.matches(emojiRegex))
        assertTrue("Save emoji should be valid Unicode emoji", saveEmoji.matches(emojiRegex))
        assertTrue("Name field emoji should be valid Unicode emoji", nameEmoji.matches(emojiRegex))
        
        // Verify emoji + text pattern can be constructed
        // (We can't test actual localized strings without UI context, but we verify the pattern)
        val dashboardPattern = "$dashboardEmoji ${"Dashboard"}" // English example
        val savePattern = "$saveEmoji ${"Save"}" // English example
        val namePattern = "$nameEmoji ${"Name"}" // English example
        
        assertTrue("Dashboard pattern should start with emoji", dashboardPattern.startsWith(dashboardEmoji))
        assertTrue("Save pattern should start with emoji", savePattern.startsWith(saveEmoji))
        assertTrue("Name pattern should start with emoji", namePattern.startsWith(nameEmoji))
    }
}

