# UI Integration Guidelines for Truth Training

---

## ✅ 1. General Principles

* UI must **not implement business logic** — all logic stays in the core.
* UI communicates with core via:

  * **HTTP API** (default, for all platforms)
  * **Direct FFI/JNI** (optional for Android, Windows, macOS for performance).
* Ensure **localization support** (RU/EN) based on user settings.

---

## ✅ 2. UI Responsibilities

* Provide **clean, minimalistic interface** with emphasis on:

  * Event creation (truth/lie detection).
  * Progress visualization.
  * Synchronization status.
* Implement **expert system wizard** for evaluating detected events:

  * Show guiding questions.
  * Allow user to mark event as recognized/unrecognized.

---

## ✅ 3. API Usage (HTTP Mode)

* Base URL: `http://<core-ip>:<port>/`
* Endpoints:

  * `POST /init` – initialize DB.
  * `POST /seed` – load knowledge base.
  * `POST /events` – add new event.
  * `POST /detect` – mark detection.
  * `POST /impacts` – add impact.
  * `GET /events` – list events (signed for P2P use).
  * `GET /progress` – read progress metrics.
  * `GET /progress` – get progress metrics.

---

## ✅ 4. Desktop UI (Linux/Windows/macOS)

* Use **Tauri (Linux)** or **WinUI (Windows)** or **SwiftUI (macOS)**.
* Provide local config to set:

  * Core API address.
  * Language.
  * Sync mode (auto/manual).

---

## ✅ 5. Mobile UI (Android)

* Use **Kotlin + Jetpack Compose**.
* JNI bridge for direct calls:

  * Use Rust functions for DB and logic.
* Offline-first principle:

  * All events stored locally, then synced via Wi-Fi mesh when available.

---

## ✅ 6. Future Enhancements

* Push notifications for sync events.
* Support offline peer-to-peer sync on mobile (Wi-Fi Direct).
* Add **dark mode** for better UX.
