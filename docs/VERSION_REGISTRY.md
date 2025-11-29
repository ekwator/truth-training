# Truth Training – Version Registry

This document tracks all current versions across the Truth Training ecosystem.

| Crate/Component     | Version | Description                        |
|---------------------|---------|------------------------------------|
| app                 | v1.0.0  | Main desktop app/logic             |
| core_lib            | v1.0.0  | Shared core library                |
| truth_core          | v1.0.0  | Core library binary                |
| truth-ui-desktop    | v1.0.0  | Tauri-based desktop UI             |
| truth-android-client| v1.0.0  | Android client (Kotlin/Jetpack)    |

**v1.0.0 Baseline**: First stable release with context fields embedded in events. Breaking change from previous versions (context_id removed). Manual database migration required.

---

## 🔄 Update Policy
- All version updates must be reflected in:
  - `Cargo.toml` for each crate
  - [spec/README.md](../spec/README.md) (Version Map)
  - This registry file

---

## 📦 Build References
- CI/CD Artifacts: [CI_Workflows_Artifacts.md](CI_Workflows_Artifacts.md)
- Install Paths by OS: [Install_Paths_By_OS.md](Install_Paths_By_OS.md)
- Release Notes: [GitHub Releases](https://github.com/ekwator/truth-training/releases)

