# Tasks: Fix Installer Packaging for Truth Core Server

**Input**: Design documents from `/specs/009-fix-linux-deb/`
**Prerequisites**: [plan.md](plan.md)(p[lan.md](lan.md)) (required), [research.md](research.md)(r[esearch.md](esearch.md)), [data-model.md](data-model.md)(d[ata-model.md](ata-model.md)), [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md))

## Execution Flow (main)
```
1. Load [plan.md](plan.md)(p[lan.md](lan.md)) from feature directory
   → Extract: tech stack, libraries, structure
2. Load optional design documents:
   → [data-model.md](data-model.md)(d[ata-model.md](ata-model.md)): Extract entities → model tasks
   → [research.md](research.md)(r[esearch.md](esearch.md)): Extract decisions → setup tasks
   → [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)): Extract test scenarios → verification tasks
3. Generate tasks by category:
   → Setup: common script updates, binary rename logic
   → Core: platform-specific packaging updates
   → Integration: CI workflow updates
   → Polish: manual verification tests
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Platform-specific tasks can run in parallel
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
8. Validate task completeness
9. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- Packaging scripts: `packaging/{platform}/`
- CI workflows: `.github/workflows/`
- All paths relative to repository root

## Phase 3.1: Setup & Common Changes
- [x] T001 Update packaging/debian/Makefile: Rename binary from truth_core_server to truth-core-server, change installation path from /usr/local/bin to /usr/bin, remove systemd system-wide installation
- [x] T002 Update packaging/debian/control: Update package name to truth-core-server (if needed), ensure proper paths in description
- [x] T003 [P] Create packaging/debian/truth-core-server.service: New systemd user service file (no User= directive, path /usr/bin/truth-core-server, install location ~/.config/systemd/user/)

## Phase 3.2: Linux DEB Packaging Updates
- [x] T004 Update packaging/debian/postinst: Replace system-wide service installation with user service installation to ~/.config/systemd/user/, add warning if systemctl --user unavailable, handle service enable/start
- [x] T005 Update packaging/debian/postrm: Update to stop and disable user service (systemctl --user), handle gracefully if service already removed
- [x] T006 Update packaging/debian/prerm: Update to stop user service before removal (systemctl --user stop)
- [x] T007 Update packaging/debian/Makefile: Update to copy truth-core-server.service to temporary location for postinst, remove /etc/systemd/system installation

## Phase 3.3: Linux Generic Packaging Updates
- [x] T008 [P] Update packaging/linux/postinst: Replace system-wide service installation with user service installation to ~/.config/systemd/user/
- [x] T009 [P] Rename and update packaging/linux/truth-training-server.service: Rename to truth-core-server.service, update ExecStart to /usr/bin/truth-core-server, remove User= directive, update for user service

## Phase 3.4: macOS Packaging Updates
- [x] T010 [P] Update packaging/macos/com.truth.training.server.plist: Change executable path from /usr/local/bin/truth-training-server to /usr/local/bin/truth-core-server, ensure it's configured as LaunchAgent (not LaunchDaemon)
- [x] T011 [P] Update packaging/macos/postinstall.sh: Change from LaunchDaemon (/Library/LaunchDaemons/) to LaunchAgent (~/Library/LaunchAgents/), update launchctl commands for user service

## Phase 3.5: Windows Packaging Updates
- [x] T012 [P] Update packaging/windows/truth-training-server.xml: Rename executable from truth-training-server.exe to truth-core-server.exe, ensure service name is TruthCoreServer
- [x] T013 [P] Update packaging/windows/postinstall.ps1: Update service installation for user service mode (if WinSW supports it), ensure no admin requirement for service management

## Phase 3.6: CI Workflow Updates
- [x] T014 Update .github/workflows/server-package.yml build-linux job: Update FPM command to use /usr/bin/truth-core-server path, remove --deb-systemd flag, add binary rename step (cp truth_core_server truth-core-server), update systemd unit preparation for user service
- [x] T015 Update .github/workflows/server-package.yml build-linux job: Update RPM packaging to match DEB layout and naming
- [x] T016 Update .github/workflows/server-package.yml build-macos job: Update binary path to truth-core-server, change LaunchDaemon to LaunchAgent installation (~/Library/LaunchAgents/)
- [x] T017 Update .github/workflows/server-package.yml build-windows job: Update binary name to truth-core-server.exe, update WinSW config path
- [x] T018 Update .github/workflows/server-debian.yml: Update to use new Makefile paths (/usr/bin/truth-core-server), ensure binary rename is handled

## Phase 3.7: Migration & Conflict Detection
- [x] T019 Update packaging/debian/postinst: Add conflict detection for old naming (truth_core_server binary or service), fail with clear error message if found
- [x] T020 Update packaging/debian/postinst: Add upgrade migration logic to detect files in old locations (/usr/local/bin/truth_core_server), stop old service, migrate binary to /usr/bin/truth-core-server, update service definitions

## Phase 3.8: Manual Verification & Testing
- [ ] T021 [P] Manual test: Linux DEB installation verification per [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) (install package, verify binary location /usr/bin/truth-core-server, verify user service status, test service management without root)
- [ ] T022 [P] Manual test: Linux RPM installation verification per [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) (same as DEB)
- [ ] T023 [P] Manual test: macOS .pkg installation verification per [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) (install package, verify binary location /usr/local/bin/truth-core-server, verify LaunchAgent status, test service management)
- [ ] T024 [P] Manual test: Windows EXE installation verification per [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) (install package, verify binary location and name, verify service status, test service management)
- [ ] T025 Manual test: Package upgrade scenario per [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)) (install old version, upgrade to new, verify migration and service continuity)
- [ ] T026 Manual test: Conflict detection scenario (install old version, attempt new installation, verify error message)
- [ ] T027 Manual test: User service permission edge case (system without user service support, verify warning message and partial installation)

**Note**: Manual testing tasks (T021-T027) require built packages and cannot be automated. These should be performed after packages are built via CI or local build process.

## Dependencies
- T001-T003: Setup tasks, must complete before platform-specific tasks
- T004-T007: DEB packaging (sequential within DEB, can run parallel with other platforms)
- T008-T009: Linux generic packaging [P] (can run parallel with DEB, macOS, Windows)
- T010-T011: macOS packaging [P] (can run parallel with Linux, Windows)
- T012-T013: Windows packaging [P] (can run parallel with Linux, macOS)
- T014-T018: CI workflows (must complete after packaging scripts updated)
- T019-T020: Migration logic (must complete after postinst updates)
- T021-T027: Manual testing (must complete after all implementation)

## Parallel Execution Examples

### Example 1: Platform-Specific Packaging Updates (T008-T013)
```
# Launch T008-T013 together (different platforms, no conflicts):
Task: "Update packaging/linux/postinst for user service"
Task: "Rename and update packaging/linux/truth-training-server.service"
Task: "Update packaging/macos/com.truth.training.server.plist"
Task: "Update packaging/macos/postinstall.sh"
Task: "Update packaging/windows/truth-training-server.xml"
Task: "Update packaging/windows/postinstall.ps1"
```

### Example 2: Manual Testing (T021-T024)
```
# Launch T021-T024 together (different platforms, independent tests):
Task: "Manual test: Linux DEB installation verification"
Task: "Manual test: Linux RPM installation verification"
Task: "Manual test: macOS .pkg installation verification"
Task: "Manual test: Windows EXE installation verification"
```

## Notes
- [P] tasks = different files/platforms, no dependencies
- Platform-specific tasks (T008-T013) can run in parallel
- CI workflow updates (T014-T018) must wait for packaging script updates
- Migration logic (T019-T020) must be added to postinst after basic updates
- Manual testing (T021-T027) requires built packages from CI or local builds
- All binary references must change from `truth_core_server` to `truth-core-server`
- All service installations must use user-specific locations (no root required)
- Service files must NOT include User= or Group= directives for user services

## Task Generation Rules
*Applied during main() execution*

1. **From Data Model**:
   - InstallationPackage entities → packaging script updates
   - ServiceDefinition entities → service file updates
   - InstallationLayout entities → path updates

2. **From Research**:
   - Systemd user services → postinst/postrm/prerm updates
   - FPM packaging → CI workflow updates
   - LaunchAgent vs LaunchDaemon → macOS script updates
   - WinSW user services → Windows script updates

3. **From Quickstart**:
   - Each verification scenario → manual test task [P]
   - Upgrade scenarios → migration task
   - Edge cases → conflict detection task

4. **Ordering**:
   - Setup → Platform-specific → CI → Migration → Testing
   - Dependencies block parallel execution
   - Platform tasks can run in parallel

## Validation Checklist
*GATE: Checked before execution*

- [x] All packaging scripts updated for new paths and naming
- [x] All service definitions updated for user services
- [x] All CI workflows updated for binary rename and paths
- [x] Migration logic included for upgrades
- [x] Conflict detection included for old installations
- [x] Manual test scenarios cover all platforms
- [x] Each task specifies exact file path
- [x] Parallel tasks truly independent (different files/platforms)

_Version: v1.0.0_
