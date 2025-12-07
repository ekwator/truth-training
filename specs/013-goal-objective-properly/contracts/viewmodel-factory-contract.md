# ViewModelFactory Contract

**Feature**: 013-goal-objective-properly  
**Component**: ViewModelFactory.kt

## Contract Definition

### Factory Interface

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/ViewModelFactory.kt`

**Implements**: `androidx.lifecycle.ViewModelProvider.Factory`

### Required ViewModels

| ViewModel | Class | Constructor Parameters |
|-----------|-------|------------------------|
| DashboardViewModel | `com.truth.training.client.ui.DashboardViewModel` | `Application` |
| NodesViewModel | `com.truth.training.client.ui.compose.nodes.NodesViewModel` | `Application` |

### Factory Implementation Contract

```kotlin
class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // MUST:
        // 1. Check modelClass against known ViewModel types
        // 2. Return appropriate ViewModel instance
        // 3. Throw IllegalArgumentException for unknown types
    }
}
```

### Usage Contract

**In MainNavigation.kt**:

```kotlin
val context = LocalContext.current
val viewModel: DashboardViewModel = viewModel(
    factory = ViewModelFactory(context.applicationContext as Application)
)
```

**Pattern**: 
- Get Application context from LocalContext
- Create ViewModelFactory with Application
- Pass factory to `viewModel()` composable function

## Validation Rules

1. **Factory Creation**: MUST accept `Application` parameter
2. **ViewModel Support**: MUST support DashboardViewModel and NodesViewModel
3. **Error Handling**: MUST throw IllegalArgumentException for unsupported ViewModel types
4. **Type Safety**: MUST use proper type casting with `as T`
5. **Reusability**: MUST be reusable across multiple composables

## Extension Points

**Future ViewModels**: Add new `when` branch for each new ViewModel:

```kotlin
modelClass.isAssignableFrom(NewViewModel::class.java) -> {
    NewViewModel(application) as T
}
```

## Breaking Changes

**None** - This is a new component. Existing inline factories can be gradually migrated.

---

_Version: v1.0.0_

