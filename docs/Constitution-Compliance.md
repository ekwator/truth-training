# Constitution Compliance (v2.1.0)

## Principles Coverage
- Truth as Anonymous Confession: `/events` accepts content without author metadata; CLI `confess` warns plaintext-at-rest.
- Truth Without Author: Events store no author metadata beyond legacy fields; validation relies on transport envelopes.
- Anti-Fraud: Independent confirmations modeled via unique sender nodes; weights/decay utilities in `core::weights`.
- Digital Conscience: Reflection via corrections and non-punitive flows; CLI `judge` supports abstain.
- Decentralized Civic Dialogue: Ternary judgments (confirm/reject/abstain) supported.
- Local Mesh of Truth Exchange: Wi‑Fi Direct nearby sync scaffold added (`p2p::wifi_direct`).

## How to Validate
1. Run `cargo test --all-features`.
2. Follow [specs/005-constitution-compliance-truth/quickstart.md](https://github.com/ekwator/truth-training/blob/main/specs/005-constitution-compliance-truth/quickstart.md) steps.
3. Use CLI: `truthctl confess` and `truthctl judge`.

## Security Note
- Anonymous confessions are stored plaintext-at-rest by design; ensure environment trust and backups policies.

_Version: v1.0.0_

