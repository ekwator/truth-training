# Constitution Compliance (Truth Training v1.0.0)

**Constitution Version**: v2.2.0 (last amended: 2025-12-01)  
**Project Version**: v1.0.0

## 🔐 Privacy and Confidentiality

**Truth Training is built on the fundamental principle of confidentiality**: **No user actions are logged or persistently stored**. This principle is enforced across all platforms and is a core architectural requirement. The application does not track, record, or save any user interactions, navigation patterns, clicks, or behavioral data.

**Privacy Guarantees:**
- ✅ **No User Action Logging**: No clicks, navigation, or interaction history is stored
- ✅ **No Persistent User Tracking**: No identifiers, session data, or behavioral analytics
- ✅ **No Telemetry Collection**: No user activity is transmitted or stored
- ✅ **Ephemeral Logs Only**: Only system-level logs (errors, sync operations) are temporarily stored for debugging purposes

---

## Principles Coverage (v1.0.0)

- **Truth as Anonymous Confession**: `/api/v1/events` accepts content without author metadata; events use embedded context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) instead of `context_id`.
- **Truth Without Author**: Events store no author metadata; validation relies on transport envelope signatures (`X-Public-Key`, `X-Signature`, `X-Timestamp` headers).
- **Anti-Fraud**: Independent confirmations modeled via unique sender nodes; collective intelligence consensus calculation with weighted judgments.
- **Digital Conscience**: Reflection via corrections and non-punitive flows; judgment system supports ternary assessments ('true' | 'false' | 'uncertain').
- **Decentralized Civic Dialogue**: Ternary judgments (confirm/reject/abstain) supported via `/api/v1/judgments` endpoint with confidence levels (0.0-1.0).
- **Local Mesh of Truth Exchange**: Cross-platform node discovery via UDP multicast (239.255.0.1:52525), global registry polling, and P2P sync implemented across Desktop, CLI, Server, and Android platforms.

## How to Validate (v1.0.0)

1. **Run tests**: `cargo test --all-features` (Core, Server, CLI)
2. **Test Desktop UI**: `cd ui/desktop && npm test` (unit, integration, E2E)
3. **Test Android**: `cd truth-android-client && ./gradlew test` (unit, instrumentation)
4. **Verify API compliance**: Test `/api/v1/events` endpoint accepts embedded context fields (no `context_id`)
5. **Verify judgments**: Test `/api/v1/judgments` endpoint with ternary assessments ('true' | 'false' | 'uncertain')
6. **Verify node discovery**: Test cross-platform discovery via UDP multicast (239.255.0.1:52525)
7. **Documentation validation**: Execute `make doc-refactor-run` and confirm `reports/doc_refactor/link_report.json` contains zero `status: "missing"` edges

**Reference**: [specs/005-constitution-compliance-truth/quickstart.md](../specs/005-constitution-compliance-truth/quickstart.md) for detailed validation steps.

## Security Note
- Anonymous confessions are stored plaintext-at-rest by design; ensure environment trust and backups policies.

_Version: v1.0.0_
