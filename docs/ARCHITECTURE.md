# Architecture Overview

This document describes the high-level architecture of the Truth Training platform.

## Functional Separation

- **truth-core**: core logic, P2P, crypto, and DB access (library only, no user I/O).
- **app (truthctl)**: administrative CLI that uses truth-core as a dependency.
- **server**: network node (HTTP + P2P) that provides API endpoints.

This separation ensures modular testing, clean builds, and independent versioning.

