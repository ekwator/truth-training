# Truth Training — Android Client
Truth Android Client v1.0.0
=================================

**Версия:** 1.0.0 (stable)  
**Статус:** ✅ Полное соответствие Desktop UI v1.0.0  
**Дата:** 2025-11-02

Требования:
- Android Studio (Giraffe+), JDK 17
- Android SDK 26+ (minSdk 26, targetSdk 33)
- Truth Core Server v1.0.0+

Сборка:
```bash
./gradlew assembleLocalDebug
```

Базовая конфигурация:
- BASE_URL задаётся через BuildConfig и productFlavors:
  - local: `http://10.0.2.2:8080`
  - remote: замените `https://truth-core.example.com`

## Основные возможности v1.0.0

### ✅ Полная функциональность
- **Room Database** - Offline-first архитектура с локальным SQLite хранилищем
- **Context Templates** - Создание, редактирование, поиск и использование шаблонов контекста
- **Events Management** - Полный CRUD для событий с embedded context fields (v1.0.0 API)
- **Judgments & Consensus** - Отправка суждений и просмотр статистики консенсуса
- **P2P Synchronization** - Прямая синхронизация событий между Android клиентами
- **Jetpack Compose UI** - Современный Material 3 интерфейс
- **Background Sync** - Автоматическая синхронизация через WorkManager

### Архитектура
- **Offline-First:** Все операции сохраняются локально, синхронизация в фоне
- **Room Database:** SQLite через Room с Flow поддержкой для reactive UI
- **Repository Pattern:** Единый слой доступа к данным (Room + API)
- **Sync Queue:** Отслеживание и автоматическая обработка pending операций

Интеграция с Truth Core v1.0.0:
- **API Endpoints:** Полная поддержка v1.0.0 endpoints (Events, Contexts, Judgments, Impacts)
- **Authentication:** JWT через `Authorization: Bearer <token>` header
- **Embedded Fields:** События используют `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` вместо `context_id`
- **Token Storage:** JWT в SharedPreferences, автоматическое обновление через RefreshAuthenticator

## Тестирование

Unit tests:
```bash
./gradlew test
```

Integration tests:
```bash
./gradlew connectedAndroidTest
```

Contract tests (API endpoints):
```bash
./gradlew test --tests "*Contract*"
```

Примечания по интеграции:
- Доп. материалы см. в `truthcore_api/api_reference_link.md` и в репозитории Truth Core.

Mock-сборка:
- Запуск: `./gradlew assembleMockDebug`
- Источники: `app/src/mock/assets/api/*.json`
- Реализация: `MockTruthApi`, включается при flavor `mock`.

Взаимодействие с Truth Core из Android:
- Экран `MainDashboardActivity` предоставляет кнопки для действий:
  - Sync Peers, Submit Claim, Get Claims, Analyze Text, Get Stats
- Ответы отображаются как JSON на экране
- Пример запроса: `{"action":"get_stats"}`

Local P2P Discovery:
- Обнаружение пиров через NSD (`_truthnode._tcp.`), запуск локального сервера и обмен JSON.
- Экран `P2PActivity`: список пиров (LAN), отправка ping/произвольного JSON, вывод ответа.
- Требования: устройства в одной Wi‑Fi сети; разрешения сети в `AndroidManifest.xml`.

Secure P2P Messaging:
- Генерация Ed25519-ключей в Android Keystore (alias `truth_node_key`, 2048-bit)
- Каждое исходящее сообщение подписывается и содержит поля `signature` и `public_key` (Base64)
- Сервер проверяет подпись; при недействительной подписи отвечает `{ "status": "error", "reason": "invalid_signature" }`
- На экране `P2PActivity` показывается окончание публичного ключа для быстрой идентификации
- The Rust core now verifies message signatures (RSA/Ed25519) for all incoming JSON packets from Android before further processing.

## Структура проекта

### Room Database
- `data/database/TruthDatabase.kt` - главная база данных
- `data/database/entities/` - EventEntity, ContextTemplateEntity, JudgmentEntity, ImpactEntity, SummaryEntity, SyncQueueEntity
- `data/database/daos/` - DAO интерфейсы с Flow поддержкой

### Repositories
- `data/repository/EventRepository.kt` - управление событиями (offline-first)
- `data/repository/ContextTemplateRepository.kt` - управление шаблонами контекста
- `data/repository/JudgmentRepository.kt` - управление суждениями и консенсусом
- `data/repository/ImpactRepository.kt` - управление воздействиями
- `data/repository/SummaryRepository.kt` - управление резюме

### Sync Infrastructure
- `data/sync/SyncQueueManager.kt` - управление очередью синхронизации
- `data/sync/SyncWorker.kt` - WorkManager worker для фоновой синхронизации

### P2P
- `p2p/P2PSyncManager.kt` - распространение событий через P2P
- `p2p/P2PMessageHandler.kt` - обработка зашифрованных P2P сообщений
- `p2p/P2PDiscoveryService.kt` - обнаружение peer'ов через NSD

### UI (Jetpack Compose)
- `ui/compose/events/` - экраны событий (список, создание, детали)
- `ui/compose/contexts/` - экраны шаблонов контекста (список, редактор, выбор)
- `ui/compose/judgments/` - экраны суждений (список, отправка)

## Ed25519 P2P Signatures

Подпись Ed25519 (BouncyCastle) для P2P сообщений:
- Каждое сообщение подписывается перед отправкой
- Формат конверта:
```json
{
  "payload": { "type": "EVENT_SYNC", "event_id": "...", ... },
  "signature": "<base64>",
  "public_key": "<base64>"
}
```

## Миграция с v0.3.0

Подробное руководство по миграции см. в `docs/ANDROID_MIGRATION.md`.

Основные изменения:
- Версия: `0.3.0` → `1.0.0`
- minSdk: `24` → `26`
- Room Database для offline-first режима
- Jetpack Compose UI
- Embedded context fields вместо `context_id`

## Дополнительная документация

- **Сравнение с Desktop:** `docs/COMPARISON_ANDROID_VS_DESKTOP.md`
- **Миграция:** `docs/ANDROID_MIGRATION.md`
- **Спецификация:** `specs/007-title-align-truth/spec.md`
- **Data Model:** `specs/007-title-align-truth/data-model.md`
- **API Contracts:** `specs/007-title-align-truth/contracts/openapi.yaml`
