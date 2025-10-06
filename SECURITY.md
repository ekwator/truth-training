# 🔐 Security Policy

## 🧩 Supported Versions

Truth Training is currently under **active development** and published as an open-source experimental project.  
Security updates are provided for the **latest stable release** and the **development branch**.

| Version          | Supported          | Notes |
| ---------------- | ------------------ | ----- |
| `main` (latest)  | ✅ Yes              | Active development, frequent commits |
| `v0.3.x`         | ✅ Yes              | Core stabilization & crypto verification |
| `v0.2.x`         | ⚠️ Limited         | Legacy P2P prototype |
| `< v0.2`         | ❌ No               | Deprecated research builds |

---

## 🧠 Security Model Overview

Truth Training is a **peer-to-peer knowledge network**, where every node:
- Signs outgoing messages using **Ed25519 digital signatures**;  
- Verifies incoming messages using **public keys from peers**;  
- Does not rely on a centralized server or single point of trust.  

Main risks include:
- Misconfigured nodes or compromised peers;  
- Leaked private keys;  
- Injection of invalid sync data;  
- Replay or forgery attacks.

---

## 🧰 Development Security Guidelines

1. **Never commit private keys or seed phrases.**  
2. Use **Result-based error handling** for all crypto/network ops.  
3. Run `cargo clippy`, `cargo fmt`, and `cargo audit` before PRs.  
4. Avoid `unsafe` code unless documented and reviewed.  
5. Sign all sync payloads with timestamps to prevent replay.  
6. All crypto/network PRs require manual review.

---

## 🧾 Reporting a Vulnerability

Report privately:
- 📧 **Email:** ekwatormail@gmail.cpm  
- 🐙 [GitHub Security Advisory](https://github.com/ekwator/truth-training/security/advisories/new)

Include:
- Detailed description  
- Proof of concept (if any)  
- Affected commits or versions  

You’ll receive acknowledgment **within 48 hours**,  
and verification or fix plan **within 7 days**.

---

## 🛠 Security Review and Testing

Before each release:
- ✅ Run `cargo audit`  
- ✅ Review dependencies  
- ✅ Test signature and verification routines  
- ✅ Run fuzz tests for serialization  

---

## ⚖️ Legal & Ethical Notice

Truth Training is an **educational, research-oriented project**.  
Do **not** use it for:
- Surveillance or disinformation  
- Unauthorized data collection  
- Any illegal activity  

Use under the **MIT License**, following the project’s ethical guidelines.

