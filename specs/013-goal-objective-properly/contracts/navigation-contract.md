# Navigation Contract

**Feature**: 013-goal-objective-properly  
**Component**: MainNavigation.kt

## Contract Definition

### Navigation Graph Structure

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/MainNavigation.kt`

#### Required Routes

| Route | Screen | Status |
|-------|--------|--------|
| `"dashboard"` | DashboardScreen | **MUST BE ADDED** |
| `"events"` | EventListScreen | Exists (placeholder) |
| `"event/create"` | EventCreateScreen | Exists (placeholder) |
| `"event/{eventId}"` | EventDetailScreen | Exists (placeholder) |
| `"contexts"` | ContextTemplateListScreen | Exists (placeholder) |
| `"context/create"` | ContextTemplateEditorScreen | Exists (placeholder) |
| `"context/{templateId}"` | ContextTemplateEditorScreen | Exists (placeholder) |
| `"judgments/{eventId}"` | JudgmentListScreen | Exists (placeholder) |
| `"judgment/submit/{eventId}"` | JudgmentSubmissionScreen | Exists (placeholder) |
| `"nodes"` | NodesScreen | Exists (implemented) |

#### Start Destination Contract

**REQUIRED**: `startDestination = "dashboard"`

**Current**: `startDestination = "events"` (empty placeholder)

**Change**: Must be updated to `"dashboard"` to display DashboardScreen on launch.

### Navigation Callbacks Contract

**Source**: MainActivity.kt provides callbacks to MainNavigation

```kotlin
interface NavigationCallbacks {
    fun onNavigateToEvents(): Unit
    fun onNavigateToEventDetails(eventId: String): Unit
    fun onNavigateToNewEvent(): Unit
    fun onNavigateToContexts(): Unit
    fun onNavigateToContextEditor(templateId: Int?): Unit
    fun onNavigateToJudgments(eventId: String): Unit
    fun onNavigateToJudgmentSubmission(eventId: String): Unit
    fun onNavigateBack(): Unit
}
```

**Implementation**: Provided by MainActivity via NavHostController navigation methods.

### DashboardScreen Route Contract

**Route**: `"dashboard"`

**Composable Signature**:
```kotlin
composable("dashboard") {
    // MUST:
    // 1. Create DashboardViewModel using ViewModelFactory
    // 2. Collect ViewModel state flows
    // 3. Create SyncStatus from ViewModel state
    // 4. Extract eventCount from stats
    // 5. Call DashboardScreen composable with all required parameters
    // 6. Pass navigation callbacks from MainNavigation parameters
}
```

**Required Parameters for DashboardScreen**:
- `syncStatus: SyncStatus` - Derived from ViewModel state
- `eventCount: Int` - From stats.totalEvents
- `onNavigateToEvents: () -> Unit` - From MainNavigation parameter
- `onNavigateToContexts: () -> Unit` - From MainNavigation parameter
- `onNavigateToJudgments: () -> Unit` - From MainNavigation parameter
- `onSyncNow: () -> Unit` - Calls viewModel.refresh()

## Validation Rules

1. **Start Destination**: MUST be `"dashboard"` (not `"events"` or empty)
2. **Dashboard Route**: MUST be registered before or as start destination
3. **ViewModel Creation**: MUST use ViewModelFactory (not inline factory)
4. **State Collection**: MUST collect all required ViewModel StateFlows
5. **SyncStatus Mapping**: MUST correctly map ViewModel state to SyncStatus
6. **Navigation Callbacks**: MUST pass all callbacks from MainNavigation parameters

## Breaking Changes

**None** - This is a new route addition and start destination change. Existing routes remain unchanged.

---

_Version: v1.0.0_

