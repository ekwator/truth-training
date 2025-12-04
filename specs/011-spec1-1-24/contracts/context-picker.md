# Contract: Context Selection & Validation UX

## Actors
- **User**: Event creator on desktop UI (`NewEvent` page).
- **Frontend**: React `ContextPicker` component (to be introduced).
- **Backend**: `ApiService.getContexts()` (Tauri command `list_contexts` or HTTP `/contexts` endpoint).

## Data Contract
```ts
type ContextOption = {
  id: number;
  name: string;
  description?: string;
  category_id?: number;
  forma_id?: number;
  cause_id?: number;
  develop_id?: number;
  effect_id?: number;
};
```
- Response payload: `{ data: ContextOption[], fetched_at: string }`.
- UI caches dataset with `fetchedAt` timestamp to show “Last updated …”.

## Interaction Flow
1. **Load**: On component mount, show skeleton → fetch contexts. Failure displays inline error (`"Unable to load contexts. Retry"`) and disables submission until retry succeeds.
2. **Combo experience**:
   - Primary input is a searchable dropdown listing `name` + short metadata badges.
   - Manual entry allowed via free-text field; on blur or submit, value is matched against `ContextOption.id`.
   - Autocomplete suggestions update as the user types (downshift-style).
3. **Validation**:
   - Submission blocked unless every populated context field matches an ID present in the dataset.
   - Invalid IDs highlight the field, show tooltip `"Unknown context ID"`, and log telemetry event.
   - When dataset is stale (>24h) show warning banner but still allow selection if IDs validated.
4. **Template integration**: Selecting a template still pre-fills fields but the combo boxes render the friendly label rather than numeric IDs.
5. **Accessibility**: Keyboard navigation (arrow keys, Enter, ESC), ARIA attributes for role="combobox", `aria-expanded`, `aria-activedescendant`.

## Acceptance Criteria
- Attempting to submit with an ID absent from `ContextOption` results in blocked submission and error toast.
- Valid selection populates event payload with numeric IDs identical to `context.id`.
- Loading indicator, empty state (“No contexts available”), and error state are all localized (EN/RU).
- Unit/UI test covers: loading → list render, invalid manual entry, valid selection path.

## Failure Modes
- **Fetch failure**: Show inline error + retry button; disable submit.
- **Zero contexts**: Display message prompting admin to seed contexts; allow manual entry but still validate (will fail until dataset present).
- **Offline**: Use cached dataset from previous fetch stored in IndexedDB/localStorage; mark as offline but still enforce whitelist.

## Observability
- Log `context_picker.load.success|failure`, `context_picker.validation.failure` with metadata (count of invalid IDs).
- Optional metrics: time from mount to first successful fetch (UX perf).

