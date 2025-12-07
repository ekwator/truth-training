# Quick Start: Android UI Registration and Launch Configuration

**Feature**: 013-goal-objective-properly  
**Date**: 2025-12-06  
**Phase**: 1 - Quick Start Guide

## Problem

Android app launches but shows blank/black screen instead of DashboardScreen.

## Root Cause

Navigation graph's start destination (`"events"`) is empty (placeholder only). DashboardScreen exists but is not registered in navigation graph.

## Solution Summary

1. Create ViewModelFactory for DashboardViewModel
2. Register DashboardScreen in MainNavigation.kt
3. Change start destination from `"events"` to `"dashboard"`

## Implementation Steps

### Step 1: Create ViewModelFactory

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/ViewModelFactory.kt`

```kotlin
package com.truth.training.client.ui.compose

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.truth.training.client.ui.DashboardViewModel
import com.truth.training.client.ui.compose.nodes.NodesViewModel

class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(application) as T
            }
            modelClass.isAssignableFrom(NodesViewModel::class.java) -> {
                NodesViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
```

### Step 1.5: Update DashboardViewModel (if needed)

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/DashboardViewModel.kt`

**Check if syncStatus is already exposed**. If not, add:

```kotlin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.truth.training.client.data.SyncStatus

// Add to DashboardViewModel class:
val syncStatus: StateFlow<SyncStatus> = repository.syncStatus.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SyncStatus.Unknown
)
```

**Note**: Verify if this already exists before adding. This exposes the repository's syncStatus Flow as a StateFlow for easy observation in Compose.

### Step 2: Update MainNavigation.kt

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/MainNavigation.kt`

**Changes**:

1. **Import ViewModelFactory and DashboardViewModel**:
```kotlin
import com.truth.training.client.ui.compose.ViewModelFactory
import com.truth.training.client.ui.DashboardViewModel
import com.truth.training.client.data.SyncStatus
```

2. **Change start destination** (line 36):
```kotlin
startDestination = "dashboard",  // Changed from "events"
```

3. **Add DashboardScreen route** (before or after "events" route):
```kotlin
composable("dashboard") {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    
    // Collect ViewModel state
    val info by viewModel.info.collectAsState()
    val stats by viewModel.stats.collectAsState()
    
    // IMPORTANT: Use syncStatus from ViewModel (after Step 1.5 update)
    // If syncStatus is exposed in DashboardViewModel:
    val syncStatus by viewModel.syncStatus.collectAsState()
    
    // Alternative (if ViewModel not updated in Step 1.5 - not recommended):
    // val repository = TruthRepository(context.applicationContext)
    // val syncStatus by remember { 
    //     flowOf(repository.getSyncStatus()).collectAsState(initial = SyncStatus.Unknown)
    // }
    
    // Extract event count from stats
    val eventCount = stats?.totalEvents ?: 0
    
    // Display DashboardScreen
    DashboardScreen(
        syncStatus = syncStatus,
        eventCount = eventCount,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToContexts = onNavigateToContexts,
        onNavigateToJudgments = onNavigateToJudgments,
        onSyncNow = { viewModel.refresh() }
    )
}
```

### Step 3: Verify AndroidManifest.xml

**File**: `truth-android-client/app/src/main/AndroidManifest.xml`

**Status**: ✅ Already correct - no changes needed
- MainActivity declared as launcher
- Intent filters correct
- `android:exported="true"` set

## Testing

### Manual Testing

1. Build and install APK on physical Android device
2. Launch app from launcher
3. **Expected**: DashboardScreen displays immediately (not blank screen)
4. **Verify**: 
   - DashboardScreen shows sync status
   - Event count displays
   - Navigation buttons work

### Instrumentation Test

**File**: `truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/MainActivityLaunchTest.kt`

Add test to verify DashboardScreen displays:

```kotlin
@Test
fun testDashboardScreenDisplaysOnLaunch() {
    val scenario = launchActivity<MainActivity>()
    
    // Verify DashboardScreen is displayed (not blank)
    onView(withText("Dashboard")).check(matches(isDisplayed()))
    onView(withText("Sync Status")).check(matches(isDisplayed()))
    
    scenario.close()
}
```

## Verification Checklist

- [ ] ViewModelFactory.kt created
- [ ] MainNavigation.kt updated with DashboardScreen route
- [ ] Start destination changed to "dashboard"
- [ ] SyncStatus structure verified and mapped correctly
- [ ] App builds without errors
- [ ] App launches and shows DashboardScreen (not blank screen)
- [ ] Navigation from DashboardScreen works
- [ ] Instrumentation tests pass
- [ ] Physical device testing completed (minimum 2 devices)

## Rollback Plan

If issues occur:
1. Revert MainNavigation.kt start destination to `"events"`
2. Remove DashboardScreen route registration
3. Keep ViewModelFactory (useful for future ViewModels)

## Next Steps

After implementation:
1. Run instrumentation tests
2. Test on physical devices (minimum 2)
3. Verify no blank screen on launch
4. Document any SyncStatus mapping adjustments needed

---

_Version: v1.0.0_

