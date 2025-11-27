
# Research: Constitution Compliance (Truth Training Core & UI)

## Decisions
- Authorship handling: No author metadata in events; validation via transport envelope signatures and distributed confirmations.
- Anonymous confession storage: Plaintext at rest; rely on TLS in transport; communicate risk in UI and docs.
- Independent confirmation definition: Distinct transport envelopes from unique sender nodes within anti-replay window.
- Judgment signal: Ternary — confirm, reject, abstain.
- Opportunistic transport target: Wi‑Fi Direct peer‑to‑peer for nearby sync.

## Rationale
- Separating truth from authorship aligns with “Truth Without Author” and reduces identity coupling risks.
- Plaintext-at-rest is a deliberate tradeoff to enable local debugging and simplicity; risk mitigation via clear disclosure and optional future E2E.
- Using transport envelopes for independence ties to node-level identity and anti-replay protections already in place.
- Ternary judgments capture nuanced civic dialogue without overcomplicating UI.
- Wi‑Fi Direct provides robust nearby connectivity across common devices without extra hardware.

## Alternatives Considered
- Salted author hashes: rejected to avoid re-identification vectors.
- End-to-end sealed boxes: deferred; increases key management complexity.
- Confidence scalar: deferred; adds UX and math complexity.
- Bluetooth LE first: deferred due to bandwidth and platform constraints.

_Version: v1.0.0_
