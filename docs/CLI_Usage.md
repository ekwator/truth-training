# truthctl CLI

Командная утилита для P2P‑синхронизации и обслуживания узла.

## Установка и запуск

Сборка с клиентскими p2p‑функциями:
```bash
cargo build --bin truthctl --features p2p-client-sync
```

## Примеры

Полная синхронизация:
```bash
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode full
```

Инкрементальная синхронизация:
```bash
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode incremental
```

Только push:
```bash
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode push
```

Только pull:
```bash
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode pull
```

Статус узла:
```bash
truthctl status --db truth.db --identity keys/node1.json
```
Вывод включает имя ноды и порт (из `~/.truthctl/config.json`), путь к БД, количество пиров (из `~/.truthctl/peers.json`) и последние 5 записей синхронизации из таблицы `sync_logs`. Для свежей БД выводит предупреждение: "No sync history yet.".

## Key Generation
Генерация новой пары Ed25519 (hex):
```bash
truthctl keys generate
# вывод:
# private: <64-hex>
# public:  <64-hex>

# сохранить в локальное хранилище (~/.truthctl/keys.json)
truthctl keys generate --save
```

## Node Initialization
Создание конфигурации узла и файлов:
```bash
truthctl init-node <node_name> --port 8080 --db truth.db --auto-peer
```
Файлы:
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

## Configuration Management
Управление пользовательской конфигурацией узла (`~/.truthctl/config.json`):
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

## Peer Auto-Registration
Флаг `--auto-peer` автоматически добавляет локальный узел в `peers.json`.
Пример записи:
```json
{ "url": "http://127.0.0.1:8080", "public_key": "<hex>" }
```

## Управление ключами
Импорт и список локальных ключей (хранятся в `~/.truthctl/keys.json`):
```bash
truthctl keys import <priv_hex> <pub_hex>
truthctl keys list
```

## Peer Management
Работа со списком пиров (`~/.truthctl/peers.json`):
```bash
truthctl peers list
truthctl peers add http://127.0.0.1:8081 <pub_hex>
```

## Network Sync
Синхронизация со всеми известными пирами:
```bash
# Полная двунаправленная
truthctl peers sync-all --mode full

# Инкрементальная за последний час
truthctl peers sync-all --mode incremental

# Сухой прогон
truthctl peers sync-all --mode full --dry-run
```

## Logs
Просмотр и очистка журнала синхронизации:
```bash
truthctl logs show --limit 100 --db truth.db
truthctl logs clear --db truth.db
```
Столбцы: id, timestamp, peer_url, mode, status, details. Записи создаются автоматически после каждой попытки `peers sync-all`.

## Diagnostics and Reset

Проверка состояния узла:
```bash
truthctl diagnose [--verbose]
```
Выводит цветной сводный отчёт по конфигу, ключам, пирам, базе данных и фиче `p2p-client-sync`. При `--verbose` печатает полный JSON (`config`, `peers`, `keys`).

Сброс локальных данных:
```bash
truthctl reset-data [--confirm] [--reinit]
```
Шагает по очистке: удаляет SQLite БД, вызывает очистку журналов синхронизации, и при подтверждении удаляет `~/.truthctl/peers.json`. Печатает: `🧹 Node data cleared successfully.`

### Interactive Reinitialization

Флаг `--reinit` после очистки:
- Проверяет наличие ключевой пары в `~/.truthctl/keys.json`.
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

Примечания:
- Команды `sync` и `verify` по умолчанию используют первый доступный ключ из локального хранилища, если явный файл не указан флагом `--identity`.
- Формат ключей — hex (32 байта для приватного и публичного ключа Ed25519).

## Формат ключей
`keys/node1.json`:
```json
{
  "private_key": "b3f7c97e6aeb...",
  "public_key": "a1f45d6f12c3..."
}
```

