# Truth Training Architecture
Version: v0.4.0
Updated: 2025-01-18

---

## 🔄 General Concept

**Truth Training** is a cross-platform platform whose core is implemented in **Rust**. The core handles:

* Data processing logic (events, contexts, expert system).
* Local storage (SQLite).
* API (REST/HTTP) for interaction with UI and other nodes.
* Synchronization module (P2P via UDP + HTTP).

UI shells for different platforms integrate with the core through FFI or HTTP API.

---

## 🔋 Repository Structure

```
truth-training/             # Core: Rust + Actix-web + Sync Engine
truth-training-unix/        # UI for Linux (GTK or Tauri)
truth-training-windows/     # UI for Windows (WinUI or Tauri)
truth-training-android/     # UI for Android (Kotlin + JNI)
truth-training-apple/       # UI for macOS and iOS (SwiftUI + FFI)
```

---

## 🔧 Core (Rust)

* **Language:** Rust
* **Frameworks:** Actix-web, Tokio
* **Database:** SQLite (via `rusqlite`)
* **Functions:**

  * Knowledge base management.
  * Event creation and processing (`truth_events`).
  * Expert system (lie detector).
  * Data synchronization via P2P.

---

## 🌐 UI Platforms

### **Linux (truth-training-unix)**

* **Options:** GTK (via `gtk-rs`) or Tauri (HTML + Rust backend).
* **Core connection:**

  * Via HTTP API (Actix).
  * Or direct function calls via crate (if installed locally).

### **Windows (truth-training-windows)**

* **Options:**

  * **WinUI 3** (C# + Rust DLL via FFI).
  * **Tauri** (universal approach).
* **Connection:**

  * Via HTTP API.
  * Or via DLL + FFI.

### **Android (truth-training-android)**

* **Language:** Kotlin + JNI.
* **Core connection:**

  * Rust compiled to `.so` (via cargo-ndk).
  * JNI wrapper for calling core functions.

### **Apple (macOS/iOS) (truth-training-apple)**

* **Options:**

  * SwiftUI + Rust via FFI (`dylib`).
  * Or Tauri for macOS.
* **Connection:**

  * Via FFI (calling Rust functions from Swift).
  * For iOS need Rust cross-compilation.

---

## 📂 Integration and Updates

* All UI projects connect core as **Git submodule** or as **crate from crates.io**.
* Common documents (`docs/`) stored in `truth-training`.

---

## 🖌 Mermaid Architecture Diagram

```mermaid
flowchart TB
    subgraph Core [Core (Rust)]
        DB[(SQLite)]
        API[REST API (Actix-web)]
        Sync[P2P Sync Engine]
    end

    subgraph LinuxUI [Linux UI]
        GTK[GTK / Tauri]
    end

    subgraph WindowsUI [Windows UI]
        WIN[WinUI 3 / Tauri]
    end

    subgraph AndroidUI [Android UI]
        AND[Kotlin + JNI]
    end

    subgraph AppleUI [macOS / iOS]
        APP[SwiftUI + FFI]
    end

    API <--> GTK
    API <--> WIN
    API <--> AND
    API <--> APP
```

---

## 📄 Documents

* **architecture.md** (current file) — module diagram and connections.
* **ui\_guidelines.md** — UI integration rules with core.
* **build\_instructions.md** — core and UI build instructions.