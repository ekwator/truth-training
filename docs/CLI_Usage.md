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

## Формат ключей
`keys/node1.json`:
```json
{
  "private_key": "b3f7c97e6aeb...",
  "public_key": "a1f45d6f12c3..."
}
```

