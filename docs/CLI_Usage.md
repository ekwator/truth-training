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

