# truthctl CLI

Командная утилита для P2P‑синхронизации и обслуживания узла Truth Training.

## Установка и запуск

Сборка с клиентскими p2p‑функциями:
```bash
cargo build --bin truthctl --features p2p-client-sync
```

## Основные команды

### Синхронизация

Полная синхронизация с конкретным пиром:
```bash
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode full
```

Инкрементальная синхронизация:
```bash
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode incremental
```

Синхронизация со всеми известными пирами:
```bash
truthctl peers sync-all --mode full
truthctl peers sync-all --mode incremental --dry-run
```

### Статус узла

```bash
truthctl status --db truth.db --identity keys/node1.json
```

Вывод включает:
- Имя ноды и порт (из `~/.truthctl/config.json`)
- Путь к БД
- Количество пиров (из `~/.truthctl/peers.json`)
- Последние 5 записей синхронизации из таблицы `sync_logs`
- Для свежей БД выводит предупреждение: "No sync history yet."

## Key Management

### Генерация ключей
```bash
truthctl keys generate
# вывод:
# private: <64-hex>
# public:  <64-hex>

# сохранить в локальное хранилище (~/.truthctl/keys.json)
truthctl keys generate --save
```

### Импорт ключей
```bash
truthctl keys import <priv_hex> <pub_hex>
truthctl keys list
```

## Node Initialization

Создание конфигурации узла:
```bash
truthctl init-node <node_name> --port 8080 --db truth.db --auto-peer
```

Создаваемые файлы:
- `~/.truthctl/config.json`:
```json
{
  "node_name": "mynode",
  "port": 8080,
  "db_path": "truth.db",
  "public_key": "<hex>",
  "private_key": "<hex>"
}
```
- `~/.truthctl/peers.json` (при `--auto-peer`):
```json
{
  "peers": [
    { "url": "http://127.0.0.1:8080", "public_key": "<hex>" }
  ]
}
```

## Peer Management

### Добавление и просмотр пиров
```bash
truthctl peers list
truthctl peers add http://127.0.0.1:8081 <pub_hex>
```

### Синхронизация со всеми пирами
```bash
# Полная двунаправленная
truthctl peers sync-all --mode full

# Инкрементальная
truthctl peers sync-all --mode incremental

# Сухой прогон
truthctl peers sync-all --mode full --dry-run
```

## Configuration Management

Управление конфигурацией узла (`~/.truthctl/config.json`):
```bash
truthctl config show
truthctl config set <key> <value>
truthctl config reset [--confirm]
```

Поддерживаемые ключи:
- `node_name`
- `port` (u16)
- `database` (путь к БД)
- `auto_peer` (boolean)
- `p2p_enabled` (boolean)

## Trust Propagation & Ratings

Просмотр доверия и изменений:
```bash
truthctl ratings trust [--verbose]
```

- Локальный уровень доверия — среднее значение `trust_score` по `node_ratings`
- Средняя сеть — `group_ratings.global.avg_score`
- В подробном режиме показывает образцы изменений с цветовой индикацией: 🟢 + (рост), 🔴 – (падение), ⚪ = (без изменений)

Механика распространения доверия (выполняется прозрачно при `/sync` и `/incremental_sync`):
- Формула смешивания: `new = local*0.8 + remote*0.2` (с обрезкой в диапазон [-1, 1])
- Временной спад: если `last_updated` старше 7 дней, применяется коэффициент 0.9

## Logs

Просмотр и очистка журнала синхронизации:
```bash
truthctl logs show --limit 100 --db truth.db
truthctl logs clear --db truth.db
```

Столбцы: id, timestamp, peer_url, mode, status, details. Записи создаются автоматически после каждой попытки `peers sync-all`.

## Diagnostics and Reset

### Проверка состояния узла
```bash
truthctl diagnose [--verbose]
truthctl diagnose --server [--verbose]
```

Выводит:
- Локальные проверки (`diagnostics.rs`): конфиг, ключи, пиры, база и состояние фичи `p2p-client-sync`. При `--verbose` печатает JSON (`config`, `peers`, `keys`).
- При `--server` запускает серверные проверки из `truth_core::server_diagnostics`:
  - **API**: доступность HTTP маршрута `/health`
  - **Database**: возможность открыть SQLite и выполнить чтение
  - **P2P**: статус слушателя UDP 37020 (если включён)

### Сброс локальных данных
```bash
truthctl reset-data [--confirm] [--reinit]
```

Шагает по очистке: удаляет SQLite БД, вызывает очистку журналов синхронизации, и при подтверждении удаляет `~/.truthctl/peers.json`. Печатает: `🧹 Node data cleared successfully.`

### Interactive Reinitialization

Флаг `--reinit` после очистки:
- Проверяет наличие ключевой пары в `~/.truthctl/keys.json`
- Если нет — генерирует Ed25519 и сохраняет, выводя `🔑 New keypair generated.`
- Если есть — предлагает:
```
A keypair already exists.
Do you want to:
[1] Keep existing key
[2] Generate new key and replace old one
Enter choice [1/2]:
```
При выборе `[2]` генерирует и заменяет пару (`🔁 Keypair replaced.`).

Далее автоматически выполняет:
```bash
truthctl init-node <node_name> --port <port> --db <db_path> --auto-peer
```
и печатает `🚀 Node reinitialized successfully.`

## Примечания
## HTTP API: новые эндпоинты для мобильной интеграции

- GET `/api/v1/info` — возвращает информацию об узле:
  - `node_name`, `version`, `p2p_enabled`, `db_path`, `peer_count`

- GET `/api/v1/stats` — агрегированная статистика БД:
  - `events`, `statements`, `impacts`, `node_ratings`, `group_ratings`, `avg_trust_score`

Документация OpenAPI доступна на `/api/docs` (Swagger UI) и `/api/docs/openapi.json` (JSON).

Примечание по безопасности: CORS включён для отладки (разрешены все источники, методы и заголовки). В продакшене обязательно включайте HTTPS и ограничивайте CORS до доверенных доменов.

- Команды `sync` и `verify` по умолчанию используют первый доступный ключ из локального хранилища, если явный файл не указан флагом `--identity`
- Формат ключей — hex (32 байта для приватного и публичного ключа Ed25519)
- Все команды поддерживают цветной вывод для лучшей читаемости

