# 🌐 Truth Training — The Network of Anonymous Trust  
*(Created in Cursor AI IDE)*

## 🔐 Privacy and Confidentiality

**Truth Training is built on the fundamental principle of confidentiality**: **No user actions are logged or persistently stored**. The application does not track, record, or save any user interactions, navigation patterns, clicks, or behavioral data. This ensures complete privacy and anonymity — users can interact with the system without leaving any trace of their actions.

**Key Privacy Guarantees:**
- ✅ **No User Action Logging**: No clicks, navigation, or interaction history is stored
- ✅ **No Persistent User Tracking**: No identifiers, session data, or behavioral analytics
- ✅ **No Telemetry Collection**: No user activity is transmitted or stored
- ✅ **Ephemeral Logs Only**: Only system-level logs (errors, sync operations) are temporarily stored for debugging purposes

This confidentiality principle is enforced across all platforms (Desktop UI, Android, Server, CLI) and is a core architectural requirement.

---

Truth Training is a decentralized communication ecosystem where **truth travels without identity**.  
Events move freely through the network — encrypted, verified, and echoed by others — creating a distributed field of awareness instead of a chain of messages.

Each reflection of an event confirms its existence; each independent echo increases its credibility.  
Like confession without a priest, users anonymously release truths into the network — and the collective conscience responds.

It can serve as an **alternative to voting systems**, measuring the authenticity of social signals and public sentiment not through ballots, but through shared evaluation of facts.

Unlike LoRa-based mesh systems such as **Meshtastic**, Truth Training builds a **mesh of minds, not hardware** — using Wi-Fi and the Internet as carriers of encrypted meaning, forming an autonomous infrastructure of human understanding.

Originally conceived to combat fraud, Truth Training evolves into a **self-learning immunity against falsehood** — distinguishing truth from deception through context, correlation, and collective resonance.

And beyond communication, Truth Training enables **teamwork without a team lead** — a coordination model where decisions arise from **collective consensus**, not hierarchy, creating a self-organizing environment for groups and projects.

Ultimately, without network connectivity, the application can serve as a **personal electronic diary** — a private space for individual reflection and truth-tracking.

---

## 1. Core Idea and Purpose of the System

🧠 **Truth Training — Operating Logic and Computational Model**
For more details see : [docs/model_core.md](docs/model_core.md)

**Truth Training** is a system for collective evaluation of events and statements, based on the principle of the *wisdom of the crowd*.  
It does not assume the presence of a central arbiter of truth and does not require expert knowledge from individual participants.

In the system, truth:

- is not declared  
- is not voted on directly  
- is not determined by authority  

Instead, it emerges statistically — as a stable result of independent evaluations accumulated over time.

---

## 2. Core Entity: Event (Truth Event)

An **Event** is a statement or fact that has been recorded in the system.

An event:

- appears as unverified  
- circulates within the system  
- receives independent evaluations  
- over time is either reinforced or rejected  

### Key Logic

- No event is immediately considered true or false  
- Truthfulness is a **process**, not a state  

---

## 3. Event Code (Field Code)

Each event has an **8-bit code** used not for semantic meaning, but for protocol-level propagation logic.

In the described model, the code controls:

- event transmission  
- retransmission  
- termination of propagation  

The code:

- does not directly participate in truth calculation  
- can be algorithmically modified during propagation  

This allows the system to:

- prevent infinite propagation  
- implement P2P logic without changing data structures  
- separate transport logic from evaluation logic  

---

## 4. Event Evaluation (Impact)

### What Is Impact

**Impact** is a subjective assessment of the *consequences* of an event, not an evaluation of the fact itself.

Impact answers the question:

> “What effect did (or will) this event have?”

Each Impact:

- is linked to a specific event  
- has a type (reputation, finance, emotions, etc.)  
- has a sign:
  - positive  
  - negative  
- is time-stamped  

### Key Principle

The system does not ask *“Is this true?”*  
It asks:

> “What happened as a result of this being accepted as true?”

## 5. Judgment Assessment (Truth Determination)

### What Is Judgment

**Judgment** is an individual user's assessment of an event's truthfulness, forming the basis for collective truth determination.

Judgment answers the question:

> "Is this event true or false based on my understanding?"

Each Judgment:

- is linked to a specific event
- represents individual truth assessment
- contributes to collective truth score
- preserves user anonymity
- is time-stamped

### Truth Calculation Based on Judgments

The truthfulness of an event emerges from the aggregation of individual judgments:

Event truthfulness ≈
(Σ positive judgments − Σ negative judgments)
÷ total number of judgments

**Key Principle:**

Truth is determined through collective independent assessments rather than direct declaration or voting.

---

## 6. Impact vs Judgment

While Impact evaluates consequences, Judgment determines truthfulness:

- **Impact**: "What effect did this have?" (consequence-focused)
- **Judgment**: "Is this true or false?" (truth-focused)

---

## 7. Truth Calculation (Implicit Truth Score)


The truthfulness of an event is not stored as a field and is not explicitly defined.

It is derived from:

- the number of Impact evaluations  
- their direction (positive / negative)  
- accumulation over time  
- stability of the result  

### Simplified Calculation Model

Event truthfulness ≈
(Σ positive impacts − Σ negative impacts)
÷ number of events  

**Important:**

- early evaluations carry less weight  
- stable evaluations gain significance over time  
- sharp changes indicate conflicting interpretations  

---

## 8. The "Wisdom of the Crowd" Principle

The system embeds key conditions for valid collective evaluation:

- **Independence of participants**  
  - participants do not see others’ evaluations  

- **Diversity of sources**  
  - different contexts, motivations, experiences  

- **Sufficient number of evaluations**  
  - the law of large numbers applies  

- **Absence of a central truth authority**  

Truth emerges as statistical equilibrium, not as a decision.

---

## 9. Evaluation Context

Each event is linked to a **context**, which defines:

- domain (social, financial, political, etc.)  
- form (truth, deception, omission)  
- cause  
- development path  
- effect  

Context:

- does not define truth  
- provides a frame for interpreting consequences  

---

## 10. Aggregated Metrics (Progress Metrics)

The system periodically computes aggregates:

- total number of events  
- ratio of positive to negative impacts  
- trend (slope of trust change)  

These metrics:

- are not used for decision-making  
- serve as system state indicators  
- allow observation of dynamics  

---

## 11. Database Schema (Semantic Overview)

### Core Tables

#### `truth_events`

Stores events:

- description  
- context  
- timestamps  
- discovery status  
- protocol propagation code  

#### `impact`

Stores impacts:

- link to event  
- impact type  
- sign (positive / negative)  
- timestamp  


#### `context`

Multifactor interpretation model:

- category  
- form  
- cause  
- development  
- effect  

#### `judgment`

Stores judgment:

- link to event
- truth assessment (true/false)
- timestamp
- user anonymity identifier

#### `progress_metrics`

Aggregated system state indicators

---

## 12. The Main Consequence

**Truth Training:**

- does not fight lies directly  
- does not require acknowledging falsehood  
- does not force truth  

Instead, the system:

- allows events to “live through time”  
- records consequences  
- shows which statements remain stable over time  

Truth here is:

> that which continues to function without destroying the system

---

## 13. Why This Logic Is Resilient

- lies may be profitable in the short term  
- but their consequences accumulate  
- collective evaluation does not require trust in participants  
- only trust in statistics  

Thus, the system naturally identifies and suppresses fraud —  
not through control, but through observation of consequences.

---

## Release Surfaces

- **Core Library** — integration guide: [docs/quickstart_core.md](docs/quickstart_core.md)
- **CLI** — quickstart: [docs/quickstart_cli.md](docs/quickstart_cli.md), reference: [docs/CLI_Usage.md](docs/CLI_Usage.md)
- **Server** — quickstart: [docs/quickstart_server.md](docs/quickstart_server.md), deployment: [docs/Deployment.md](docs/Deployment.md)
- **Desktop UI** — quickstart: [docs/quickstart_desktop.md](docs/quickstart_desktop.md), reference: [docs/UI_Desktop.md](docs/UI_Desktop.md)
- **Android Mobile** — quickstart: [docs/quickstart_android.md](docs/quickstart_android.md), architecture: [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md)
- **iOS Mobile** — quickstart: [docs/quickstart_ios.md](docs/quickstart_ios.md)

## 📦 Downloads & Pre-built Binaries

Ready-to-use binaries and installers for all platforms are available in the [GitHub Releases section](https://github.com/ekwator/truth-training/releases). Download pre-compiled executables, installers, and packages for:

- **Desktop Applications**: Linux (AppImage, DEB, RPM), Windows (MSI, EXE), macOS (DMG, PKG)
- **Android**: APK and AAB packages for direct installation
- **iOS**: IPA packages and App Store builds
- **Server**: RPM and DEB packages for Linux distributions, PKG for macOS
- **CLI Tools**: Pre-compiled `truthctl` binaries for all supported platforms
- **Core Libraries**: Static and dynamic libraries (`.a`, `.so`, `.dylib`) for integration

All releases include checksums (SHA256) for verification and are signed for security. Visit the [releases page](https://github.com/ekwator/truth-training/releases) to download the latest stable version or development builds.

## Documentation Entry Points

- [docs/README.md](docs/README.md) — Human-readable, narrative depth
- [spec/README.md](spec/README.md) — AI-focused directives and constraints
- [docs/Documentation_Refactor_Overview.md](docs/Documentation_Refactor_Overview.md) — Pipeline summary
- [docs/Documentation_Refactor_Inventory.md](docs/Documentation_Refactor_Inventory.md) — Inventory instructions
- [docs/Documentation_Refactor_Links.md](docs/Documentation_Refactor_Links.md) — Link validation workflow


