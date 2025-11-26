# 🌐 Truth Training — The Network of Anonymous Trust  
*(Created in Cursor AI IDE)*

Truth Training is a decentralized communication ecosystem where **truth travels without identity**.  
Events move freely through the network — encrypted, verified, and echoed by others — creating a distributed field of awareness instead of a chain of messages.

Each reflection of an event confirms its existence; each independent echo increases its credibility.  
Like confession without a priest, users anonymously release truths into the network — and the collective conscience responds.

It can serve as an **alternative to voting systems**, measuring the authenticity of social signals and public sentiment not through ballots, but through shared evaluation of facts.

Unlike LoRa-based mesh systems such as **Meshtastic**, Truth Training builds a **mesh of minds, not hardware** — using Wi-Fi and the Internet as carriers of encrypted meaning, forming an autonomous infrastructure of human understanding.

Originally conceived to combat fraud, Truth Training evolves into a **self-learning immunity against falsehood** — distinguishing truth from deception through context, correlation, and collective resonance.

---

## 🚫 No Bans. No Muting. No Administrative Suppression.

A fundamental principle of Truth Training is **absolute non-censorship**:

- **No user can be banned.**  
- **No message can be deleted or blocked.**  
- **No administrator or server operator can silence, shadow-ban, or filter participants.**

The system is designed so that **no central authority exists** with the ability to suppress information or exclude individuals.

All data flows are governed by:

- **cryptographic identity**,  
- **context evaluation**,  
- **decentralized consensus**,  
- **and distributed replication**,  

—not by human moderation or arbitrary rules.

Instead of punishment or exclusion, the network uses:

- **context-based interpretation**
- **collective impact scoring**
- **correlation across independent nodes**
- **temporal decay and natural fading of irrelevant data**

This forms an **organic, self-regulating signal ecosystem**, where falsehood loses weight naturally — not because someone censored it, but because **the network rejects it collectively**.

Truth Training is not a platform where content is controlled.  
It is a field where meaning **emerges**.

---

## Version & Platform Matrix

**Current Version**: v1.0.0 (stable)

| Component | Version | Platform |
|-----------|---------|----------|
| Core Library | v1.0.0 | Cross-platform (Rust) |
| Desktop UI | v1.0.0 | Linux, Windows, macOS |
| Android Client | v1.0.0 | Android (API 26+) |
| Server | v1.0.0 | Linux, Windows, macOS |
| CLI (`truthctl`) | v1.0.0 | Linux, Windows, macOS |

**Release Information**: See [Release Notes](docs/RELEASE_v1.0.0_DRAFT.md) and [Version Registry](docs/VERSION_REGISTRY.md) for detailed version history and platform-specific details.

---

## Quick Start

### Requirements
- Rust (recommended ≥ 1.75)
- cargo
- SQLite (libsqlite3-dev)
- Git

### Build & Run (Development)
```bash
# Clone
git clone https://github.com/ekwator/truth-training.git
cd truth-training

# Build
cargo build --workspace

# Run node (example)
cargo run --bin truth_core -- --port 8080 --db truth_training.db --http-addr http://127.0.0.1:8080
```

For detailed build instructions, see [Cross-Platform Build Instructions](spec/19-build-instructions.md).

---

## Platform Overview

Truth Training uses a **cross-platform core library** (`truth_core`) that adapts to different platforms:

- **Desktop** (Linux, Windows, macOS): Full feature set with HTTP server, CLI tools, and complete P2P networking
- **Mobile** (Android): Minimal subset with FFI interfaces for native app integration
- **iOS**: Prototype stub (community contributions welcome)

### Platform-Specific Features

**Desktop Features:**
- HTTP REST API server
- CLI management tools (`truthctl`)
- Complete P2P synchronization
- Web-based administration interface
- Full async runtime (Tokio)

**Mobile Features:**
- Minimal P2P protocol
- Ed25519 cryptographic operations
- FFI interfaces for native apps
- Lightweight async runtime (Smol)
- JSON signature verification

For detailed platform integration guides, see:
- [Android Integration](integration/android/README_INTEGRATION.md)
- [iOS Integration](integration/ios/README_INTEGRATION.md) (prototype)
- [Desktop Integration](integration/desktop/README_INTEGRATION.md)

---

## Architecture Overview

**Core idea**  
Truth Training is a decentralized, peer-to-peer system for collecting, verifying and contextualizing events and claims. It is inspired by the principles of FIDONet (store-and-forward, hub/leaf roles, trust propagation) and uses cryptographic signatures (Ed25519) to ensure author authenticity and data integrity.

**High-level goals**
- Decentralized storage and verification of events.
- Reproducible, auditable history with signed events.
- Peer discovery, synchronization and local diagnostics via CLI.

### FIDONet-inspired Network Model
- **Node roles**: *leaf* (edge node) or *hub* (relay/aggregator).
- **Store-and-forward**: nodes store data locally, synchronize on schedule or on-demand.
- **Trust & signatures**: events are signed with Ed25519; public keys identify nodes.
- **Routing & replication**: leaf→hub→hub→leaf; hub nodes relay and aggregate.

For detailed architecture documentation, see:
- [Architecture Guide](docs/architecture.md)
- [Technical Specification](docs/Technical_Specification.md)
- [Architecture Spec](spec/03-architecture.md)

---

## API & CLI

### HTTP API

Truth Training provides a RESTful HTTP API with signed endpoints, JWT authentication, and role-based access control.

**Quick Reference:**
- Authentication: `POST /api/v1/auth` (JWT tokens)
- Events: `GET /events`, `POST /events`
- Sync: `POST /sync`, `POST /incremental_sync`
- Health: `GET /health`

For complete API documentation, see:
- [API Reference](docs/api_reference/API_REFERENCE.md) — Human-readable API guide
- [API Spec](spec/05-api.md) — Canonical API specification

### CLI: `truthctl`

The `truthctl` command-line tool provides administration capabilities for node management, peer synchronization, and diagnostics.

**Quick Examples:**
```bash
truthctl keys generate --save
truthctl init-node mynode --port 8080 --db ./node.db --auto-peer
truthctl peers add http://127.0.0.1:8081 <peer_pubkey_hex>
truthctl peers sync-all --mode incremental
truthctl graph show --format ascii --min-priority 0.5
```

For complete CLI documentation, see:
- [CLI Usage Guide](docs/CLI_Usage.md) — User guide
- [CLI Specification](spec/10-cli.md) — Complete command reference

---

## Documentation

### For Users & Developers

- **[API Reference](docs/api_reference/API_REFERENCE.md)** — Complete HTTP API documentation
- **[CLI Usage](docs/CLI_Usage.md)** — Command-line tool guide
- **[Architecture Guide](docs/architecture.md)** — System architecture overview
- **[Technical Specification](docs/Technical_Specification.md)** — Detailed technical documentation
- **[Design Index](docs/DESIGN_INDEX.md)** — Cross-reference for all design documents

### Platform-Specific Documentation

- **Android**: [Migration Guide](docs/ANDROID_MIGRATION.md), [Test Report](docs/TEST_REPORT_ANDROID_v1.0.0.md), [Test Fix Suggestions](docs/ANDROID_TEST_FIX_SUGGESTIONS.md)
- **Cross-Platform Comparison**: [Android vs Desktop](docs/Truth-training/Truth-training.md)
- **Version Registry**: [Version Registry](docs/VERSION_REGISTRY.md)

### For AI Agents & Architects

- **[Spec Index](spec/README.md)** — Primary entry point for AI agents and architects
- **Core Specs**: Product vision, requirements, architecture, API contracts, test plans, traceability, quality gates

All specifications are located in the [`spec/`](spec/) directory. See [spec/README.md](spec/README.md) for guidance on using specs for decision-making.

---

## Testing

Truth Training includes comprehensive test suites across all platforms:

- **Desktop**: Unit and integration tests (`cargo test --workspace --features p2p-client-sync`)
- **Android**: Instrumentation tests (96.3% coverage, see [Test Report](docs/TEST_REPORT_ANDROID_v1.0.0.md))
- **Cross-Platform**: Integration tests for TTL consistency and JSON serialization

For detailed testing information, see:
- [Test Plan](spec/16-test-plan.md)
- [Android Test Report](docs/TEST_REPORT_ANDROID_v1.0.0.md)
- [Android Test Fix Suggestions](docs/ANDROID_TEST_FIX_SUGGESTIONS.md)

---

## Security & Responsible Disclosure

See [`SECURITY.md`](SECURITY.md) for the security policy. In short:
- Use up-to-date dependencies.
- Report vulnerabilities to the repository owner (see SECURITY.md).
- Signed messages use Ed25519; private keys must be kept secret.

---

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) (or [`spec/14-quality-gates.md`](spec/14-quality-gates.md)) — standards require:
- `cargo fmt` and `cargo clippy` clean runs.
- Tests for new features.
- Spec updates in `spec/` for any protocol or API changes.

---

## License

This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0). See [`LICENSE.txt`](LICENSE.txt) for details.
