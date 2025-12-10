package com.truth.training.client.utils

/**
 * Emoji mapping system for UI elements.
 * Provides consistent emoji assignment for accessibility (constitutional requirement Rule 8).
 * All UI elements must include appropriate emojis for better understanding.
 * 
 * This utility is language-independent: same emoji is returned regardless of selected language (English/Russian).
 * UI components should combine emoji from this utility with localized text strings from Android resources.
 * 
 * Structure matches Desktop `ui/desktop/src/utils/emojiMapping.ts` exactly for cross-platform consistency.
 * 
 * @see com.truth.training.client.utils.EmojiMapping.getEmoji
 */
object EmojiMapping {
    
    /**
     * Screens category: Maps screen names to emoji characters for screen titles.
     */
    data class Screens(
        val dashboard: String = "🏠",
        val newEvent: String = "➕",
        val contextEditor: String = "📝",
        val events: String = "📋",
        val judgments: String = "⚖️",
        val overallSummary: String = "📊",
        val trainingResults: String = "📈",
        val settings: String = "⚙️"
    )

    /**
     * Actions category: Maps action names to emoji characters for action buttons.
     */
    data class Actions(
        val save: String = "💾",
        val cancel: String = "❌",
        val delete: String = "🗑️",
        val edit: String = "✏️",
        val create: String = "➕",
        val submit: String = "✅",
        val refresh: String = "🔄",
        val sync: String = "🔄",
        val back: String = "⬅️",
        val next: String = "➡️"
    )

    /**
     * Fields category: Maps form field names to emoji characters for field labels.
     */
    data class Fields(
        val name: String = "📝",
        val description: String = "📄",
        val category: String = "🏷️",
        val forma: String = "📐",
        val cause: String = "🔍",
        val develop: String = "📈",
        val effect: String = "💥",
        val startDate: String = "📅",
        val endDate: String = "📅",
        val assessment: String = "⚖️",
        val confidence: String = "📊",
        val reasoning: String = "💭"
    )

    /**
     * Status category: Maps status indicator names to emoji characters for status displays.
     */
    data class Status(
        val online: String = "🟢",
        val offline: String = "🔴",
        val syncing: String = "🔄",
        val error: String = "❌",
        val success: String = "✅",
        val warning: String = "⚠️"
    )

    /**
     * Navigation category: Maps navigation item names to emoji characters for navigation menus.
     */
    data class Navigation(
        val home: String = "🏠",
        val events: String = "📋",
        val judgments: String = "⚖️",
        val templates: String = "📝",
        val summary: String = "📊",
        val training: String = "📈",
        val settings: String = "⚙️"
    )

    /**
     * Default emoji mapping instances matching Desktop `emojiMapping.ts` exactly.
     * Emojis are semantically meaningful and directly related to function.
     * Same function = same emoji across the application (consistency requirement).
     */
    private val defaultScreens = Screens()
    private val defaultActions = Actions()
    private val defaultFields = Fields()
    private val defaultStatus = Status()
    private val defaultNavigation = Navigation()

    /**
     * Get emoji for a UI element.
     * Returns appropriate emoji based on element type and key.
     * 
     * This function is language-independent: returns the same emoji character regardless of 
     * selected language (English or Russian). UI components should combine the returned emoji 
     * with localized text strings from Android string resources.
     * 
     * @param category Category of UI element (screens, actions, fields, status, navigation)
     * @param key Key within the category (e.g., "dashboard", "save", "name")
     * @return Emoji string matching Desktop implementation, or empty string if not found
     * 
     * @example
     * ```kotlin
     * val saveEmoji = EmojiMapping.getEmoji("actions", "save") // Returns "💾"
     * val dashboardEmoji = EmojiMapping.getEmoji("screens", "dashboard") // Returns "🏠"
     * 
     * // In UI component, combine with localized text:
     * Text("${EmojiMapping.getEmoji("actions", "save")} ${context.getString(R.string.save)}")
     * // English: "💾 Save"
     * // Russian: "💾 Сохранить"
     * ```
     */
    fun getEmoji(category: String, key: String): String {
        if (key.isEmpty()) return ""
        
        return when (category) {
            "screens" -> {
                when (key) {
                    "dashboard" -> defaultScreens.dashboard
                    "newEvent" -> defaultScreens.newEvent
                    "contextEditor" -> defaultScreens.contextEditor
                    "events" -> defaultScreens.events
                    "judgments" -> defaultScreens.judgments
                    "overallSummary" -> defaultScreens.overallSummary
                    "trainingResults" -> defaultScreens.trainingResults
                    "settings" -> defaultScreens.settings
                    else -> ""
                }
            }
            "actions" -> {
                when (key) {
                    "save" -> defaultActions.save
                    "cancel" -> defaultActions.cancel
                    "delete" -> defaultActions.delete
                    "edit" -> defaultActions.edit
                    "create" -> defaultActions.create
                    "submit" -> defaultActions.submit
                    "refresh" -> defaultActions.refresh
                    "sync" -> defaultActions.sync
                    "back" -> defaultActions.back
                    "next" -> defaultActions.next
                    else -> ""
                }
            }
            "fields" -> {
                when (key) {
                    "name" -> defaultFields.name
                    "description" -> defaultFields.description
                    "category" -> defaultFields.category
                    "forma" -> defaultFields.forma
                    "cause" -> defaultFields.cause
                    "develop" -> defaultFields.develop
                    "effect" -> defaultFields.effect
                    "startDate" -> defaultFields.startDate
                    "endDate" -> defaultFields.endDate
                    "assessment" -> defaultFields.assessment
                    "confidence" -> defaultFields.confidence
                    "reasoning" -> defaultFields.reasoning
                    else -> ""
                }
            }
            "status" -> {
                when (key) {
                    "online" -> defaultStatus.online
                    "offline" -> defaultStatus.offline
                    "syncing" -> defaultStatus.syncing
                    "error" -> defaultStatus.error
                    "success" -> defaultStatus.success
                    "warning" -> defaultStatus.warning
                    else -> ""
                }
            }
            "navigation" -> {
                when (key) {
                    "home" -> defaultNavigation.home
                    "events" -> defaultNavigation.events
                    "judgments" -> defaultNavigation.judgments
                    "templates" -> defaultNavigation.templates
                    "summary" -> defaultNavigation.summary
                    "training" -> defaultNavigation.training
                    "settings" -> defaultNavigation.settings
                    else -> ""
                }
            }
            else -> ""
        }
    }
}

