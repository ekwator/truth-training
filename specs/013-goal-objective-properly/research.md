# Research: Android UI Registration and Launch Configuration

**Feature**: 013-goal-objective-properly  
**Date**: 2025-12-06  
**Phase**: 0 - Research and Discovery

## Problem Statement

The Truth Training Android application launches but displays a blank/black screen instead of the expected DashboardScreen. Investigation reveals:

1. **AndroidManifest.xml**: ✅ Correctly configured
   - MainActivity declared as launcher with proper intent filters
   - `android:exported="true"` set correctly
   - No manifest issues

2. **MainActivity.kt**: ✅ NavigationHost initialized
   - `setContent {}` called with NavigationHost
   - MainNavigation composable invoked
   - Error handling present

3. **MainNavigation.kt**: ❌ **ROOT CAUSE IDENTIFIED**
   - `startDestination = "events"` (line 36)
   - `composable("events")` route is **empty** (lines 39-42: placeholder comment only)
   - DashboardScreen exists but **not registered** in navigation graph
   - Result: Blank screen because start destination has no UI content

4. **DashboardScreen.kt**: ✅ Exists and functional
   - Composable function with proper parameters
   - Requires: syncStatus, eventCount, navigation callbacks, onSyncNow
   - UI components: SyncStatusCard, StatCard, QuickActionButton

5. **DashboardViewModel.kt**: ✅ Exists
   - AndroidViewModel subclass
   - Manages sync status and stats
   - Requires Application context for TruthRepository

6. **ViewModelFactory**: ❌ **MISSING**
   - No centralized ViewModelFactory
   - NodesViewModel uses inline factory (object : ViewModelProvider.Factory)
   - DashboardViewModel needs factory for proper initialization

## Current State Analysis

### Files Inspected

1. **AndroidManifest.xml** (`truth-android-client/app/src/main/AndroidManifest.xml`)
   - Status: ✅ No changes needed
   - MainActivity correctly configured as launcher

2. **MainActivity.kt** (`truth-android-client/app/src/main/java/com/truth/training/client/MainActivity.kt`)
   - Status: ✅ NavigationHost initialization correct
   - Lines 52-80: setContent with MainNavigation
   - Error handling present (lines 28-39, 81-85)

3. **MainNavigation.kt** (`truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/MainNavigation.kt`)
   - Status: ❌ **REQUIRES MODIFICATION**
   - Line 36: `startDestination = "events"` → **MUST CHANGE** to `"dashboard"`
   - Lines 39-42: Empty composable("events") → **MUST REPLACE** with DashboardScreen registration
   - Missing: DashboardScreen route registration

4. **DashboardScreen.kt** (`truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/DashboardScreen.kt`)
   - Status: ✅ Ready to use
   - Function signature: `DashboardScreen(syncStatus, eventCount, onNavigateToEvents, onNavigateToContexts, onNavigateToJudgments, onSyncNow, modifier)`
   - Requires SyncStatus data type

5. **DashboardViewModel.kt** (`truth-android-client/app/src/main/java/com/truth/training/client/ui/DashboardViewModel.kt`)
   - Status: ✅ Ready to use
   - Constructor: `DashboardViewModel(app: Application)`
   - Provides: info, stats, lastSync StateFlows

6. **ViewModelFactory**: ❌ **MUST CREATE**
   - Location: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/ViewModelFactory.kt`
   - Pattern: Follow NodesViewModel inline factory pattern but make it reusable
   - Required for: DashboardViewModel, future ViewModels

## Technical Dependencies

### Required Data Types

- **SyncStatus**: Used by DashboardScreen
  - Location: `com.truth.training.client.data.SyncStatus`
  - Need to verify structure and availability

- **TruthRepository**: Used by DashboardViewModel
  - Location: `com.truth.training.client.data.TruthRepository`
  - Already instantiated in DashboardViewModel constructor

### Navigation Dependencies

- **NavHostController**: Provided by MainActivity via `rememberNavController()`
- **Navigation routes**: Must define "dashboard" route
- **Navigation callbacks**: Already defined in MainActivity (lines 63-76)

## Solution Approach

### Step 1: Create ViewModelFactory
- Create reusable ViewModelFactory.kt
- Support DashboardViewModel creation
- Follow existing pattern from NodesViewModel (inline factory)

### Step 2: Register DashboardScreen in Navigation Graph
- Add `composable("dashboard")` route in MainNavigation.kt
- Initialize DashboardViewModel using ViewModelFactory
- Pass required parameters to DashboardScreen composable
- Connect navigation callbacks from MainActivity

### Step 3: Update Start Destination
- Change `startDestination = "events"` to `startDestination = "dashboard"`
- Verify "events" route can remain for future use (or remove if not needed)

### Step 4: Verify SyncStatus Availability
- Check SyncStatus data structure
- Ensure DashboardViewModel provides syncStatus StateFlow
- Map ViewModel state to DashboardScreen parameters

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| SyncStatus structure unknown | Medium | Inspect TruthRepository.lastSync and SyncStatus definition |
| Navigation callbacks mismatch | Low | Use existing callbacks from MainActivity (already defined) |
| ViewModelFactory pattern inconsistency | Low | Follow existing NodesViewModel pattern, make it reusable |
| Blank screen persists after changes | High | Add instrumentation tests to verify DashboardScreen displays |

## Open Questions Resolved

✅ **Q1: Which screen should be initial?** → A: DashboardScreen (from clarifications)  
✅ **Q2: What is the current problem?** → A: Blank screen due to empty "events" route  
✅ **Q3: Navigation graph state?** → A: Inspected - "events" is empty, DashboardScreen not registered  
✅ **Q4: ViewModelFactory pattern?** → A: Missing, must create following NodesViewModel pattern  

## Next Steps

1. Verify SyncStatus structure and availability
2. Create ViewModelFactory.kt
3. Update MainNavigation.kt with DashboardScreen registration
4. Change start destination to "dashboard"
5. Test on physical device

---

_Version: v1.0.0_

