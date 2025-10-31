
# Quickstart: Constitution Compliance Validation

## Steps
1. Submit anonymous confession via CLI; ensure event body contains no author metadata.
2. Verify event stored plaintext at rest and signed via transport envelope.
3. Submit judgments from multiple nodes: confirm/reject/abstain.
4. Check compliance matrix: independent confirmations counted by unique envelope sender nodes.
5. Observe inconsistency decay: repeated conflicting judgments reduce weight over time.
6. Perform nearby sync over Wi‑Fi Direct; verify events propagate without central connectivity.

## Expected Outcomes
- Ternary judgments accepted and recorded.
- Envelope-only validation path succeeds; author identity not stored.
- Plaintext storage observed; risk warning displayed in UI docs.
- Mesh sync functions over Wi‑Fi Direct.


