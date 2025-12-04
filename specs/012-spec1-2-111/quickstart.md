# Quickstart: Android Parity with Desktop UI & Startup Fix

**Feature Branch**: `012-spec1-2-111`  
**Date**: 2025-12-03  
**Status**: Draft

## Overview

This quickstart guides you through verifying and implementing Android parity with Desktop UI while fixing the critical startup bug. The feature includes: (1) fixing AndroidManifest.xml to make MainActivity the launcher, (2) ensuring TruthDatabase uses canonical schema from shared SQL assets, (3) removing legacy tables without data migration, (4) replacing numeric context inputs with dropdowns, (5) implementing context validation, (6) documenting or implementing RU/EN localization, and (7) updating documentation.

## Prerequisites

- Android Studio (latest stable)
- Android SDK (API 24+)
- Android device/emulator for testing
- Git branch `012-spec1-2-111` checked out
- Access to `core/src/storage.rs` for canonical schema reference

## Step 1: Fix Android Startup (Critical)

### 1.1 Update AndroidManifest.xml

**File**: `truth-android-client/app/src/main/AndroidManifest.xml`

**Changes**:
1. Remove launcher declaration from `MainDashboardActivity`:
   ```xml
   <activity
       android:name=".MainDashboardActivity"
       android:exported="false" />
   ```

2. Make `MainActivity` the launcher:
   ```xml
   <activity
       android:name=".MainActivity"
       android:exported="true"
       android:theme="@style/Theme.Material3.DayNight.NoActionBar">
       <intent-filter>
           <action android:name="android.intent.action.MAIN" />
           <category android:name="android.intent.category.LAUNCHER" />
       </intent-filter>
   </activity>
   ```

**Verification**:
- Launch app on emulator/device
- **Expected**: `MainActivity` (Compose UI) is displayed, app does not disappear

### 1.2 Verify MainActivity Initialization

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/MainActivity.kt`

**Check**:
- `setContent {}` is called with `MainNavigation` composable
- `NavController` is initialized via `rememberNavController()`
- Entry screen is explicitly defined in navigation graph

**Verification**:
- Launch app
- **Expected**: Main navigation screen is visible and stable

## Step 2: Database Schema Parity

### 2.1 Create Shared SQL Asset

**File**: `truth-android-client/app/src/main/assets/schema.sql` (new file)

**Content**: Extract canonical SQL from `core/src/storage.rs::SCHEMA_SQL` and save as `schema.sql`.

**Verification**:
- File exists in `app/src/main/assets/`
- SQL matches `core/src/storage.rs::SCHEMA_SQL`

### 2.2 Update TruthDatabase Initialization

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabase.kt`

**Changes**:
1. Read `schema.sql` from assets during database initialization
2. Execute SQL to create canonical schema
3. Ensure all canonical tables are created: `truth_events`, `impact`, `progress_metrics`, `context`, etc.

**Verification**:
- Database inspector shows canonical tables
- No legacy `events` table exists

### 2.3 Add Migration to Drop Legacy Tables

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/data/database/TruthDatabaseMigrations.kt`

**Changes**:
1. Add migration that executes:
   ```sql
   DROP TABLE IF EXISTS events;
   DROP TABLE IF EXISTS impacts;
   DROP TABLE IF EXISTS summaries;
   DROP TABLE IF EXISTS logs;
   ```
2. Ensure migration is idempotent (safe to run multiple times)

**Verification**:
- Run migration on database with legacy tables
- **Expected**: Legacy tables are dropped, canonical tables exist

### 2.4 Add Regression Test

**File**: `truth-android-client/app/src/androidTest/java/com/truth/training/client/data/database/TruthDatabaseSchemaTest.kt` (new file)

**Test**:
```kotlin
@Test
fun testLegacyTablesAbsent() {
    val db = Room.inMemoryDatabaseBuilder(context, TruthDatabase::class.java).build()
    // Initialize database
    // Query sqlite_master for legacy table names
    // Assert COUNT = 0
}
```

**Verification**:
- Run test
- **Expected**: Test passes (no legacy tables found)

## Step 3: Context Dropdowns

### 3.1 Create Context Picker Component

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/components/ContextPicker.kt` (new file)

**Implementation**:
- Compose dropdown component (e.g., `ExposedDropdownMenuBox`)
- Load contexts from `ContextTemplateRepository.getAllTemplatesFlow()`
- Display human-readable labels
- Allow selection from list

**Verification**:
- Component renders dropdown with labels
- Selection updates state

### 3.2 Update EventCreateScreen

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt`

**Changes**:
1. Replace `OutlinedTextField` for context fields with `ContextPicker` components
2. Load contexts via `ContextTemplateRepository` in ViewModel or Composable
3. Populate dropdowns with context data

**Verification**:
- Open `EventCreateScreen`
- **Expected**: Dropdowns are displayed with human-readable labels

### 3.3 Add Context Validation

**File**: `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/EventRepository.kt`

**Changes**:
1. Add validation method that checks context IDs against lookup tables
2. Return error if any ID is invalid
3. Block submission if validation fails

**Verification**:
- Attempt to submit event with invalid context ID
- **Expected**: Submission is blocked, error message is displayed

## Step 4: Localization Audit

### 4.1 Check Localization Status

**Files to check**:
- `truth-android-client/app/src/main/res/values/strings.xml`
- `truth-android-client/app/src/main/res/values-ru/strings.xml` (if exists)

**Action**:
- If `values-ru/` exists: Verify RU/EN switching works, ensure strings are consistent with Desktop
- If EN-only: Document EN-only status in specs and quickstarts

### 4.2 Update Documentation

**Files to update**:
- `docs/quickstart_android.md`
- `spec/09-ux-guidelines.md`
- `docs/UI_Desktop.md`
- `truth-android-client/README.md`

**Content**: Document Android localization status (RU/EN or EN-only)

**Verification**:
- Documentation clearly states localization status

## Step 5: Documentation Updates

### 5.1 Update Quickstarts

**Files**:
- `docs/quickstart_android.md`
- `docs/quickstart_desktop.md`

**Content**: Add Android behavior alongside Desktop, describe Init workflow, dropdown UI, validation rules, localization status

### 5.2 Update Specs

**Files**:
- `spec/23-function_desktop.md`
- `spec/09-ux-guidelines.md`

**Content**: Include Android behavior, cross-platform parity notes

### 5.3 Update UI Guidelines

**File**: `docs/UI_Desktop.md`

**Content**: Add Android UI behavior, context dropdown UX, validation rules

**Verification**:
- All documentation files are updated
- Cross-platform parity is documented

## Validation Checklist

- [ ] Android app launches successfully and displays UI without disappearing
- [ ] Database initialization uses canonical schema from shared SQL asset
- [ ] Legacy tables are dropped immediately without data migration
- [ ] Context fields use dropdowns instead of numeric inputs
- [ ] Context validation blocks invalid IDs
- [ ] Localization status is documented
- [ ] All documentation files are updated
- [ ] Automated tests pass (schema validation, UI tests)

## Troubleshooting

### App Still Disappears on Launch
- Check `AndroidManifest.xml` for correct launcher Activity
- Verify `MainActivity` has `exported="true"` and intent filters
- Check Logcat for crash logs

### Legacy Tables Still Exist
- Verify migration executes `DROP TABLE IF EXISTS` statements
- Check database inspector for table names
- Run regression test to verify

### Context Dropdowns Not Loading
- Check `ContextTemplateRepository.getAllTemplatesFlow()` returns data
- Verify database is initialized and contexts are seeded
- Check Logcat for errors

### Documentation Inconsistencies
- Run link checker script: `scripts/doc_refactor/fix_broken_links.py --check`
- Verify all cross-references are updated

## References

- **Spec**: `/home/ekwator/Code/truth-training/specs/012-spec1-2-111/spec.md`
- **Plan**: `/home/ekwator/Code/truth-training/specs/012-spec1-2-111/plan.md`
- **Contracts**: `/home/ekwator/Code/truth-training/specs/012-spec1-2-111/contracts/`
- **Canonical Schema**: `core/src/storage.rs::SCHEMA_SQL`
- **Desktop Reference**: `ui/desktop/src/pages/NewEvent.tsx`

