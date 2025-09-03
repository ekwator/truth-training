# Truth Training Architecture

---

## 🔄 Общая концепция

**Truth Training** — это кроссплатформенная платформа, ядро которой реализовано на **Rust**. Ядро отвечает за:

* Логику обработки данных (события, контексты, экспертная система).
* Локальное хранение (SQLite).
* API (REST/HTTP) для взаимодействия с UI и другими узлами.
* Модуль синхронизации (P2P через UDP + HTTP).

UI-оболочки для разных платформ интегрируются с ядром через FFI или HTTP API.

---

## 🔋 Структура репозиториев

```
truth-training/             # Ядро: Rust + Actix-web + Sync Engine
truth-training-unix/        # UI для Linux (GTK или Tauri)
truth-training-windows/     # UI для Windows (WinUI или Tauri)
truth-training-android/     # UI для Android (Kotlin + JNI)
truth-training-apple/       # UI для macOS и iOS (SwiftUI + FFI)
```

---

## 🔧 Ядро (Rust)

* **Язык:** Rust
* **Фреймворки:** Actix-web, Tokio
* **База данных:** SQLite (через `rusqlite`)
* **Функции:**

  * Управление базой знаний.
  * Создание и обработка событий (`truth_events`).
  * Экспертная система (детектор лжи).
  * Синхронизация данных через P2P.

---

## 🌐 UI-платформы

### **Linux (truth-training-unix)**

* **Опции:** GTK (через `gtk-rs`) или Tauri (HTML + Rust backend).
* **Связь с ядром:**

  * Через HTTP API (Actix).
  * Либо прямой вызов функций через crate (если установлен локально).

### **Windows (truth-training-windows)**

* **Опции:**

  * **WinUI 3** (C# + Rust DLL через FFI).
  * **Tauri** (универсальный подход).
* **Связь:**

  * Через HTTP API.
  * Или через DLL + FFI.

### **Android (truth-training-android)**

* **Язык:** Kotlin + JNI.
* **Связь с ядром:**

  * Rust собирается в `.so` (через cargo-ndk).
  * JNI обертка для вызова функций ядра.

### **Apple (macOS/iOS) (truth-training-apple)**

* **Опции:**

  * SwiftUI + Rust через FFI (`dylib`).
  * Или Tauri для macOS.
* **Связь:**

  * Через FFI (вызовы Rust функций из Swift).
  * Для iOS нужен Rust cross-compilation.

---

## 📂 Интеграция и обновления

* Все UI-проекты подключают ядро как **Git submodule** или как **crate с crates.io**.
* Общие документы (`docs/`) хранятся в `truth-training`.

---

## 🖌 Mermaid-схема архитектуры

```mermaid
flowchart TB
    subgraph Core [Ядро (Rust)]
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

## 📄 Документы

* **architecture.md** (текущий файл) — схема модулей и связи.
* **ui\_guidelines.md** — правила интеграции UI с ядром.
* **build\_instructions.md** — инструкции по сборке ядра и UI.
