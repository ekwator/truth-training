package com.truth.training.client.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Utility class for managing application locale.
 * Provides methods to set, get, and apply locale changes.
 */
object LocaleHelper {
    /**
     * Sets the application locale to the specified language code.
     * 
     * @param context Application context
     * @param locale Language code ("en" for English, "ru" for Russian)
     * @return Context with updated locale configuration
     */
    fun setLocale(context: Context, locale: String): Context {
        val localeObj = when (locale) {
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale.getDefault()
        }
        
        return updateConfiguration(context, localeObj)
    }
    
    /**
     * Gets the current application locale.
     * 
     * @param context Application context
     * @return Current locale code ("en" or "ru")
     */
    fun getLocale(context: Context): String {
        val config = context.resources.configuration
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
        
        return locale.language
    }
    
    /**
     * Updates the configuration with the specified locale.
     * 
     * @param context Application context
     * @param locale Locale to apply
     * @return Context with updated configuration
     */
    private fun updateConfiguration(context: Context, locale: Locale): Context {
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            // createConfigurationContext creates a new context with the updated configuration
            // This is the correct way to apply locale in Android N+
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
}

