# Feature Specification: Fix Installer Packaging for Truth Core Server

**Feature Branch**: `009-fix-linux-deb`  
**Created**: 2025-01-XX  
**Status**: Draft  
**Input**: User description: "Fix Linux (DEB/RPM), macOS (.pkg), and Windows installer packaging for truth_core_server. Packages must install files using correct filesystem layout, proper naming, and run the server using the user who installs the package (no dedicated truth user unless explicitly allowed by OS)."

## Execution Flow (main)
```
1. Parse user description from Input
   → If empty: ERROR "No feature description provided"
2. Extract key concepts from description
   → Identify: actors, actions, data, constraints
3. For each unclear aspect:
   → Mark with [NEEDS CLARIFICATION: specific question]
4. Fill User Scenarios & Testing section
   → If no clear user flow: ERROR "Cannot determine user scenarios"
5. Generate Functional Requirements
   → Each requirement must be testable
   → Mark ambiguous requirements
6. Identify Key Entities (if data involved)
7. Run Review Checklist
   → If any [NEEDS CLARIFICATION]: WARN "Spec has uncertainties"
   → If implementation details found: ERROR "Remove tech details"
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers
- 🧠 Align with collective intelligence principles and truth training methodology

### Section Requirements
- **Mandatory sections**: Must be completed for every feature
- **Optional sections**: Include only when relevant to the feature
- When a section doesn't apply, remove it entirely (don't leave as "N/A")

### For AI Generation
When creating this spec from a user prompt:
1. **Mark all ambiguities**: Use [NEEDS CLARIFICATION: specific question] for any assumption you'd need to make
2. **Don't guess**: If the prompt doesn't specify something (e.g., "login system" without auth method), mark it
3. **Think like a tester**: Every vague requirement should fail the "testable and unambiguous" checklist item
4. **Common underspecified areas**:
   - User types and permissions
   - Data retention/deletion policies  
   - Performance targets and scale
   - Error handling behaviors
   - Integration requirements
   - Security/compliance needs

---

## Clarifications

### Session 2025-11-23
- Q: What happens when a user installs the package but doesn't have permission to create user services? → A: Installer continues installation but warns that service will not start automatically
- Q: What happens if the user account is deleted after installation? → A: Package uninstall script (postrm) must check and clean up service during uninstallation
- Q: How does the system handle upgrades when the previous version used a different installation layout? → A: Automatically migrate files from old paths to new paths during upgrade
- Q: How does the installer handle conflicts with existing installations using the old naming convention? → A: Fail installation with error message instructing user to remove old version first
- Q: What happens if installation is interrupted (e.g., disk space or user cancellation)? → A: Rollback all changes and restore system to original state

## User Scenarios & Testing *(mandatory)*

### Primary User Story
As a system administrator, I want to install the Truth Core Server package on my Linux/macOS/Windows system so that the server runs automatically under my user account without requiring root privileges or a dedicated service user, and follows standard installation conventions for my operating system.

### Acceptance Scenarios
1. **Given** a Linux system administrator has downloaded a DEB package, **When** they install it using `dpkg -i`, **Then** the server executable is placed in `/usr/bin/` with the name `truth-core-server`, configuration directory is created in `/etc/truth-core-server/`, and the service runs as the installing user without requiring root privileges.

2. **Given** a Linux system administrator has downloaded an RPM package, **When** they install it using `rpm -i`, **Then** the installation follows the same layout and naming conventions as the DEB package, and the service runs as the installing user.

3. **Given** a macOS user has downloaded a .pkg installer, **When** they install it, **Then** the executable is placed in `/usr/local/bin/truth-core-server` and the service runs under their user account.

4. **Given** a Windows user has downloaded an installer, **When** they install it, **Then** the executable is named `truth-core-server.exe` and the service runs under their user account.

5. **Given** a user has installed the package, **When** they start the service, **Then** it runs automatically on system boot without requiring manual intervention or root access.

6. **Given** a user has installed the package, **When** they check the service status, **Then** they can see that it is running under their user account, not a dedicated service account.

### Edge Cases
- **User service permission issues**: If a user installs the package but cannot create user services (e.g., systemd user services not supported), the installer MUST continue installation but display a warning message that the service will not start automatically. The binary and configuration files are still installed, allowing manual service setup if needed.
- **Upgrade migration**: When upgrading from a previous version that used a different installation layout (e.g., `/usr/local/bin/truth_core_server` to `/usr/bin/truth-core-server`), the upgrade process MUST automatically detect and migrate files from old paths to new paths. This includes stopping the old service, moving the binary to the new location, updating service definitions, and starting the new service. Configuration files MUST be preserved and migrated if their location changes.
- **User account deletion**: If the user account is deleted after installation, the service files in user-specific directories (e.g., `~/.config/systemd/user/`) are automatically removed by the operating system when the home directory is deleted. During package uninstallation, the uninstall script (postrm) MUST check if the service still exists and clean it up gracefully if found, without failing if the service was already removed by the OS.
- **Naming convention conflicts**: If the installer detects an existing installation using the old naming convention (e.g., `truth_core_server` binary or service), the installation MUST fail with a clear error message instructing the user to remove the old version first before installing the new package. This prevents conflicts and ensures clean migration.

- **Installation interruption**: If installation is interrupted (e.g., insufficient disk space, user cancellation, or system error), the package manager MUST automatically rollback all changes and restore the system to its original state before installation began. This ensures system integrity and prevents partial installations.

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: Installer packages MUST place the server executable in standard system directories following operating system conventions (Linux: `/usr/bin/`, macOS: `/usr/local/bin/`, Windows: application directory).

- **FR-002**: The server executable MUST be named `truth-core-server` (with hyphens) across all platforms, replacing any previous naming conventions using underscores.

- **FR-003**: Installer packages MUST install configuration files in standard system configuration directories (Linux: `/etc/truth-core-server/`, macOS: `/usr/local/etc/truth-core-server/`, Windows: application config directory).

- **FR-004**: The service MUST run under the user account that installed the package, not a dedicated service user account.

- **FR-005**: On Linux systems, the service MUST be installed as a user service (systemd user service) that can be managed without root privileges.

- **FR-006**: The service MUST automatically start after installation and restart on system boot without requiring manual intervention.

- **FR-007**: Installer packages MUST NOT create dedicated service user accounts (e.g., "truth" user) unless explicitly required by the operating system's security model.

- **FR-008**: Installer packages MUST support standard package management operations (install, upgrade, uninstall) without breaking existing installations.

- **FR-009**: The installation process MUST preserve any existing configuration files during upgrades.

- **FR-010**: Installer packages MUST provide clear feedback during installation about where files are being placed and which user account will run the service.

- **FR-011**: On Linux systems, the service unit file MUST be placed in the user's systemd directory (`~/.config/systemd/user/`) rather than system-wide directories.

- **FR-012**: The service MUST be manageable using standard service management commands (Linux: `systemctl --user`, macOS: `launchctl`, Windows: service management tools) without requiring elevated privileges.

- **FR-013**: Installer packages MUST follow standard naming conventions for their respective platforms (DEB: `truth-core-server`, RPM: `truth-core-server`, macOS: `truth-core-server.pkg`, Windows: `truth-core-server.exe`).

- **FR-014**: If user services cannot be created during installation (e.g., systemd user services not supported), the installer MUST continue installation, display a warning message that automatic service startup is unavailable, and allow manual service configuration by the user.

- **FR-015**: During package uninstallation, the uninstall script (postrm/prerm) MUST check if the service still exists and clean it up gracefully (stop and disable service) if found, without failing if the service was already removed by the operating system (e.g., due to user account deletion).

- **FR-016**: During package upgrade from a previous version with different installation layout, the upgrade process MUST automatically detect files in old locations (e.g., `/usr/local/bin/truth_core_server`), stop the old service, migrate the binary to the new standard location (e.g., `/usr/bin/truth-core-server`), update service definitions to use new paths, and start the new service. Configuration files MUST be preserved and migrated if their location changes.

- **FR-017**: If the installer detects an existing installation using the old naming convention (e.g., `truth_core_server` binary or service files), the installation MUST fail with a clear, actionable error message instructing the user to remove the old version first before installing the new package. This prevents naming conflicts and ensures clean migration.

- **FR-018**: If installation is interrupted for any reason (e.g., insufficient disk space, user cancellation, system error), the package manager MUST automatically rollback all changes and restore the system to its original state before installation began. This ensures system integrity and prevents partial or corrupted installations.

### Key Entities
- **Installation Package**: Represents a distributable package format (DEB, RPM, PKG, EXE) containing the server executable, configuration templates, and service definitions.

- **Service Definition**: Represents the configuration that defines how the server runs as a background service, including executable path, user account, restart behavior, and environment variables.

- **Installation Layout**: Represents the filesystem structure where package files are placed, including executable location, configuration directory, and service definition location.

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status
*Updated by main() during processing*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
