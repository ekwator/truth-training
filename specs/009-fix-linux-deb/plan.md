# Implementation Plan: Fix Installer Packaging for Truth Core Server

**Branch**: `009-fix-linux-deb` | **Date**: 2025-11-23 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-fix-linux-deb/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Ensure collective intelligence principles are preserved
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, `GEMINI.md` for Gemini CLI, `QWEN.md` for Qwen Code, or `AGENTS.md` for all other agents).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary

Fix Linux (DEB/RPM), macOS (.pkg), and Windows installer packaging for truth_core_server to use correct filesystem layout, proper naming conventions (hyphens instead of underscores), and run services under the installing user account without requiring root privileges or dedicated service users. The solution involves updating packaging scripts, renaming binaries during build, fixing installation paths, converting systemd services to user services, and updating CI workflows.

## Technical Context
**Language/Version**: Rust 1.75+ (stable toolchain)  
**Primary Dependencies**: 
- FPM (Ruby gem) for DEB/RPM packaging
- pkgbuild (macOS) for .pkg creation
- NSIS + WinSW for Windows installers
- systemd (Linux) for service management
- launchctl (macOS) for service management

**Storage**: N/A (packaging only, no data storage changes)  
**Testing**: 
- Manual installation testing on target platforms
- Package verification (dpkg -c, rpm -qlp)
- Service status verification (systemctl --user, launchctl list)
- Binary path verification

**Target Platform**: 
- Linux (DEB: Debian/Ubuntu, RPM: RHEL/CentOS/Fedora)
- macOS (10.15+)
- Windows (10/11)

**Project Type**: single (packaging scripts and CI workflows)  
**Performance Goals**: N/A (packaging only)  
**Constraints**: 
- Must not require root privileges for service management
- Must follow OS-specific installation conventions
- Must preserve existing configuration during upgrades
- Must support standard package operations (install/upgrade/uninstall)
- Must handle migration from old naming/layout to new

**Scale/Scope**: 
- 3 packaging formats (DEB, RPM, PKG, EXE)
- 2 CI workflows (server-package.yml, server-debian.yml)
- 4 packaging directories (debian/, linux/, macos/, windows/)
- Binary rename: truth_core_server → truth-core-server

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Separation of Concerns by Crate
✅ **PASS**: Packaging is separate from core/server/app crates. Changes are isolated to packaging scripts and CI workflows.

### II. API- and CLI-First Interfaces
✅ **PASS**: No API/CLI changes required. This is purely packaging infrastructure.

### III. Cryptographic Integrity (NON-NEGOTIABLE)
✅ **PASS**: No cryptographic changes. Binary signing and verification remain unchanged.

### IV. Integration Testing Across Layers
⚠️ **REVIEW**: Manual testing required for package installation and service verification. Consider adding automated package installation tests in CI.

### V. Observability, Versioning & Simplicity
✅ **PASS**: Changes follow standard packaging conventions. Versioning handled by existing CI workflows.

### VI-IX. Collective Intelligence Principles
✅ **PASS**: No impact on collective intelligence features. Packaging changes are infrastructure-only.

**Initial Constitution Check**: ✅ PASS (with testing recommendation)  
**Post-Design Constitution Check**: ✅ PASS

## Project Structure

### Documentation (this feature)
```
specs/009-fix-linux-deb/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
packaging/
├── debian/
│   ├── Makefile         # Update: binary rename, paths, user service
│   ├── control          # Update: package name, paths
│   ├── postinst         # Update: user service installation
│   ├── postrm           # Update: user service removal
│   ├── prerm            # Update: user service stop
│   └── truth-core.service  # Update: user service template
├── linux/
│   ├── postinst         # Update: user service installation
│   └── truth-training-server.service  # Rename & update to user service
├── macos/
│   ├── com.truth.training.server.plist  # Update: executable path, LaunchAgent
│   └── postinstall.sh   # Update: user LaunchAgent instead of LaunchDaemon
└── windows/
    ├── postinstall.ps1  # Update: service installation
    └── truth-training-server.xml  # Rename & update: executable name

.github/workflows/
├── server-package.yml   # Update: binary rename, paths, user services
└── server-debian.yml    # Update: binary rename, paths, user services
```

**Structure Decision**: Single project structure. Packaging scripts are organized by platform in `packaging/` directory. CI workflows in `.github/workflows/` handle cross-platform builds. No new crates or modules required.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - Research systemd user services best practices
   - Research FPM packaging for user services
   - Research macOS LaunchAgent vs LaunchDaemon for user services
   - Research Windows user service installation without admin
   - Research binary renaming during build process

2. **Generate and dispatch research agents**:
   ```
   Task: "Research systemd user services for packaging (no root required)"
   Task: "Research FPM --deb-systemd and user service installation"
   Task: "Research macOS LaunchAgent installation in .pkg packages"
   Task: "Research Windows user service installation with WinSW"
   Task: "Research binary renaming strategies in Rust build process"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - InstallationPackage entity (format, platform, paths)
   - ServiceDefinition entity (type, user, paths)
   - InstallationLayout entity (executable, config, service locations)

2. **Generate API contracts** from functional requirements:
   - N/A: No API changes required. This is packaging-only.

3. **Generate contract tests** from contracts:
   - N/A: No API contracts. Package installation tests will be manual/integration.

4. **Extract test scenarios** from user stories:
   - Each acceptance scenario → installation verification test
   - Quickstart test = package installation and service verification steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh cursor`
   - Add packaging tools (FPM, pkgbuild, NSIS, WinSW, systemd user services)

**Output**: data-model.md, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (data model, quickstart)
- Each platform (Linux DEB, Linux RPM, macOS, Windows) → packaging update tasks [P]
- Each CI workflow → update task
- Binary rename → build script update task
- Service definition updates → per-platform tasks [P]
- Testing tasks → manual verification scenarios

**Ordering Strategy**:
- Platform-independent first: Binary rename, common script updates
- Platform-specific in parallel: DEB, RPM, PKG, EXE [P]
- CI workflow updates after packaging scripts
- Testing tasks last (manual verification)

**Estimated Output**: 15-20 numbered, ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, package installation verification)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

No violations requiring justification.

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [x] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented

---
*Based on Constitution v2.1.0 - See `.specify/memory/constitution.md`*
