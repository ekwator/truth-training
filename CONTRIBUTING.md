# 🤝 Contributing to Truth Training

Thank you for your interest in improving **Truth Training — The Network of Anonymous Trust**.  
This document explains **how to contribute correctly**, which **tools are mandatory**, and which **quality requirements** must be met for a Pull Request to be reviewed and accepted.

---

**⚠️ Attention: all requirements of this manual when using this repository are fulfilled automatically when using Cursor AI IDE !!!**

---

## 🧰 Required Tools and Workflow

The following tools are **mandatory** for all contributors:

### 1. Cursor AI IDE (required)

All changes must be developed using **Cursor AI IDE**:

- Inline type checking and type hints  
- Automated refactoring and formatting  
- Integrated AI assistant for safe code edits  
- Preparation of Spec-Kit prompts and structured task analysis  

> Even if changes are made manually, your PR must still follow the standards expected from work performed with Cursor + Spec-Kit.

---

### 2. Spec-Kit (required)

We use [GitHub Spec-Kit](https://github.com/github/spec-kit), installed in the directory:

```
.cursor/
```

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

Spec-Kit provides a **spec-driven development workflow**, ensuring that all work is planned, validated, and implemented consistently.
# 📝 Spec-Driven Development Workflow

Truth Training uses a **strict spec-driven development workflow** enforced by Cursor AI + Spec-Kit.  
All changes must follow this sequence:

---

## 1. `/speckit.specify` — Define the task

Every contribution must begin with a **specification** describing:

- The goal of the change  
- Constraints and limitations  
- Affected modules (Core, Server, Desktop UI, Android, CLI)  
- Acceptance criteria  
- Security implications  

The output is stored in:

```
docs/prompt/<author>/[specifyN/specN.md](specifyN/specN.md)
```

---

## 2. `/speckit.plan` — Create an implementation plan

Each specification must include a **plan folder**:

```
docs/prompt/<author>/[specifyN/plan/planN.md](specifyN/plan/planN.md)
```

The plan describes:

- Implementation steps  
- Data structures and API touchpoints  
- Testing strategy  
- Potential risks  
- Required refactoring  

---

## 3. `/speckit.task` — Developer tasks

Spec-Kit converts the plan into **atomic tasks** that can be implemented safely.  
Every task must be small, testable, and reversible.

---

## 4. `/speckit.implementation` — Apply changes

Only after the plan is approved can implementation proceed.  
Cursor AI IDE applies changes automatically or semi-automatically, ensuring:

- Correct formatting  
- Type safety  
- Spec compliance  
- No accidental edits beyond task scope  

---

📌 **Important:**  
A Pull Request **cannot be reviewed** unless it includes:

- A `/speckit.specify` file  
- A `/speckit.plan` file  
- A full task chain  
- A summary of changes linked to Spec-Kit  
# 🔐 Security Requirements for Contributors

Security is a core principle of Truth Training.  
Every contribution must maintain or improve security guarantees of:

- the Core (Rust),
- the Server,
- Desktop UI,
- Android Client,
- Networking layer,
- Cryptographic operations.

Below are the **mandatory security rules** for all contributors.

---

## 1. Mandatory Security Checks Before Submitting a PR

Every PR must include:

### ✅ `cargo audit`
Run in the project root and in any Rust sub-crates:

```sh
cargo audit
```

You must fix or justify all:

- vulnerable dependencies  
- unsound crates  
- security advisories  
- memory safety warnings  

---

### ✅ Dependency Security Review

Before submitting a PR:

- Check all new dependencies for maintenance status  
- Avoid crates that:
  - are unmaintained,
  - have open RUSTSEC advisories,
  - depend on outdated crypto.

- JavaScript packages must pass:
  - `npm audit`
  - `npm outdated`
  - review for known supply-chain attacks

- Android dependencies must be:
  - API 24+ compatible,
  - free of reflection-based payloads,
  - from trusted repositories (MavenCentral only).

---

### ✅ Cryptography Verification

Any code touching cryptography must follow these rules:

- Only approved algorithms:
  - `Ed25519` for signatures
  - `ChaCha20-Poly1305` for symmetric encryption (Core)
- Never implement custom crypto
- Never reuse nonces
- Never log private keys
- Key operations must pass "signature → verification" cycle tests

---

### ✅ Fuzz Testing (Core + Serialization)

Before merging any change that touches:

- event models  
- context templates  
- serialization/deserialization  
- networking structures  

You must run fuzz tests:

```sh
cargo fuzz run serialize_fuzzer
cargo fuzz run api_fuzzer
```

Any panic, UB, or malformed behavior blocks the PR.

---

### ✅ GitHub Actions Security Review

Your PR must not disable or weaken:

- branch protection rules  
- required CI checks  
- test coverage minimums  
- artifact signing  
- dependency caching verification

---

## 2. Mandatory Test Requirements

Every PR must pass:

### Core (Rust)
- Unit tests for:
  - signature verification  
  - storage schema initialization  
  - graph traversal  
  - consensus  
  - expert heuristics  
- Integration tests for:
  - REST API  
  - context engine  
  - event lifecycle  

### Desktop UI
- Jest unit tests  
- React integration tests  
- Playwright E2E tests  
- Offline queue tests  
- UI performance tests (<200ms navigation, <100ms pagination)

### Android
Because GitHub emulators are unstable:

- Both runs of CI must pass (initial + re-triggered)  
- Performance tests must be reviewed manually if emulator fails  
- Local testing on **physical device** is required for PR approval  
- Tests showing emulator installation errors must be explained in PR description

---

## 3. Security Logging Requirements

All new code must comply with logging rules:

- No sensitive data in logs  
- No private keys, tokens, context fingerprints  
- Network logs must be redacted  
- Desktop and Android logs must stay below 50 lines per operation  
- Rust logs must use structured logging:

```rust
tracing::info!(event_id, "Event created");
```

---

## 4. PR Security Checklist

Every PR must include a “Security Review” section confirming:

- [ ] `cargo audit` passed  
- [ ] Dependencies reviewed  
- [ ] No unsafe code added (or justified)  
- [ ] Fuzzers executed  
- [ ] Logs sanitized  
- [ ] CI tests passed (both runs for Android)  
- [ ] Cryptographic checks passed  
- [ ] No new attack surface introduced  
- [ ] Spec-Kit chain included  

---

📌 **Pull Requests missing any of the above will not be reviewed.**

# 🛠 Development Workflow

Truth Training uses a strict, deterministic, and auditable development workflow.  
Every contribution must follow this process without exceptions.

---

## 1. Branching Rules

All contributors **must** follow this branching structure:

```
main              — stable, production-ready
develop           — integration branch for upcoming releases
feature/<name>    — new features
fix/<name>        — bug fixes
spec/<id>-<name>  — branches created automatically by Spec-Kit
```

### ❗ Never commit directly into:
- `main`
- `develop`
- release branches (e.g., `release/1.0.0`)

These branches are protected.

---

## 2. Commit Requirements

All commits must:

- Use English
- Be atomic (one logical change per commit)
- Have deterministic formatting
- Contain no generated files unless explicitly allowed
- Pass formatting and linting before being pushed

### Commit message format:

```
<type>(scope): <description>

[optional details]
```

Example:

```
feat(core): add embedded context validation
```

Types:

- `feat` — new feature  
- `fix` — bug fix  
- `refactor` — code change without behavior modification  
- `docs` — documentation updates  
- `test` — adding or updating tests  
- `perf` — performance improvements  
- `chore` — maintenance tasks  
- `build` — CI, build system, dependencies  

---

## 3. Required Local Checks Before Push

Before pushing, contributors must run these commands:

### Rust (Core + Server)
```sh
cargo fmt --all
cargo clippy --all-targets --all-features -- -D warnings
cargo test --all
```

### Desktop UI
```sh
npm run format
npm run lint
npm run test
npm run test:e2e
```

### Android
```sh
./gradlew lint
./gradlew test
./gradlew connectedDebugAndroidTest   # physical device strongly recommended
```

Push is allowed only when all of the above succeed.

---

## 4.1 Documentation Refactor Automation

The v1.0.0 release ships with an automated documentation pipeline (`scripts/doc_refactor/`) that enforces README hierarchy, `/docs` depth, `/spec` compression, link integrity, and archive hygiene. Contributors touching Markdown must validate their changes with these tools before opening a PR:

1. **Bootstrap the environment**
   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   ```
2. **Targeted phase runs**
   - Inventory + link validation: `python scripts/doc_refactor/main.py run --phases inventory,link_discovery,validation`
   - README / archive restructuring: `python scripts/doc_refactor/main.py run --phases restructuring`
   - Spec optimization pass: `python scripts/doc_refactor/main.py run --phases spec_opt`
3. **End-to-end verification**
   ```bash
   make doc-refactor-run   # executes --phases all
   make doc-refactor-test  # runs pytest suite under scripts/doc_refactor/tests
   ```

Reports (inventory, link graph, validation, restructure, spec_opt, dedupe, run_summary) are written to `reports/doc_refactor/…`. Scenario details and acceptance criteria live in [specs/010-documentation-refactoring-0-0/quickstart.md](specs/010-documentation-refactoring-0-0/quickstart.md); follow Scenario 6 before submitting documentation-heavy PRs.

---

## 4. Pull Request Requirements

Every PR must include the following sections:

### PR Template (mandatory)
```
## Summary
Short description of the change.

## Spec-Kit Chain
/speckit.specify link  
/speckit.plan link  
/speckit.task or /speckit.implementation link  

## Testing
- Local tests passed
- CI tests passed
- Android tests passed on physical device (if applicable)

## Security
- cargo audit ✔
- Dependencies reviewed ✔
- No insecure code added ✔

## Notes
Anything reviewers must be aware of.
```

### PR Acceptance Rules

A PR will be reviewed **only if**:

- Spec-Kit chain is present  
- CI is green  
- Static analysis is clean  
- Tests pass on both:
  - CI emulator
  - a real Android device (if Android-related)  
- Changes follow architecture rules  
- Commit history is clean  
- Formatting and lint rules are respected  

Otherwise, the PR is auto-rejected.

---

## 5. Code Style Rules

### Rust

- Follow `rustfmt` defaults
- No `unsafe` unless reviewed and justified
- All public functions must be documented
- No panics in production code

### TypeScript / React

- Use functional components
- Use Zustand for state
- No mutable state
- No inline CSS (Tailwind only)
- Use React Testing Library for UI tests

### Kotlin / Android

- Use MVVM
- Use coroutines (no RxJava)
- No global mutable state
- All network calls must be typed
- Logging must be minimal and sanitized

---

## 6. Review Process

A PR requires:

- 1 approval for minor changes  
- 2 approvals for Core, networking, cryptography, or consensus code  
- Approval from maintainers of the affected subsystem  
- Passing cross-platform CI:
  - Linux
  - Windows
  - macOS
  - Android
  - Desktop UI

Reviewers may request:

- more tests  
- security justification  
- architectural diagrams  
- Spec-Kit clarification runs  

The contributor must respond within **7 days**, otherwise PR is closed.

---

## 7. Merge Rules

A PR is allowed to merge only when:

- All checks are green  
- Reviewers approve  
- No merge conflicts  
- Spec-Kit `implementation` stage is complete  

Merges to `main` also require:

- Version bump (Core/UI/Android)  
- GitHub release notes updated  
- Changelog updated  

---

## 8. Forbidden Actions

The following actions are strictly prohibited:

- Direct commits to protected branches  
- Bypassing CI  
- Disabling tests or linters  
- Adding dependencies without review  
- Introducing logging of sensitive data  
- Making API changes without updating specs  
- Introducing custom cryptography  
- Force-pushing shared branches  
- Using AI tools that rewrite code unpredictably  
- Leaving TODOs in production code  

Violations result in PR closure and possibly revocation of contributor rights.

---

## 9. Contributor Responsibilities

Contributors must:

- Follow the architecture  
- Maintain backwards compatibility  
- Keep code deterministic  
- Ensure the system remains cryptographically correct  
- Test changes on all supported platforms  
- Keep documentation up to date  
- Maintain Spec-Kit specifications for every feature  

---

📌 *This workflow ensures Truth Training remains verifiable, safe, deterministic, and scalable across platforms.*

# 🔬 Testing Requirements

Every contribution must include **complete, reproducible, automated tests**.  
Truth Training follows a “zero untested logic” rule.

Tests are mandatory for:

- Core logic  
- API handlers  
- Consensus and judgment calculations  
- Database migrations  
- Context Template matching  
- Desktop UI workflows  
- Android flows (where applicable)  

---

## 1. Rust Core & Server Tests

All Rust components must include:

### ✅ Unit Tests
- Signature verification  
- Event model validation  
- Context Template creation & matching  
- Judgment scoring  
- Graph integrity checks  
- Serialization/deserialization (serde)  

### ✅ Integration Tests
- REST API endpoints  
- Storage (SQLite/Postgres)  
- Node sync behavior  
- P2P logic (feature-flagged)

### ✅ Fuzz Testing
Required for:
- event serialization  
- template matching  
- consensus scores  
- signature verification  

Command:

```sh
cargo fuzz run fuzz_target
```

(At minimum fuzzing must run locally before PR submission.)

### ✅ Property-Based Tests (optional but recommended)

---

## 2. Desktop UI Tests

For React/Tauri UI, contributors must provide:

### Unit Tests
- Component rendering  
- Validation logic  
- Stores (Zustand)  
- API service mocks  

Frameworks:
```text
Jest + React Testing Library
```

### Integration Tests
- Create Event flow  
- Template matching flow  
- Context Editor  
- Judgments UI  
- Offline queue logic  

### E2E Tests
Using Playwright:

```sh
npm run test:e2e
```

Must run successfully in CI.

---

## 3. Android Tests

Android tests must be written and run using:

- **JUnit**
- **Espresso**
- **UIAutomator** (optional)
- **Macrobenchmark / Microbenchmark** (for performance)

### Required:

#### Unit Tests
- ViewModels  
- Repositories  
- DTO validation  
- Retrofit service mocks  

#### Instrumentation Tests
Run on:
- physical Android device (mandatory for PR)  
- emulator in CI (expected to occasionally fail — see [SECURITY.md](SECURITY.md) warnings)

#### Performance Benchmarks
We require:
- Navigation performance tests  
- Cold start/warm start rendering  
- Database write/read benchmarks  
- UI response time tests  

These must not regress beyond thresholds.

---

## 4. Cross-Platform Consistency Tests

Because Truth Training runs on:

- Rust Core
- Desktop UI
- Android
- iOS (future)
- CLI tools

Every PR must pass **Cross-Platform Test Matrix**:

| Platform | Test Suite |
|---------|------------|
| Linux   | Core + Desktop |
| macOS   | Core + Desktop |
| Windows | Desktop |
| Android | Unit + Instrumented |
| Server  | Core integration |

CI will fail if any platform regresses.

---

## 5. Required Before Merging

A PR **cannot** be merged unless all of the following conditions are true:

### ✔ All tests pass locally  
### ✔ All tests pass in CI  
### ✔ No performance regressions  
### ✔ No new flakiness introduced  
### ✔ No skipped tests (`@Ignore`, `.skip`)  
### ✔ No commented-out test blocks  
### ✔ Full coverage for new logic  

---

## 6. Test Coverage Requirements

Minimum coverage thresholds:

| Component | Coverage |
|----------|----------|
| Rust Core | ≥ 90% |
| Desktop UI | ≥ 85% |
| Android | ≥ 70% (unit + instrumentation combined) |
| Server API | ≥ 90% |

Coverage below thresholds requires a justification and maintainer approval.

---

## 7. Test Artifact Requirements

Each CI run must produce:

- HTML reports  
- junit.xml files  
- benchmark results  
- performance graphs (desktop)  
- APK benchmarks (Android)

Artifacts are required for reviewing regressions.

---

## 8. Handling Flaky Tests

If a test is flaky:

1. Open an issue  
2. Add label: `flaky-test`  
3. Assign responsible maintainer  
4. Provide:
   - screenshot or HTML report  
   - logs  
   - reproduction steps  

Tests **cannot** be disabled without explicit approval.

---

## 9. Performance Regression Policy

A PR is **blocked** if:

- Navigation takes > 200 ms on desktop  
- Pagination > 100 ms  
- Android cold start > 1.3 s  
- Android interaction tests show degradation  
- Database operations exceed thresholds  
- Graph calculations slow down (Core)  

Performance is a first-class requirement.

---

## 10. Security Testing Requirements

Each PR must include:

```sh
cargo audit
```

and review:

- crypto routines  
- signature flows  
- serialization safety  
- secrets exposure  
- dependency vulnerabilities  

For Android:
- network security config  
- keystore usage  
- no plaintext sensitive logs  

For Desktop:
- CSP header validation  
- IPC channel safety  
- no exposed debug endpoints  

---

📌 *Testing is the backbone of Truth Training’s security and reliability.  
No feature is considered complete without a full automated test suite.*

# 🧪 Cross-Device, Cross-Platform, and Real-World Testing

Truth Training is a distributed, multi-device ecosystem.  
To maintain consistency and security across all environments, contributors must validate their changes on **real hardware**, not just emulators.

This section describes the mandatory verification steps.

---

## 1. Real-Device Testing Requirements

Before submitting a PR, you must test on:

### ✔ At least one physical Android device  
Required for:
- JNI → Kotlin interactions  
- Network behavior (LAN discovery, NSD)  
- Signature verification and push API  
- Performance benchmarks  
- Runtime stability (ANRs, memory issues)

Testing only on the emulator is insufficient.

### ✔ Desktop builds on real OS environments  
Required on at least:
- Linux (primary platform)
- Windows or macOS (one of them)

These ensure:

- Tauri Rust backend works correctly  
- SQLite migrations run cleanly  
- IPC channels behave consistently  
- Browser engines don’t introduce visual/UI regressions  

Snapshots should be compared where necessary.

---

## 2. Cross-Platform Behavior Validation

Before merging, confirm that:

### Rust Core:
- Builds and runs on Linux, Windows, macOS  
- Exposes consistent FFI interface for Desktop and Android  
- API schemas match across platforms  
- JSON outputs are identical for the same inputs

### Desktop UI:
- Uses the correct Core API feature flags (`desktop`)  
- Reflects identical behavior to CLI & server  
- No missing template fields  
- No client-side interpretation differences

### Android Client:
- JSON models match Desktop and Server  
- JNI layer returns correct pointers and free-functions  
- Retrofit model definitions match server schemas  

> **Cross-platform drift is not allowed.**  
> Any PR that introduces API differences will be rejected until fixed.

---

## 3. Cross-Device E2E Testing

Contributors must verify:

### 🔗 LAN Discovery Consistency
- NSD discovery works across multiple phones  
- Peer lists match across devices  
- Multi-device ping/echo tests succeed

### 🌐 Server Sync Consistency
- All devices (Android, Desktop, CLI) receive the same data  
- No platform diverges in event interpretation  
- Consensus scoring is identical across devices

### ⚙️ Real-World Network Scenarios
Test at least one of the following:

- Home Wi-Fi with multiple routers  
- Hotspot mode  
- Mobile 4G/5G  
- DNS relay / CGNAT environment  

These scenarios often expose:
- Timeout differences  
- JSON mismatch  
- Erroneous parsing  
- Fragmentation issues  
- Date/locale bugs  

---

## 4. Reliability Testing

Each PR must validate:

### ✔ Backward compatibility  
Old clients must not crash after server or Core updates.

### ✔ Edge cases  
- Empty lists  
- Huge payloads  
- Long strings  
- Missing fields  
- Corrupted packets  
- Mismatched signatures  
- Network delays  

### ✔ Long-running stability  
Test that the application runs for **at least 20 minutes** under:

- Repeated sync  
- Tab switching  
- Navigation workflows  
- Frequent Core calls  

Memory leaks or crashes must be fixed before submitting PR.

---

## 5. Testing Against a Clean Install

Every PR must validate:

### Mandatory:
- Clean install  
- Clean database  
- No leftover config from previous runs  

### Confirm:
- Schema migration runs cleanly  
- Default config files generate properly  
- No unsafe default settings  

---

## 6. External Dependencies

If your change affects:

- Networking  
- Storage  
- Encryption  
- Shared libraries  
- Tauri commands  
- JNI bindings  

…you must test:

### ✔ Connectivity on local network  
### ✔ Connectivity through the internet  
### ✔ Storage on real filesystem  
### ✔ Signature validation across devices  
### ✔ Interactions with Rust Core  

> In distributed systems, **cross-device behavior matters more than unit tests**.  
> This is a critical requirement for Truth Training’s security guarantees.

---

## 7. Required Artifacts for Review

When submitting a PR, attach:

- Local test reports  
- Device model used  
- OS versions  
- APK build hash  
- Desktop build hash  
- Cross-device sync screenshots (optional but useful)  
- Benchmark summaries  

These help maintainers reproduce issues.

---

## 8. What Happens If Tests Fail on GitHub?

Due to GitHub’s limited runner resources:

- Android tests may fail due to emulator instability  
- Benchmark tests may be inconsistent  
- First run may differ from second run  
- Split-APK installation may randomly fail  

Therefore:

### ✔ PRs must pass local + real device tests  
### ✔ CI failures must be analyzed but not always block merging  
### ✔ Multi-run comparison is used to detect consistent issues  

> CI is not a substitute for real-device testing —  
> it is a *supplement*.

---

📌 *Cross-device consistency is one of the core security properties of Truth Training.  
PRs that break multi-device behavior cannot be merged.*

# 🔐 Cryptography, Identity, and Trust Model Requirements

Truth Training is a distributed system where *truth flows without identity*, yet cryptographic guarantees must remain absolute.  
This section defines the rules for handling cryptographic materials, signatures, and trust-model constraints.

Every contributor must follow these requirements strictly.

---

## 1. Key Management Rules

### ✔ No personal or developer keys in the repository  
It is **forbidden** to commit:

- Private keys  
- JWT tokens  
- Debug keystores  
- Sample signing keys  
- Exported Android signing configs  
- Generated Rust keypairs used during development  

All such materials must stay local and excluded via `.gitignore`.

### ✔ Test keys must be generated per-run  
For tests:

- Rust: generate ephemeral Ed25519 keypairs  
- Android: generate in-memory BouncyCastle keys  
- Desktop: generate ephemeral browser-side keys  

**Never reuse the same keypair in tests.**  
This prevents hidden assumptions about cryptographic state.

---

## 2. Signature Rules

All signatures must follow these principles:

### ✔ Ed25519 everywhere  
Truth Training uses **Ed25519** across all platforms:

- Rust Core  
- Desktop UI  
- Android Client  

There may be no divergence in signature formats.

### ✔ JSON signature formats must match across ecosystems  
A signed message always contains:

```json
{
  "payload": "...",
  "signature": "base64...",
  "public_key": "base64..."
}
```

Any PR that changes field names or ordering must include:

- Full compatibility review  
- Cross-platform tests  
- API contract updates  

### ✔ Deterministic signing only  
Randomized or context-dependent signing is prohibited.

### ✔ Canonical JSON required  
Before signing:

- No trailing commas  
- Stable field ordering  
- No platform-specific serialization quirks  

Rust, Android, and Desktop must produce *the same byte sequence* from the same JSON input.

---

## 3. Identity and Privacy Model

Truth Training operates under the principle:

> **No bans, no accounts, no identity — only truth flow.**

Therefore:

### ✔ No persistent identifiers  
Do not introduce:

- User IDs  
- Phone numbers  
- Emails  
- Biometric data  
- Stable device IDs  

The only allowed identifiers:

- **Ephemeral cryptographic keys**  
- **Session identifiers**  
- **Node runtime IDs** (resettable)

### ✔ No IP-based or MAC-based tracking  
Forbidden:

- Logging IPs  
- Persisting MAC addresses  
- Fingerprinting devices  

### ✔ No analytics, no telemetry  
Contributors must not add:

- Crash analytics (Firebase, Sentry, etc.)  
- Tracking SDKs  
- Hidden logs with user metadata  

---

## 4. Trust Graph Requirements

The Trust Graph is central to the project.

Every PR must ensure:

### ✔ Graph consistency across all platforms  
Rust Core must produce the same:

- Hashes  
- Scores  
- Node degrees  
- Weight propagation  

Android and Desktop must not alter graph logic on the client side.

### ✔ No platform-specific deviations  
Forbidden:

- Changing trust scoring rules in Android UI  
- Adding hidden heuristics  
- Adjusting weights differently on Desktop  

All trust evaluation logic lives **only in Rust Core**.

---

## 5. Secure Serialization Rules

Before merging a PR, contributors must validate:

### ✔ Serialization is stable across platforms  
Rust → Android → Desktop conversions must be reversible.

### ✔ Embedded fields must match  
For example:

- `category_id`  
- `forma_id`  
- `cause_id`  
- `develop_id`  
- `effect_id`  

These must be identical in:

- Rust structs  
- Kotlin data classes  
- TypeScript interfaces  

### ✔ No silent schema changes  
Any change to:

- JSON field names  
- Types  
- Nullability  
- Enum variants  

must go through:

- `/speckit.specify`  
- `/speckit.plan`  
- `/speckit.clarify`  
- schema sync review  

---

## 6. Randomness Requirements

All randomness must come from:

### ✔ Rust: `rand::rngs::OsRng`  
### ✔ Android: `SecureRandom()`  
### ✔ Desktop: `window.crypto.getRandomValues()`  

Forbidden:

- `Random()`  
- Pseudo-random generators without OS entropy  
- Reusing seeds  

---

## 7. Storage and Key Security

### ✔ Desktop  
Keys must be stored in:

- OS-keystore if available  
- Otherwise: encrypted file in user directory  

Never plain-text.

### ✔ Android  
Keys must be stored in:

- Android Keystore  
- Or encrypted SharedPreferences with AES-GCM  

Plain-text is prohibited.

### ✔ Rust CLI/Server  
Development keys allowed only if:

- Marked explicitly test-only  
- Never distributed  
- Never used for production  

---

## 8. Explicit Prohibitions

These will result in PR rejection:

❌ Storing user identifiers  
❌ Using non-canonical JSON for signing  
❌ Adding device analytics or telemetry  
❌ Using insecure randomness  
❌ Introducing SHA-1 or MD5  
❌ Using AES-CBC without authentication  
❌ Adding platform-specific trust heuristics  
❌ Altering cryptography without a Spec-Kit specification  
❌ Adding new enums without updating cross-platform compatibility tests  

---

## 9. Developer Checklist (Mandatory)

Before submitting a PR:

- [ ] Signatures validated on at least two devices  
- [ ] JSON serialization tested on all platforms  
- [ ] Keys generated ephemerally in tests  
- [ ] No plaintext key material in repo  
- [ ] Trust graph outputs identical across OSes  
- [ ] No identity-leaking side channels  
- [ ] Spec-Kit workflow used for schema changes  
- [ ] Security review completed  
- [ ] Cross-platform compatibility confirmed  

---

🔐 *Truth Training protects anonymity and ensures authenticity —  
every contributor must treat these guarantees as fundamental.*  

# ☁️ Cross-Platform Architecture and Platform-Parity Requirements

Truth Training is implemented across four platforms:

- **Rust Core** (canonical reference implementation)  
- **Desktop UI (React + Tauri)**  
- **Android Client (Kotlin + JNI)**  
- **CLI Tools and Server**  

This section defines the **rules for maintaining platform parity**, **architecture boundaries**, and **cross-device consistency**.

Every contributor working on multi-platform logic must follow these requirements.

---

## 1. The Rust Core Is the Source of Truth

### ✔ All business logic must originate in Rust Core  
This includes:

- Event validation  
- Context template logic  
- Trust scoring  
- Consensus and judgments  
- Graph operations  
- TTL and cleanup  
- Serialization formats  
- Signature verification  

### ✔ No platform may override or re-implement logic  
Forbidden:

- Rewriting trust scoring in Kotlin  
- Altering JSON schema in TypeScript  
- Adding “quick fixes” to UI layers  
- Adding Android-only heuristics  

All platforms must call the same Rust-implemented functions.

---

## 2. Strict Feature Flag Discipline

Truth Core supports selective builds using feature flags:

- `desktop`  
- `mobile`  
- `p2p-client-sync`  
- `server`  
- `tests`  

Requirements:

### ✔ Every feature must be documented  
Before merging:

- Provide docs in `/docs/core/`  
- Add compatibility notes in README  
- Run cross-platform tests with and without the flag  

### ✔ No hidden coupling  
Do **not** rely on a feature flag implicitly being enabled.

### ✔ Mobile features must remain lightweight  
Android JNI builds must exclude:

- Heavy consensus code  
- Experimental graph traversal  
- Full offline DB logic  

Unless explicitly planned.

---

## 3. API Compatibility Rules (v1.0.0 Standard)

All platforms must implement the same API structures.

### ✔ Required data model equivalence  
For:

- Events  
- Context templates  
- Embedded fields  
- Trust graph nodes  
- Judgments  

Kotlin `data class`, TypeScript `interface`, and Rust `struct` definitions must match **exactly**.

### ✔ Enum variants must stay in sync  
If Rust changes:

```rust
enum NodeSource { Lan, Wifi, Global }
```

Kotlin and TypeScript equivalents must be updated before merge.

### ✔ Endpoints must maintain semantic parity  
If a new endpoint is added in Rust:

- Desktop must integrate  
- Android must integrate  
- CLI must reflect it  
- Specs must be updated  

---

## 4. Platform-Specific Behavior (Allowed and Forbidden)

### ✔ Allowed platform differences:

- UI layout  
- Navigation structure  
- Local storage paths  
- OS-specific background worker logic  
- OS networking stack differences  
- JVM memory limitations  

### ❌ Forbidden differences:

- Inconsistent JSON serialization  
- Unique trust or consensus rules  
- Different validation logic  
- Diverging cryptography  
- Platform-specific business features  

Platform UX may differ.  
Platform logic **must not differ**.

---

## 5. JNI and FFI Requirements (Android + Desktop)

### ✔ Memory ownership rules must be followed  
Rust → Kotlin:

- Strings must be freed via `free_string(ptr)`
- No leaking pointers  
- No returning owned references to static data  

Rust → TypeScript via Tauri:

- FFI commands must remain async  
- No blocking Rust code in UI thread  

### ✔ Kotlin/TypeScript must not reinterpret data  
Never parse Rust binary structures manually.

Always pass JSON.

---

## 6. Cross-Platform Testing Requirements

Before merging logic that affects multiple platforms:

### Mandatory tests:

- ✔ Rust unit tests  
- ✔ Android instrumentation tests  
- ✔ Desktop E2E tests  
- ✔ Cross-device P2P sync tests  
- ✔ JSON schema equivalence tests  
- ✔ Enum compatibility tests  

### Required tools:

- GitHub Actions  
- Local Android device or emulator  
- Desktop Playwright test suite  
- Rust CLI integration tests  

A PR may be rejected if:

- Only one platform was tested  
- Tests ran only on desktop  
- API compatibility tests were skipped  

---

## 7. Cross-Device Sync Requirements

Truth Training supports:

- LAN discovery  
- UDP multicast  
- HTTP registry polling  
- Cross-node incremental sync  

### ✔ All sync rules must match Rust Core  
Android and Desktop must use:

- identical TTL  
- identical cleanup  
- identical ping formats  
- identical announcements  
- identical incremental sync formats  

### ✔ No “platform-local optimizations”  
Forbidden:

- Custom caching rules  
- Altered sync intervals  
- Modified retry behavior  

---

## 8. Compatibility Matrix (Must Be Updated)

Every PR touching logic must update:

[docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md)

This ensures:

- Platform capability transparency  
- API parity is maintained  
- Discovery behavior stays consistent  
- Sync logic is unified  

---

## 9. Required Developer Checklist (Cross-Platform)

Before submitting a PR:

- [ ] Rust Core updated first  
- [ ] Desktop UI integrated  
- [ ] Android JNI bindings updated  
- [ ] CLI compatibility tested  
- [ ] JSON schema validated  
- [ ] Enum parity confirmed  
- [ ] Cross-platform tests passed  
- [ ] Sync logic tested on two platforms  
- [ ] Docs updated in `/docs/cross_platform*`  
- [ ] Spec-Kit plan approved  

---

🌐 *Keeping platforms unified ensures that Truth Training behaves consistently across every device and every environment.*

# 📦 Versioning, Releases, and Tagging Rules

This section defines the **mandatory rules** for versioning, creating releases, tagging commits, and preparing publish-ready artifacts across all platforms (Core, Desktop, Android, Server, CLI).

These rules ensure that releases are **reproducible**, **auditable**, and **cryptographically trustworthy**, without accidental drift between environments.

---

## 1. Semantic Versioning (Required)

Truth Training uses **strict Semantic Versioning**:

```
MAJOR.MINOR.PATCH
```

### ✔ MAJOR (breaking changes)
Increased when:

- API schema changes  
- JSON fields are renamed or removed  
- ABI or FFI boundaries change  
- Trust-scoring rules change  
- Discovery or sync protocol changes  

### ✔ MINOR (new features, backward-compatible)
Includes:

- Adding new API endpoints  
- New UI screens  
- Performance improvements  
- Additional discovery strategies  

### ✔ PATCH (bug fixes)
Includes:

- Fixing crashes  
- Correcting JSON serialization  
- UI fixes  
- Documentation updates  

---

## 2. Release Preparation Requirements

Before a release can be prepared:

### ✔ All tests must pass
- Rust tests  
- Desktop E2E tests  
- Android device & instrumentation tests  
- CLI integration tests  
- Cross-platform compatibility tests  
- Sync consistency tests  

### ✔ No compiler warnings
All components must build with **zero warnings**.

### ✔ Version numbers must match across platforms
Required updates:

- `Cargo.toml`  
- `app/build.gradle.kts`  
- `package.json`  
- `tauri.conf.json`  
- `truthctl --version` output  
- [docs/VERSION_REGISTRY.md](docs/VERSION_REGISTRY.md)  
- [spec/README.md](spec/README.md) *(required by `.github/workflows/desktop.yml` version gate)*  
- [docs/UI_Desktop.md](docs/UI_Desktop.md) *(must mention `Core/Server vX.Y.Z` exactly as in tags)*  

### ✔ Release notes must describe **features**, not tasks

The file [release-info.txt](release-info.txt) must be filled out with release information **before** running the release script. The file must contain:

- Release tag on the first line (e.g., `v1.0.0-Release`)
- Name release on the second line
- Summary of the changes  
- Highlights of major improvements  
- Security impact  
- Platform compatibility notes  

**Task IDs or internal Spec-Kit logs are not allowed in release notes.**

**After the release script completes**, rename `release-info.txt` to `release-info-vX_Y_Z.txt` (replace `X_Y_Z` with the version number using underscores, e.g., `release-info-v1_0_0.txt`). This versioned file is then referenced in [CHANGELOG.md](CHANGELOG.md).

---

## 3. Mandatory Release Script Usage

**Release workflow steps:**

1. Fill out [release-info.txt](release-info.txt) with the release tag (first line), release name (second line), and release information
2. Run the release script: [create-release.sh](create-release.sh)
3. After the script completes, rename `release-info.txt` to `release-info-vX_Y_Z.txt` (e.g., `release-info-v1_0_0.txt`)
4. Update [CHANGELOG.md](CHANGELOG.md) to reference the versioned release-info file

The release script:

- Reads the release tag from the first line of [release-info.txt](release-info.txt)  
- Builds Rust Core  
- Builds Desktop UI installers  
- Builds Android APK/AAB  
- Runs all tests  
- Packages artifacts  
- Pushes Git tags  
- Uploads artifacts to GitHub Releases  

### 🚫 Forbidden:  
- Manually pushing tags  
- Manually uploading release artifacts  
- Bypassing the release script  

Reproducibility must be guaranteed.

---

## 4. Tagging Rules

### ✔ Tag must match [release-info.txt](release-info.txt) 
Example:

- `v1.0.0-Release`  
- `v1.1.0-Beta`  
- `v1.0.1-Patch1`  



### ✔ Tags must be immutable
After publishing:

- Tags cannot be moved
- Tags cannot be deleted
- Releases cannot be modified retroactively

If a tag was added accidentally, without tag-based workflows enabled, simply delete the tag.

If a tag was added accidentally, with tag-based workflows enabled, first delete the released release, then delete all workflows running for that tag, and then delete the tag.

---

## 5. Artifact Requirements

The release must include:

### **Rust Core**
- Compiled binaries  
- `.rlib` / `.so` if needed  
- Documentation snapshot  
- API schema snapshot  

### **Desktop UI**
- Linux `.deb`, `.AppImage`  
- Windows `.exe`, `.msi`  
- macOS `.dmg`  
- Tauri bundle metadata  
- E2E test logs  

### **Android**
- `app-release.aab`  
- JNI `.so` libraries for all architectures  
- Instrumentation test results  

### **CLI & Server**
- Precompiled binaries for Linux, Windows, macOS  
- OpenAPI definitions  
- Config template files  

### **Documentation**
- Versioned `release-info-vX_Y_Z.txt` file (e.g., `release-info-v1_0_0.txt`)  
- [CHANGELOG.md](CHANGELOG.md) (references the versioned release-info file)  
- [docs/VERSION_REGISTRY.md](docs/VERSION_REGISTRY.md)  
- [spec/README.md](spec/README.md)  
- [docs/UI_Desktop.md](docs/UI_Desktop.md)  

---

## 6. Cross-Platform Build Reproducibility

Every release must be **reproducible**.

### Required:

- Deterministic builds  
- Fixed Rust toolchain version  
- Fixed NDK version  
- Fixed Node.js & pnpm version  
- Fixed dependencies (`Cargo.lock`, `pnpm-lock.yaml`)  

### CI must:
- Rebuild the project  
- Compare checksums with local build  
- Fail if mismatched  

---

## 7. Security and Distribution Requirements

Every release must include:

- GPG-signed tag  
- SHA256 list for all artifacts  
- Supply chain transparency files  
- SLSA-compliant provenance (if enabled)  
- Verification instructions for end-users  

### All artifacts must be verified before upload.

---

## 8. Required Release Checklist

**Before calling the release script:**

- [ ] All tests pass  
- [ ] No warnings in any platform  
- [ ] Version numbers updated everywhere  
- [ ] Documentation updated  
- [ ] [docs/VERSION_REGISTRY.md](docs/VERSION_REGISTRY.md)  
- [ ] [spec/README.md](spec/README.md) references the new `vX.Y.Z`  
- [ ] [docs/UI_Desktop.md](docs/UI_Desktop.md) references `Core/Server vX.Y.Z`  
- [ ] [release-info.txt](release-info.txt) prepared with tag (first line), release name (second line), and release information
- [ ] Android physical device tests completed  
- [ ] Desktop E2E tests completed  
- [ ] Sync tests across devices completed  
- [ ] All Spec-Kit tasks finalized  

**After the release script completes:**

- [ ] `release-info.txt` renamed to `release-info-vX_Y_Z.txt` (e.g., `release-info-v1_0_0.txt`)
- [ ] [CHANGELOG.md](CHANGELOG.md) updated with reference to the versioned release-info file  

---

## 9. Release Responsibilities

### Maintainers must:
- Approve the final Spec-Kit `/implementation` run  
- Verify all CI pipelines  
- Validate cryptographic signatures  
- Announce release notes  
- Ensure platform parity  

### Contributors must:
- Follow all versioning rules  
- Not push tags  
- Not bypass release script  
- Ensure parity in code, docs, tests  

---

### 🌐 Result  
Following these rules ensures that every release of Truth Training is:

- Trustworthy  
- Secure  
- Reproducible  
- Cryptographically verifiable  
- Consistent across all platforms  

# 🔄 Code Review, PR Process, and Merge Requirements

This section defines the **mandatory rules** for Pull Requests, code review, merge gates, approval policy, and contributor responsibilities.  
All contributors — internal or external — must follow these rules without exception.

These requirements ensure that Truth Training remains **secure, stable, auditable, and cryptographically reliable** across all platforms.

---

## 1. PR Requirements (All PRs Must Follow These)

Every Pull Request **must** include:

### ✔ A valid Spec-Kit workflow
- A `/speckit.specify` branch must exist  
- A finished `/speckit.plan`  
- All tasks (`/speckit.task`) implemented  
- `/speckit.clarify` completed  
- Final `/speckit.implementation` performed  

PRs **not backed by a Spec-Kit workflow will be closed without review.**

---

### ✔ PR description must include:
- Summary of the change  
- Associated Spec-Kit spec ID  
- Implementation notes  
- Testing summary  
- Affected modules (CLI / Core / Desktop / Android / Server)  
- Backwards compatibility impact  
- Security considerations  

---

### ✔ PR must be minimal, atomic, and scoped
- One feature / fix per PR  
- No multi-feature bundles  
- No unrelated refactors mixed with functional changes  
- No "drive-by" modifications  

Large cross-cutting changes must be split across multiple specs and PRs.

---

## 2. Code Quality and Style Requirements

### ✔ Rust
- Must pass `cargo fmt --all`  
- Must pass `cargo clippy --all-targets --all-features -- -D warnings`  
- Must compile with **zero warnings**  
- Unsafe code requires explicit documented justification  
- All public APIs must include Rustdoc comments  

### ✔ Kotlin (Android)
- Must pass `ktlint`  
- Must pass `detekt`  
- ViewModels must include UI state immutability  
- Network calls must use Retrofit + coroutines  
- JNI calls must be wrapped in safe abstractions  

### ✔ TypeScript (Desktop)
- Must pass `pnpm lint`  
- Must pass `pnpm typecheck`  
- React components must be fully typed  
- Zustand stores must be tested  

---

## 3. Required Test Coverage for PR Acceptance

A PR cannot be merged unless **all** of the following pass:

### ✔ Rust tests (Core + Server + Desktop Backend)
- Unit tests  
- Integration tests  
- Feature-flagged tests (`--features desktop`, `--features mobile`)  
- JSON serialization compatibility tests  
- Sync protocol consistency tests  

### ✔ Android tests
- JUnit unit tests  
- Instrumentation tests  
- Physical device tests (for discovery, JNI, network)  

### ✔ Desktop UI tests
- Jest unit tests  
- Integration tests (create-event flow, context editor)  
- Playwright E2E tests  
- Offline queue tests  

### ✔ CLI tests
- End-to-end tests using `truthctl`  
- Coverage of: list, discover, sync, cleanup, validate  

### ✔ Cross-platform tests
- Core–Desktop compatibility  
- Core–Android compatibility  
- JSON/Schema compatibility  
- Event + Context + Judgment interoperability  

### ✔ No skipped tests allowed
Any test marked with:

```
#[ignore]
it.skip
@Ignore
```

must contain justification in PR description.

---

## 4. PR Security Requirements

Before approval, every PR must satisfy:

### ✔ `cargo audit` — no vulnerabilities allowed  
If a dependency vulnerability is found:

- Provide justification  
- Provide workaround  
- Provide a timeline for upgrade  

### ✔ Signature verification routines must be tested  
- Ed25519 signature generation  
- Ed25519 verification  
- Replay-attack prevention  
- Message integrity and serialization  

### ✔ Discovery security checks  
- LAN/Wi-Fi announcements must sanitize input  
- Node registry URLs must be validated  
- TTL rules must be enforced  

### ✔ No dangerous changes  
Forbidden PR content:

- Adding debug endpoints  
- Disabling signature checks  
- Weak cryptography  
- Logging sensitive data  
- Introducing runtime randomness in consensus logic  

---

## 5. CI Requirements (PR Must Pass)

Every PR must pass **all** automated pipelines:

- Cross-platform build  
- Android build (debug + release)  
- Android unit + instrumented tests  
- Desktop build for all platforms  
- Rust core build  
- All test suites  
- Linting  
- Formatting  
- Type checking  
- Security audits  

If any job fails → PR is blocked.

---

## 6. Review and Approval Policy

Only designated maintainers can approve PRs.

### ✔ Minimum approval: 2 maintainers  
For security-sensitive changes → 3 approvals.

### ✔ Required review steps:
- Code style review  
- Architecture review  
- API compatibility review  
- Security review  
- Cross-platform behavior review  

### ✔ Reviewers must not merge their own PRs  
A PR requires an independent reviewer.

---

## 7. Merge Rules

A PR may be merged **only if all conditions are met**:

✔ All Spec-Kit steps completed  
✔ All tests passing  
✔ CI green  
✔ Review approvals completed  
✔ No merge conflicts  
✔ Rebase onto latest `main`  
✔ Signed commits required  
✔ No TODOs left in code  
✔ [CHANGELOG.md](CHANGELOG.md) updated  
✔ Documentation updated  

---

## 8. Post-Merge Validation

After merge:

- CI must automatically rebuild all platforms  
- Artifacts must match previous build checksums  
- Version matrix must remain consistent  
- No new warnings may appear  
- No silent behavior changes allowed  

If not — the merge must be reverted.

---

## 9. Contributor Responsibilities

Contributors must:

- Follow all rules in this document  
- Use Cursor + Spec-Kit for all work  
- Keep PRs small and focused  
- Maintain cross-platform behavior  
- Ensure backward compatibility  
- Follow all security and audit rules  
- Write deterministic, testable code  

Contributors who repeatedly ignore rules may be restricted from submitting PRs.

---

## 🌐 Summary

The Truth Training project is security-critical and cross-platform by design.  
Because of that, contributions must meet **industry-level quality, security, and reproducibility requirements**.

Following these guidelines ensures:

- Stability  
- Auditability  
- Cross-platform consistency  
- Trustworthiness of the entire network  

# 🧭 Part 11 — Appendix: Reference Materials, Tools, and Developer Resources

This final section provides **direct links**, **reference materials**, and **additional resources** required for contributing to Truth Training.  
It serves as a central hub for developers working with Cursor AI, Spec-Kit, Rust tooling, Android pipelines, cryptography modules, and cross-platform architecture.

Use this appendix whenever you need to quickly access tools, documentation, workflows, or example specifications.

---

## 📚 1. Core Project Documentation

### Main Documentation Directory
```
/docs/
```

### Important Documentation Directories
- [`docs/README.md`](docs/README.md) — entry point for all human-facing guides  
- [`docs/prompt/`](docs/prompt/) — per-author Spec-Kit prompt library  
- [`docs/architecture.md`](docs/architecture.md) — high-level system architecture  
- [`docs/Technical_Specification.md`](docs/Technical_Specification.md) — core/server data model and APIs  
- [`docs/cross_platform_discovery_compatibility.md`](docs/cross_platform_discovery_compatibility.md) — discovery/sync protocol compatibility  
- [`docs/Constitution-Compliance.md`](docs/Constitution-Compliance.md) — security, governance, and compliance notes  
- Android docs: [`docs/ANDROID_MIGRATION.md`](docs/ANDROID_MIGRATION.md), [`docs/ANDROID_TEST_FIX_SUGGESTIONS.md`](docs/ANDROID_TEST_FIX_SUGGESTIONS.md), [`docs/android_discovery_architecture.md`](docs/android_discovery_architecture.md), [`docs/TEST_REPORT_ANDROID_v1.0.0.md`](docs/TEST_REPORT_ANDROID_v1.0.0.md)  
- Desktop docs: [`docs/UI_Desktop.md`](docs/UI_Desktop.md), [`docs/ui_guidelines.md`](docs/ui_guidelines.md)  

### Specify Documentation Directory
```
/spec/
```
- `spec/` — all official specifications  
- [`spec/README.md`](spec/README.md) — authoritative Spec-Kit references (AI-first)  

---

## 🧩 2. Spec-Kit Prompt Library (Developer Examples)

These files show **real working examples** of `/speckit.specify`, `/plan`, `/clarify`, `/task`, and `/implementation` usage.

### Directory:
```
/docs/prompt/ekwator/
```

### Example Set 1  
- 📄 [docs/prompt/ekwator/specify1/spec1.md](docs/prompt/ekwator/specify1/spec1.md)  
- 🗂 [docs/prompt/ekwator/specify1/plan/plan1.md](docs/prompt/ekwator/specify1/plan/plan1.md)

### Example Set 2  
- 📁 `/docs/prompt/ekwator/specify2/`

These serve as templates to ensure contributors follow the correct Spec-Kit structure.

---

## 🧰 3. Essential Tools Used in Development

### Cursor AI IDE
Used for:
- Code generation  
- Spec-Kit integration  
- Inline type correctness  
- Safe refactoring  
- Execution of structured workflows  

Cursor is **mandatory** for all contributions.

---

### GitHub Spec-Kit  
Repository:  
🔗 https://github.com/github/spec-kit

Spec-Kit provides:
- Structured development workflows  
- Specification-driven planning  
- Automated PR branch creation  
- Execution automation  
- Deterministic planning and implementation

---

### Rust Toolchain
Required tools:
- `rustup`  
- `cargo`  
- `cargo fmt`  
- `cargo clippy`  
- `cargo audit`  
- `cargo nextest`  

Nightly builds may be required for edition-related features.

---

### Android Tooling
Required:
- Android Studio  
- JDK 17  
- Gradle  
- adb (for physical device testing)  
- Protobuf compiler (for cross-platform schemas)  

---

### Desktop Tooling
Required:
- Node.js + PNPM  
- Electron Builder  
- Playwright  
- Jest  
- TypeScript strict mode  

---

## 🔒 4. Security Tools and Dependencies

Must be used before merge:
- `cargo audit`  
- `cargo deny`  
- Android ProGuard/R8  
- Desktop dependency audit (`pnpm audit`)  
- JSON schema conformance tests  
- Signature and replay-attack tests  

---

## 🧪 5. Test Infrastructure Overview

### Test layers:
1. **Rust:**
   - Unit tests  
   - Integration tests  
   - Protocol tests  
   - Fuzzing (`cargo fuzz`)  
   - Snapshot compatibility tests  

2. **Android:**
   - JUnit  
   - Instrumentation  
   - Physical device tests  
   - JNI boundary tests  

3. **Desktop:**
   - Jest  
   - Playwright  
   - Redux/Zustand store tests  

4. **Cross-Platform:**
   - Core ↔ Android parity  
   - Core ↔ Desktop parity  
   - Schema consistency  
   - Event/judgment compatibility  

---

## 🌐 6. Architecture References

Use these files to understand how Truth Training works internally:

- [docs/architecture.md](docs/architecture.md) — System architecture, runtime boundaries, deployment layouts  
- [docs/Technical_Specification.md](docs/Technical_Specification.md) — End-to-end component breakdown  
- [docs/Data_Schema.md](docs/Data_Schema.md) — Database schema, migrations, entity contracts  
- [docs/Concept_Collective_Intelligence.md](docs/Concept_Collective_Intelligence.md) — Trust propagation concepts  
- [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md) — Android discovery subsystem internals  
- [docs/UI_Desktop.md](docs/UI_Desktop.md) — Desktop state model and UX flows  

---

## 🧱 7. Commit and PR Standards (Quick Reference)

### Commit Requirements
- Signed commits  
- Conventional commits  
- One logical change per commit  
- No generated code committed manually  

### PR Requirements
- Must follow Spec-Kit  
- Must pass CI  
- Must include tests  
- Must update docs when needed  

### Merge Requirements
- ≥2 approvals  
- No warnings  
- All tests green  
- No TODOs left  
- Changelog updated  

---

## 📦 8. Release Requirements

Before cutting a release:
- All quality gates must pass  
- No dev-only features enabled  
- Crypto routines must be re-validated  
- Android & Desktop apps must build in release mode  
- Cross-platform sync must be tested end-to-end  
- Version matrix must be in sync:

```
Core    Android    Desktop    Server
 ✓         ✓          ✓          ✓
```

---

## 🗂 9. File Index for Contributors

The following files are essential for understanding the project:

| File / Directory | Purpose |
|------------------|---------|
| [README.md](README.md) | Project overview |
| [SECURITY.md](SECURITY.md) | Security policies |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution rules |
| [14-quality-gates.md](14-quality-gates.md) | PR acceptance criteria |
| [docs/README.md](docs/README.md) | Human-facing documentation hub |
| [spec/README.md](spec/README.md) | Spec-Kit index / decision source |
| [docs/VERSION_REGISTRY.md](docs/VERSION_REGISTRY.md) | Version matrix enforced by CI |
| [docs/prompt/](docs/prompt/) | Spec-Kit prompt library by author |
| [Cargo.toml](Cargo.toml) | Rust workspace definition |
| [truth-android-client/](truth-android-client/) | Android client sources |
| [ui/desktop/](ui/desktop/) | Desktop (React + Tauri) sources |
| [src/](src/) | truth_core server binary |
| [core/](core/) | Shared Rust core library |

---

## 🤝 10. Community and Support

If you want to contribute, discuss design ideas, or propose improvements:

- GitHub Issues  
- GitHub Discussions  
- Pull Requests (Spec-Kit required)  
- Security reports through GitHub Advisory  

Your contributions help expand the **Network of Anonymous Trust**.

---

## 🧠 11. Philosophy and guiding analogy (Optional reading)

Truth Training is built on the analogy:

- **Each application instance = a neuron**  
- **A human operator = activation function**  
- **Connections between users = synapses**  
- **Global network = hybrid human-machine neural network**  

This analogy guides algorithm evaluation and system design.

---

## 🎉 End of [CONTRIBUTING.md](CONTRIBUTING.md)

You are now ready to contribute safely, consistently, and effectively.

