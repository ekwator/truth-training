# Manual Testing Guide for Device Testing

**Device:** RMX3261 (Android 11, 720x1600)  
**APK:** Installed and launched successfully  
**Date:** 2025-12-10

## Quick Start

The application is already installed and launched on the device. You can now proceed with manual testing scenarios.

## Testing Scenarios

### T116: Emoji Display on Real Device

**Objective:** Verify all emojis render correctly across all screens

**Steps:**
1. Navigate through all screens:
   - Dashboard (🏠)
   - New Event (➕)
   - Context Editor (📝)
   - Events List (📋)
   - Judgments (⚖️)
   - Overall Summary (📊)
   - Training Results (📈)
   - Settings (⚙️)
   - Nodes (📋)

2. For each screen, verify:
   - [ ] Screen title displays correct emoji
   - [ ] Action buttons display correct emojis
   - [ ] Form field labels display correct emojis (if applicable)
   - [ ] Status indicators display correct emojis (if applicable)
   - [ ] Navigation items display correct emojis (if applicable)

**Expected Result:** All emojis render correctly and match Desktop implementation

---

### T117: Localization on Real Device

**Objective:** Verify emojis remain constant when language switches

**Steps:**
1. Set language to English:
   - Go to Settings → Language → English
   - Navigate through all screens
   - Verify emojis + English text (e.g., "🏠 Dashboard")

2. Switch language to Russian:
   - Go to Settings → Language → Russian
   - Navigate through all screens
   - Verify emojis + Russian text (e.g., "🏠 Панель управления")

3. Verify:
   - [ ] Emojis remain constant (same emoji in both languages)
   - [ ] Text portion changes correctly
   - [ ] Pattern: `${emoji} ${localized_text}` works correctly

**Expected Result:** Emojis are language-independent, text changes based on locale

---

### T118: Emoji Accessibility on Real Device

**Objective:** Verify TalkBack announces both emoji and text

**Steps:**
1. Enable TalkBack:
   - Go to Settings → Accessibility → TalkBack → Enable

2. Navigate through all screens with TalkBack enabled

3. For each UI element, verify:
   - [ ] Screen titles: TalkBack announces emoji + text
   - [ ] Action buttons: TalkBack announces emoji + text
   - [ ] Form field labels: TalkBack announces emoji + text
   - [ ] Status indicators: TalkBack announces emoji + text

**Expected Result:** TalkBack correctly announces both emoji and text for all elements

---

### T119: Emoji in Different Themes on Real Device

**Objective:** Verify emojis are visible in both light and dark themes

**Steps:**
1. Set theme to Light:
   - Go to Settings → Theme → Light (or system default)
   - Navigate through all screens
   - Verify emoji visibility and colors

2. Set theme to Dark:
   - Go to Settings → Theme → Dark
   - Navigate through all screens
   - Verify emoji visibility and colors

3. Verify:
   - [ ] All emojis visible in light theme
   - [ ] All emojis visible in dark theme
   - [ ] No contrast issues
   - [ ] Emoji colors appropriate for both themes

**Expected Result:** Emojis render correctly in both light and dark themes

---

### T120: Emoji Consistency on Real Device

**Objective:** Verify same functionality uses same emoji across all screens

**Steps:**
1. Navigate through all screens

2. Verify consistency:
   - [ ] Save button (💾) uses same emoji everywhere
   - [ ] Cancel button (❌) uses same emoji everywhere
   - [ ] Delete button (🗑️) uses same emoji everywhere
   - [ ] Edit button (✏️) uses same emoji everywhere
   - [ ] Create button (➕) uses same emoji everywhere
   - [ ] Refresh button (🔄) uses same emoji everywhere

3. Document any inconsistencies found

**Expected Result:** Same action type uses same emoji consistently across all screens

---

### T121: Graceful Degradation on Real Device

**Objective:** Verify UI remains functional if emojis fail to render

**Steps:**
1. Navigate through all screens
2. Verify UI functionality:
   - [ ] All buttons are clickable
   - [ ] All form fields are functional
   - [ ] Navigation works correctly
   - [ ] No crashes or errors

3. If device has limited emoji support:
   - [ ] Text labels display correctly without emojis
   - [ ] UI remains fully functional
   - [ ] No visual glitches

**Expected Result:** UI remains functional even if emojis fail to render

---

### T107 & T123: Quickstart.md Scenarios

**Objective:** Verify all 12 quickstart scenarios pass on real device

**Reference:** `specs/017-constitutional-requirement-rule/quickstart.md`

**Scenarios to test:**
1. [ ] Scenario 1: Emoji Mapping Utility Structure
2. [ ] Scenario 2: Dashboard Screen Emoji Coverage
3. [ ] Scenario 3: New Event Screen Emoji Coverage
4. [ ] Scenario 4: Event Edit Screen Emoji Coverage
5. [ ] Scenario 5: Context Editor Screen Emoji Coverage
6. [ ] Scenario 6: Settings Screen Emoji Coverage
7. [ ] Scenario 7: Status Indicator Emoji Coverage
8. [ ] Scenario 8: Navigation Menu Emoji Coverage
9. [ ] Scenario 9: Consistency Validation Across Screens
10. [ ] Scenario 10: Desktop Parity Validation
11. [ ] Scenario 11: Graceful Degradation Test
12. [ ] Scenario 12: Accessibility Test (TalkBack)

**Expected Result:** All 12 scenarios pass on real device

---

## Documenting Results

After completing each test scenario, update `device-testing-results.md` with:
- Test results (pass/fail)
- Screenshots (if applicable)
- Issues found
- Recommendations

## Notes

- Device is ready for testing
- Application is installed and can be launched
- All automated setup tasks (T112-T115) are complete
- Manual testing scenarios (T116-T121, T107, T123) require user interaction

---

**Status:** Ready for manual testing  
**Next Steps:** Execute manual testing scenarios and document results
