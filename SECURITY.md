# 🔐 Security Policy

## 🔒 Privacy and Confidentiality

**Truth Training is built on the fundamental principle of confidentiality**: **No user actions are logged or persistently stored**. This is a core architectural requirement enforced across all platforms.

### Privacy Guarantees

- ✅ **No User Action Logging**: The application does not track, record, or save any user interactions, navigation patterns, clicks, or behavioral data
- ✅ **No Persistent User Tracking**: No identifiers, session data, or behavioral analytics are stored
- ✅ **No Telemetry Collection**: No user activity is transmitted or stored
- ✅ **Ephemeral Logs Only**: Only system-level logs (errors, sync operations) are temporarily stored for debugging purposes and are not linked to user actions
- ✅ **No User-Identifiable Data**: All stored data (events, judgments, contexts) is anonymous and cannot be traced back to individual users

### Enforcement

This confidentiality principle is:
- **Architecturally enforced**: Database schemas do not include tables for user action logging
- **Code-reviewed**: All contributions must maintain this principle
- **Platform-wide**: Applied consistently across Desktop UI, Android, Server, and CLI

**Violations of this principle will result in PR rejection.**

---

## 🧩 Supported Versions

Truth Training is currently under **active development** and published as an open-source experimental project.  
Security updates are provided for the **latest stable release** and the **development branch**.

| Component | Version | Supported | Notes |
| ---------- | -------- | ---------- | ----- |
| All (`master`) | v1.0.0 | ✅ Yes | Main desktop app/logic & Shared core library & ore library binary & Tauri-based desktop UI |
| Desktop UI | v0.1.3 | ❌ No | Tauri-based UI, production-ready builds |
| Core (`master`) | v0.4.2 | ❌ No | Desktop Integration & Cross-Platform Builds |
| Desktop UI | v0.1.2 | ❌ No | Tauri-based UI, production-ready builds |
| Core (`master`) | v0.3.x | ❌ No | Core stabilization & crypto verification |
| P2P | v0.2.x | ❌ No | Legacy P2P prototype |
| P2P | v0.2.x | ❌ No | Legacy P2P prototype |
| ? | < v0.2 | ❌ No | Deprecated research builds |

---

## 🧠 Security Model Overview

Truth Training is a **peer-to-peer knowledge network**, where every node:
- Signs outgoing messages using **Ed25519 digital signatures**;  
- Verifies incoming messages using **Ed25519 signature verification**;
- **Android JSON Verification**: Mobile clients must sign JSON payloads with Ed25519 for authentication;
- **P2P Message Verification**: All P2P sync messages include cryptographic signatures for authenticity;
- Verifies incoming messages using **public keys from peers**;  
- Does not rely on a centralized server or single point of trust.  

Main risks include:
- Misconfigured nodes or compromised peers;  
- Leaked private keys;  
- Injection of invalid sync data;  
- Replay or forgery attacks.

---

## 🧰 Development Security Guidelines

To ensure consistency, safety, and traceability in development, the following tools are **mandatory** for all contributors:

1. **Cursor AI IDE**  
   You must use Cursor AI IDE for all development work. This ensures inline type checking, automated code refactoring, and alignment with the project’s code-generation standards. It is not optional.

2. **Spec-Kit**  
   We require using [GitHub Spec-Kit](https://github.com/github/spec-kit) (installed in the `.cursor` folder of the project) to run the structured specification workflow. Spec-Kit is a toolkit for Spec-Driven Development, designed to help generate and maintain executable specifications. :contentReference[oaicite:0]{index=0}

   **Installation:**
   
   For this repository, Spec-Kit must be installed system-wide using one of the following methods:
   
   - **Manual installation:**
     ```bash
     uv tool install specify-cli --from git+https://github.com/github/spec-kit.git
     ```
   
   - **Automatic installation and update:**
     Use the provided initialization script:
     ```bash
     ./init-speckit.sh
     ```
     This script will check for Spec-Kit, prompt for automatic installation if missing, and set up the project environment.

   In our project, the following Spec-Kit slash commands (available after installation) must be used:

   - `/speckit.specify` — creates a GitHub PR with a new branch based on the specification  
   - `/speckit.plan` — creates, modifies, and executes a plan from a specification  
   - `/speckit.clarify` — verifies the consistency of plan items among themselves and with the specification  
   - `/speckit.plan` (without a prompt) — finalizes the plan after clarification  
   - `/speckit.task` — creates, modifies, and executes tasks defined in the plan  
   - `/speckit.implementation` — executes part of the specification  
   - `/speckit.implementation` (without arguments) — executes the entire specification

Failure to use Spec-Kit as described may result in misaligned development, unreviewable code, or divergence from project standards.  

### 📁 Spec-Kit Prompt Library (Developer Examples)

This project includes a dedicated directory with **real, working examples** of proper Spec-Kit usage.  
These files serve as canonical templates for creating correct `/speckit.specify`, `/speckit.plan`, `/speckit.task`, and `/speckit.implementation` requests.

All examples are stored in:
- **/docs/prompt/ekwator/**  


- 📁Folder structure:

```
docs/
└── prompt/
    └── ekwator/
        ├── specify1/
        │   ├── [spec1.md](spec1.md)
        │   └── plan/
        │       └── [plan1.md](plan1.md)
        └── specify2/
            └── plan/
```

#### 📌 What each file contains:

- **specify1/**  
- 📁 A first set of examples demonstrating variations of specifications and plans.

- 📄 [Complete template + real example of a `/speckit.specify` command for a development task.](https://github.com/ekwator/truth-training/blob/master/docs/prompt/ekwator/specify1/spec1.md)

- 🗂 [Example of a fully structured `/speckit.plan` request derived from the specification.](https://github.com/ekwator/truth-training/blob/master/docs/prompt/ekwator/specify1/plan/plan1.md)

- **specify2/**  
- 📁 A second set of examples demonstrating variations of specifications and plans.

#### 📘 Purpose of these examples
These examples are intended to:

- Preserve consistent Spec-Kit workflow quality across contributors.  
- Demonstrate correct formatting, structure, and writing style.  
- Provide “copy–adapt–apply” patterns for:
#### Core Commands

Essential commands for the Spec-Driven Development workflow:

| Command                  | Description                                                           |
|--------------------------|-----------------------------------------------------------------------|
| `/speckit.constitution`  | Create or update project governing principles and development guidelines |
| `/speckit.specify`       | Define what you want to build (requirements and user stories)        |
| `/speckit.plan`          | Create technical implementation plans with your chosen tech stack     |
| `/speckit.tasks`         | Generate actionable task lists for implementation                     |
| `/speckit.implement`     | Execute all tasks to build the feature according to the plan         |

#### Optional Commands

Additional commands for enhanced quality and validation:

| Command              | Description                                                           |
|----------------------|-----------------------------------------------------------------------|
| `/speckit.clarify`   | Clarify underspecified areas (recommended before `/speckit.plan`; formerly `/quizme`) |
| `/speckit.analyze`   | Cross-artifact consistency & coverage analysis (run after `/speckit.tasks`, before `/speckit.implement`) |
| `/speckit.checklist` | Generate custom quality checklists that validate requirements completeness, clarity, and consistency (like "unit tests for English") |

### Environment Variables

| Variable         | Description                                                                                    |
|------------------|------------------------------------------------------------------------------------------------|
| `SPECIFY_FEATURE` | Override feature detection for non-Git repositories. Set to the feature directory name (e.g., `001-photo-albums`) to work on a specific feature when not using Git branches.<br/>**Must be set in the context of the agent you're working with prior to using `/speckit.plan` or follow-up commands. |

---

## ✅ Security & Process Enforcement

- All specification and planning steps **must go through Spec-Kit** before implementation.  
- Any code committed without a corresponding Spec-Kit specification or plan will be considered non-compliant and may be rejected or reverted.  
- Using Spec-Kit improves traceability, helps prevent drift between design and implementation, and ensures that AI-assisted code generation remains aligned with project governance.

By enforcing Cursor AI IDE and Spec-Kit usage, we maintain a high standard of **predictable, safe, and auditable development** across all contributors.


1. **Never commit private keys or seed phrases.**  
2. Use **Result-based error handling** for all crypto/network ops.  
3. Run `cargo clippy`, `cargo fmt`, and `cargo audit` before PRs.  
4. Avoid `unsafe` code unless documented and reviewed.  
5. Sign all sync payloads with timestamps to prevent replay.  
6. All crypto/network PRs require manual review.

---

## 🧾 Reporting a Vulnerability

Report privately:
- 🐙 [GitHub Security Advisory](https://github.com/ekwator/truth-training/security/advisories/new)

Include:
- Detailed description  
- Proof of concept (if any)  
- Affected commits or versions  

You’ll receive acknowledgment **within 48 hours**,  
and verification or fix plan **within 7 days**.

---

## 🛠 Security Review and Testing

Before each release the following security and stability checks **must** be performed:

- ✅ Run `cargo audit` to identify vulnerable Rust dependencies  
- ✅ Manually review all new crates and Gradle dependencies  
- ✅ Test signature generation & Ed25519 verification routines  
- ✅ Run fuzz tests for all JSON serialization/deserialization paths  
- 🛡 Ensure that Feature Flags match the target platform matrix  
- 📦 Verify that SQLite migrations apply cleanly across all platforms  

---

## ⚠️ Important Notice: Android CI Testing Instability

GitHub-hosted Android emulators (AVD) have **significant limitations**:

- Extremely low and inconsistent CPU/MEM/I/O performance  
- Unstable system service initialization (Settings, Package Manager, Content Provider)  
- High flakiness during APK installation and split-package deployment  
- UTP (Unified Test Platform) automatically **skips tests** when AVD is not fully initialized  
- Performance/UI tests produce **non-deterministic results**  
- Test count may vary between runs (e.g., 96 → 107 tests)

Because of these limitations, **Android CI results must not be treated as authoritative** for release-blocking security checks.

### Why this happens
Even for identical source code and identical workflows, GitHub AVD may:

- fail to install split APKs (`install-create` errors)  
- skip tests requiring UI Thread or ContentProviders  
- silently filter out Room/Performance/UI tests  
- report test failures caused by emulator boot state  
- produce different SHA256 checksums for identical APKs (different timestamps)

These issues are **specific to GitHub-hosted virtual devices** and **do not occur on real Android hardware**.

### Mandatory Recommendations

To ensure testing is meaningful and secure:

1. **Always run a full test suite on a real Android device**  
   - Required before approving a release  
   - GitHub Actions results may only serve as a preliminary smoke test  

2. **Treat GitHub Android CI as "best effort", not deterministic**  
   - Failures caused by AVD instability must not block releases  
   - Non-deterministic test count is acceptable  

3. **Performance benchmarks must not run on CI**  
   - They provide invalid results on GitHub runners  
   - Must be executed only on real hardware  

4. **Security-sensitive tests must run locally or on a physical device**  
   - Ed25519 signature tests  
   - JNI integration  
   - P2P LAN discovery  
   - Database performance tests  

5. **Document all AVD-specific errors** in [docs/device_e2e_test_report.md](docs/device_e2e_test_report.md)

---

## 🧪 Secure Testing Workflow Summary

For each release:

### **1. Desktop / CLI / Server (Linux/Mac/Windows)**
- ✔ deterministic and stable
- ✔ GitHub CI tests are authoritative  
- ✔ all unit + integration + E2E tests must pass

### **2. Android**
- ❗ GitHub CI = smoke test only  
- ✔ Real-device testing is mandatory  
- ✔ JNI + P2P + DB tests must be validated on hardware  
- ✔ Performance tests only on real phone

### **3. Cross-Platform**
- ✔ Validate JSON schema compatibility  
- ✔ Validate DB migrations across all platforms  
- ✔ Validate signature and trust graph consistency  

---

This ensures that **Truth Training** maintains strong security guarantees while acknowledging the limitations of virtual testing environments on GitHub.

---

## ⚖️ Legal & Ethical Notice

Truth Training is an **educational, research-oriented project**.  
Do **not** use it for:
- Surveillance or disinformation  
- Unauthorized data collection  
- Any illegal activity  

Use under the **MIT License**, following the project’s ethical guidelines.
