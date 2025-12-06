Create a complete modification plan for implementing the changes defined in the previous /speckit.specify request:

Goal:
The solution must properly register the UI entry point and screen structure in AndroidManifest.xml and ensure that the application opens normally and remains visible.

Plan Requirements:
1. Manifest Corrections
   - Add or correct the <activity> entry that should serve as the primary launcher.
   - Add the proper intent-filter with MAIN and LAUNCHER categories.
   - Ensure the activity is exported when required rules.

2. Entry Activity Initialization
   - Confirm that the launcher Activity initializes the NavigationHost properly.
   - Ensure that setContent{} in the Activity loads the correct initial composable.

3. Navigation Structure Validation
   - Validate that the navigation graph includes the initial screen.
   - Add missing screens if they are referenced but not registered.
   - Verify that the navigation graph loads without runtime exceptions.

4. UI Startup Flow
   - Ensure the initial screen (DashboardScreen or NodesScreen) is reachable and stable.
   - Fix any early termination caused by lifecycle issues or missing UI bindings.

5. ViewModel Setup
   - Confirm the ViewModel factory is injected correctly.
   - Add missing ViewModels for the launcher screen if required.

6. Testing Plan
   - Add unit tests or instrumentation tests to verify:
     - The manifest declares the correct launcher Activity.
     - The navigation graph loads the initial screen successfully.
     - Conduct final testing on a real physical device by first checking its connection with the adb devices command; if not connected, warn the user and receive confirmation of the connection from them.

7. Deliverables
   - A complete set of manifest patches.
   - Kotlin file modifications for Activity, Navigation, and ViewModels.
   - Instructions for applying the patch via PR generated from the final plan.
   - Update the documentation by adding the relevant documents to the spec/ and docs/ directories with clickable links in markdown format in all cross-documents

Important Rule:
- The CLI part of the application must remain untouched because it is used only for verifying Core, Server, and CLI layers by the Cursor AI agent.

Generate:
- A clear, step-by-step plan.
- Explicit file paths and modifications.
- Dependencies between steps.
- A ready-to-execute plan suitable for subsequent tasks and /implementation in automatic mode

_Version: v1.0.0_
