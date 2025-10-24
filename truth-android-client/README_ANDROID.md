# Android Integration Notes

This file provides quick guidance for integrating the Android client with the Truth Core (Rust) library.

- The app dynamically loads `libtruthcore.so` via JNI.
- Communication is JSON-based through JNI bridge functions (`TruthCore.kt`).
- From main branch updates:
  - Trusted signature verification (RSA/Ed25519)
  - Secure P2P discovery and message exchange over LAN
  - Mock JSON endpoints for offline testing

For more detailed integration steps, see:
- `docs/INTEGRATION_TRUTH_CORE.md`
- `truthcore_api/api_reference_link.md`
