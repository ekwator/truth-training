# Visual Regression Test: Action Buttons

**Task:** T080  
**User Story:** US4 - Action Button Emoji Implementation  
**Status:** ⏳ Pending (requires screenshots)

## Purpose

This document provides side-by-side comparison of Android action buttons with Desktop action buttons to verify emoji consistency and visual parity.

## Comparison Matrix

| Action | Desktop Button | Android Button (EN) | Android Button (RU) | Emoji Match | Status |
|--------|----------------|---------------------|---------------------|-------------|--------|
| Save | 💾 Save | 💾 Save | 💾 Сохранить | ✅ | ⏳ Pending |
| Cancel | ❌ Cancel | ❌ Cancel | ❌ Отмена | ✅ | ⏳ Pending |
| Delete | 🗑️ Delete | 🗑️ Delete | 🗑️ Удалить | ✅ | ⏳ Pending |
| Edit | ✏️ Edit | ✏️ Edit | ✏️ Редактировать | ✅ | ⏳ Pending |
| Create | ➕ Create | ➕ Create | ➕ Создать | ✅ | ⏳ Pending |
| Submit | ✅ Submit | ✅ Submit | ✅ Отправить | ✅ | ⏳ Pending |
| Refresh | 🔄 Refresh | 🔄 Refresh | 🔄 Обновить | ✅ | ⏳ Pending |
| Sync | 🔄 Sync | 🔄 Sync | 🔄 Синхронизировать | ✅ | ⏳ Pending |
| Back | ⬅️ Back | ⬅️ Back | ⬅️ Назад | ✅ | ⏳ Pending |
| Next | ➡️ Next | ➡️ Next | ➡️ Далее | ✅ | ⏳ Pending |

## Screenshots

### Desktop Screenshots
- [ ] Save button
- [ ] Cancel button
- [ ] Delete button
- [ ] Edit button
- [ ] Create button
- [ ] Submit button
- [ ] Refresh button
- [ ] Sync button
- [ ] Back button
- [ ] Next button

### Android Screenshots (English)
- [ ] Save button
- [ ] Cancel button
- [ ] Delete button
- [ ] Edit button
- [ ] Create button
- [ ] Submit button
- [ ] Refresh button
- [ ] Sync button
- [ ] Back button
- [ ] Next button

### Android Screenshots (Russian)
- [ ] Save button (Сохранить)
- [ ] Cancel button (Отмена)
- [ ] Delete button (Удалить)
- [ ] Edit button (Редактировать)
- [ ] Create button (Создать)
- [ ] Submit button (Отправить)
- [ ] Refresh button (Обновить)
- [ ] Sync button (Синхронизировать)
- [ ] Back button (Назад)
- [ ] Next button (Далее)

## Consistency Verification

- [ ] Same action type uses same emoji across all screens
- [ ] Save button (💾) is consistent in: EventCreateScreen, EventEditScreen, ContextTemplateEditorScreen, SettingsScreen
- [ ] Cancel button (❌) is consistent in: EventCreateScreen, EventEditScreen
- [ ] Delete button (🗑️) is consistent in: EventDetailScreen
- [ ] Edit button (✏️) is consistent in: EventDetailScreen
- [ ] Create button (➕) is consistent in: EventListScreen, ContextTemplateListScreen, JudgmentListScreen

## Verification Checklist

- [ ] All action buttons display correct emoji matching Desktop
- [ ] Emojis remain constant when language switches (English/Russian)
- [ ] Text portion changes correctly based on locale
- [ ] Same action type uses same emoji consistently across all screens
- [ ] Visual appearance matches Desktop implementation

## Notes

- Emojis are language-independent and should match Desktop exactly
- Text portion is localized (English/Russian)
- Pattern: `${emoji} ${localized_text}`
- Consistency is critical: same action = same emoji everywhere

---

**Last Updated:** 2025-01-XX  
**Next Steps:** Capture screenshots from Desktop and Android (both languages) for comparison

