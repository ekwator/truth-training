# Android Client Migration Guide: v0.3.0 → v1.0.0

**Дата:** 2025-11-02  
**Версия:** Android Client v1.0.0

---

## 📋 Обзор изменений

Android клиент обновлен с v0.3.0 (pre-release) до v1.0.0, достигнув функционального соответствия с Desktop UI v1.0.0.

### Основные изменения

1. **Версия:** `0.3.0` → `1.0.0`
2. **minSdk:** `24` → `26`
3. **targetSdk:** `35` → `33`
4. **Архитектура:** Добавлена Room Database (SQLite) для offline-first режима
5. **UI Framework:** Добавлена поддержка Jetpack Compose
6. **API Compatibility:** Полная поддержка v1.0.0 endpoints с embedded context fields

---

## 🏗️ Архитектурные изменения

### Room Database

**Новое:** Полная интеграция Room Database для локального хранения данных.

**Файлы:**
- `data/database/TruthDatabase.kt` - главная база данных
- `data/database/entities/` - 6 entity классов (Event, ContextTemplate, Judgment, Impact, Summary, SyncQueue)
- `data/database/daos/` - 6 DAO интерфейсов с Flow поддержкой

**Версии схемы Room Database:**
- **Version 1**: Базовая схема с основными таблицами (events, contexts, judgments, impacts, summaries, sync_queue)
- **Version 2**: Добавлены knowledge base entities (category, cause, develop, effect, forma, impact_type, progress_metrics) и обновлена структура EventEntity для использования embedded context fields
- Миграция 1→2 определена в `TruthDatabaseMigrations.kt` (MIGRATION_1_2)

**Миграция:**
- Старые SharedPreferences остаются для токенов
- Все данные событий, шаблонов, суждений хранятся в Room
- Автоматическая синхронизация с сервером через WorkManager
- Room автоматически применяет миграции при обновлении схемы

### Offline-First Architecture

**Новое:** Все операции сохраняются локально в Room перед синхронизацией с сервером.

**Компоненты:**
- `SyncQueueManager` - управление очередью синхронизации
- `SyncWorker` (WorkManager) - фоновая синхронизация
- `SyncQueueEntity` - отслеживание pending операций

**Поведение:**
- Операции CREATE/UPDATE/DELETE сохраняются локально немедленно
- Операции ставятся в очередь для синхронизации
- Фоновая синхронизация при доступности сети

### P2P Synchronization

**Новое:** Прямая синхронизация событий между Android клиентами.

**Компоненты:**
- `P2PSyncManager` - распространение событий через P2P
- `P2PMessageHandler` - обработка зашифрованных P2P сообщений
- Интеграция с существующим `P2PDiscoveryService`

**Функциональность:**
- Автоматическое обнаружение peer'ов через NSD
- Распространение событий через Ed25519 зашифрованные сообщения
- Local-wins conflict resolution

---

## 🔄 API Changes

### Embedded Context Fields (v1.0.0)

**Изменение:** `context_id` заменен на embedded поля в событиях.

**Старый формат (v0.3.0):**
```kotlin
data class Event(
    val context_id: Int?  // FK to contexts table
)
```

**Новый формат (v1.0.0):**
```kotlin
data class Event(
    val category_id: Int?,
    val forma_id: Int?,
    val cause_id: Int?,
    val develop_id: Int?,
    val effect_id: Int?
)
```

**Миграция данных:**
- Существующие события с `context_id` должны быть мигрированы
- Новые события создаются только с embedded полями
- API автоматически обрабатывает оба формата (обратная совместимость на сервере)

### Context Templates API

**Новое:** Полная поддержка Context Templates CRUD операций.

**Endpoints:**
- `GET /api/v1/contexts` - список шаблонов
- `POST /api/v1/contexts` - создание шаблона (с duplicate detection)
- `POST /api/v1/contexts/match` - поиск шаблона по полям
- `POST /api/v1/contexts/from-event` - создание шаблона из события

### Judgments API

**Новое:** Поддержка суждений с consensus statistics.

**Endpoints:**
- `POST /api/v1/judgments` - отправка суждения
- `GET /api/v1/judgments?event_id={id}` - список суждений для события
- `GET /api/v1/judgments/stats/{event_id}` - статистика консенсуса

---

## 🎨 UI Changes

### Jetpack Compose

**Новое:** Современный UI на базе Jetpack Compose.

**Экраны:**
- `EventListScreen` - список событий
- `EventCreateScreen` / `EventDetailScreen` - создание/просмотр событий
- `ContextTemplateListScreen` / `ContextTemplateEditorScreen` - управление шаблонами
- `JudgmentListScreen` / `JudgmentSubmissionScreen` - суждения и консенсус

**Навигация:**
- `MainNavigation` - Compose Navigation с NavHost
- Material 3 Design System

**Миграция:**
- Старые XML-based activities остаются для обратной совместимости
- Новые экраны используют Compose
- MainActivity обновлен для поддержки Compose

---

## 📦 Зависимости

### Новые зависимости

```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.6")
```

### Обновленные версии

- Kotlin: 2.0.20 (без изменений)
- Retrofit: 2.11.0 (без изменений)
- OkHttp: 4.12.0 (без изменений)

---

## 🚀 Миграция шаги

### 1. Обновление зависимостей

```bash
cd truth-android-client
./gradlew clean build
```

### 2. Миграция данных (если требуется)

**Автоматическая миграция:**
- Room автоматически обрабатывает миграцию схемы
- SharedPreferences данные остаются без изменений
- Существующие события могут потребовать обновления `context_id` → embedded fields

**Ручная миграция (если есть данные v0.3.0):**
```kotlin
// Пример миграции context_id → embedded fields
// Должна быть выполнена при первом запуске v1.0.0
```

### 3. Обновление Application class

**Новое:** `TruthTrainingApplication` инициализирует Room database.

```kotlin
// AndroidManifest.xml
<application
    android:name=".TruthTrainingApplication"
    ...>
```

### 4. Тестирование

**Рекомендуется:**
1. Выполнить integration tests (`./gradlew connectedAndroidTest`)
2. Проверить offline-first режим (отключить сеть, создать событие)
3. Проверить синхронизацию (включить сеть, дождаться sync)
4. Проверить P2P синхронизацию (2 устройства на одной сети)

---

## ⚠️ Breaking Changes

### 1. Context ID → Embedded Fields

**Изменение:** События больше не используют `context_id`.

**Миграция:**
- Старые события с `context_id` должны быть обновлены
- Новые события создаются только с embedded полями
- API v1.0.0 не поддерживает `context_id`

### 2. minSdk Increase

**Изменение:** `minSdk 24` → `minSdk 26`.

**Последствия:**
- Android 7.0 (API 24-25) больше не поддерживается
- Требуется минимум Android 8.0 (API 26)

### 3. Application Class Required

**Изменение:** Требуется `TruthTrainingApplication` для Room инициализации.

**Миграция:**
- Обновить `AndroidManifest.xml` с `android:name=".TruthTrainingApplication"`
- Удалить ручную инициализацию database из activities

---

## ✅ Чеклист миграции

- [ ] Обновить `app/build.gradle.kts` с новыми зависимостями
- [ ] Обновить `AndroidManifest.xml` с Application class
- [ ] Проверить совместимость minSdk 26
- [ ] Выполнить миграцию данных (если требуется)
- [ ] Обновить тесты для новых компонентов
- [ ] Проверить offline-first режим
- [ ] Протестировать P2P синхронизацию
- [ ] Обновить документацию проекта

---

## 📚 Дополнительные ресурсы

- **Specification:** `specs/007-title-align-truth/spec.md`
- **Data Model:** `specs/007-title-align-truth/data-model.md`
- **API Contracts:** `specs/007-title-align-truth/contracts/openapi.yaml`
- **Quickstart:** `specs/007-title-align-truth/quickstart.md`

---

**Статус:** ✅ Migration complete - Android v1.0.0 готов к использованию.

