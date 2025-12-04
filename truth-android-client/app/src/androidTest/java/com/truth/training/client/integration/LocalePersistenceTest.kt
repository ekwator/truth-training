package com.truth.training.client.integration

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for locale persistence (User Story 4).
 * 
 * Note: Android app is currently EN-only, so this test verifies
 * that no locale switching exists (as expected).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LocalePersistenceTest {
    
    private val context: Context = ApplicationProvider.getApplicationContext()
    
    /**
     * Verifies that Android app is EN-only (no locale switching).
     */
    @Test
    fun testEnOnlyNoLocaleSwitching() {
        // Check SharedPreferences for locale setting
        val prefs: SharedPreferences = context.getSharedPreferences(
            "truth_training_prefs",
            Context.MODE_PRIVATE
        )
        
        // Verify no locale preference exists (EN-only)
        val locale = prefs.getString("locale", null)
        
        // Since app is EN-only, locale should be null or "en"
        assertTrue(
            "Android app is EN-only, locale preference should be null or 'en'",
            locale == null || locale == "en"
        )
    }
}

