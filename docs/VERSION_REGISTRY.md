# Truth Training – Version Registry

This document tracks all current versions across the Truth Training ecosystem.

| Component         | Version | Description                  | Source / Link                                   |
|-------------------|---------|------------------------------|-------------------------------------------------|
| Core Library      | v0.4.2  | Main logic layer             | [core/Cargo.toml](../core/Cargo.toml)           |
| Server            | v0.4.2  | HTTP API service             | [server/Cargo.toml](../server/Cargo.toml)       |
| CLI (truthctl)    | v0.4.2  | Command-line interface       | [cli/Cargo.toml](../cli/Cargo.toml)             |
| Desktop UI        | v0.1.3  | Text-based desktop interface | [ui_desktop/Cargo.toml](../ui_desktop/Cargo.toml)|
| Spec Document     | v0.4.0  | Reference specification      | [spec/README.md](../spec/README.md)             |

---

## 🔄 Update Policy
- All version updates must be reflected in:
  - `Cargo.toml` for each crate
  - `spec/README.md` (Version Map)
  - This registry file

---

## 📦 Build References
- CI/CD Artifacts: [docs/CI_Workflows_Artifacts.md](./CI_Workflows_Artifacts.md)
- Install Paths by OS: [docs/Install_Paths_By_OS.md](./Install_Paths_By_OS.md)
- Release Notes: [GitHub Releases](https://github.com/ekwator/truth-training/releases)
