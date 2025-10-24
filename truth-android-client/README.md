# Truth Training — Android Client
Truth Android Client (v0.1.0-pre)
=================================

Требования:
- Android Studio (Giraffe+), JDK 17
- Android SDK 24+

Сборка:
```bash
./gradlew assembleLocalDebug
```

Базовая конфигурация:
- BASE_URL задаётся через BuildConfig и productFlavors:
  - local: `http://10.0.2.2:8080`
  - remote: замените `https://truth-core.example.com`

Интеграция с Truth Core (current main branch, post-v0.3.0 development):
- Эндпоинты: POST `/api/v1/auth`, GET `/api/v1/info`, `/api/v1/stats`, `/graph/json`, POST `/api/v1/refresh` (опц.)
- JWT хранится в SharedPreferences; авторизация через заголовок `Authorization: Bearer <token>`

Тесты:
```bash
./gradlew test
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

Ed25519 JSON Signatures:
- Подпись Ed25519 (BouncyCastle), единая для Android/Rust; Base64 без паддинга
- Пример конверта `/api/v1/push` и P2P:
```json
{
  "payload": { "event": "truth_claim", "value": 1 },
  "signature": "<base64>",
  "public_key": "<base64>"
}
```
- cURL пример:
```bash
curl -X POST "$BASE_URL/api/v1/push" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"payload":{"event":"truth_claim","value":1},"signature":"<b64>","public_key":"<b64>"}'
```
