/specify
Goal:
Fix the issue in truth-android-client where the application starts and instantly disappears from the screen while continuing to run in the background. The objective is to properly register the required UI screens and activities in AndroidManifest.xml so the application opens normally and stays visible.

Requirements:
1. Update AndroidManifest.xml:
   - Ensure the correct launcher Activity is declared.
   - Ensure the Activity is exported and visible on launch.
   - Add proper intent filters for main launcher behavior.
   - Prevent premature closing of the UI after onCreate().
2. Validate that the main NavigationHost is initialized correctly.
3. Ensure the initial composable screen (e.g., DashboardScreen or NodesScreen) is assigned as the entry point.
4. Ensure any required ViewModels are attached through the correct factory.
5. Add any missing screens into the navigation graph if required.
6. Provide all necessary Kotlin and manifest changes as patches ready for PR generation.

Important Rule:
- The CLI part of the application is used exclusively for verifying the Core, Server, and CLI functionality by the Cursor AI agent. It must not be modified for UI-related changes.

Deliverables:
- A validated plan for required changes.
- Patches for manifest, navigation, and affected Kotlin files.
- A correctly structured PR generated from the final specification.
