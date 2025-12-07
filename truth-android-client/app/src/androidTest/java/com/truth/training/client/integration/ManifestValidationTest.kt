package com.truth.training.client.integration

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for User Story 2: Verify AndroidManifest.xml configuration.
 * Tests MainActivity launcher declaration, intent filters, and exported flag.
 * 
 * Task: T007
 */
@RunWith(AndroidJUnit4::class)
class ManifestValidationTest {
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val packageManager = context.packageManager
    
    @Test
    fun testMainActivityIsLauncher() {
        // Get MainActivity intent
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        
        assertNotNull("Launch intent should not be null", intent)
        assertEquals(
            "MainActivity should be the launcher activity",
            "com.truth.training.client.MainActivity",
            intent?.component?.className
        )
    }
    
    @Test
    fun testMainActivityHasMainIntentFilter() {
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        
        assertNotNull("Launch intent should not be null", intent)
        assertTrue(
            "MainActivity should have MAIN action",
            intent?.action == android.content.Intent.ACTION_MAIN
        )
        assertTrue(
            "MainActivity should have LAUNCHER category",
            intent?.categories?.contains(android.content.Intent.CATEGORY_LAUNCHER) == true
        )
    }
    
    @Test
    fun testMainActivityIsExported() {
        val componentName = android.content.ComponentName(
            context.packageName,
            "com.truth.training.client.MainActivity"
        )
        
        val activityInfo = try {
            packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            fail("MainActivity not found in manifest: ${e.message}")
            return
        }
        
        assertTrue(
            "MainActivity should be exported (required for Android 12+)",
            activityInfo.exported
        )
    }
    
    @Test
    fun testMainActivityExistsInManifest() {
        val componentName = android.content.ComponentName(
            context.packageName,
            "com.truth.training.client.MainActivity"
        )
        
        try {
            val activityInfo = packageManager.getActivityInfo(componentName, 0)
            assertNotNull("MainActivity should exist in manifest", activityInfo)
            assertEquals(
                "MainActivity name should match",
                "com.truth.training.client.MainActivity",
                activityInfo.name
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("MainActivity not found in manifest: ${e.message}")
        }
    }
}

