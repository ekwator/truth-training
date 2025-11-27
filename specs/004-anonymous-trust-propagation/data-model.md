# ATPP: Data Model (Phase 1)

## 1. Основные сущности

### Event
- **id**: string (уникальный хеш события; обрабатывается как content+signature_hash; PRIMARY, UNIQUE)
- **payload**: bytes (шифрованные данные события)
- **signature**: string (подпись отправителя, не раскрывает идентификацию персоны)
- **ttl**: integer (количество секунд/таймштамп жизни события; ограничение времени распространения)
- **created_at**: datetime (время создания)
- **propagation_state_id**: string (FOREIGN KEY → PropagationState.id)

### Node
- **public_key**: string (уникальный публичный ключ; не хранится явно в событиях)
- **peer_url**: string (адрес пира; не входит в события, используется только для доставки/маршрутизации)
- **last_seen**: datetime (для собственных нужд ноды; не распространяется по сети)

### PropagationState
- **id**: string (уникальный идентификатор состояния распространения; PRIMARY)
- **event_id**: string (FOREIGN KEY → Event.id)
- **count**: integer (количество ретрансляций через разные узлы)
- **witnesses**: list<string> (анонимизированные публичные ключи узлов, подтверждавших/ретранслировавших событие)
- **last_seen**: datetime (время последней ретрансляции/обновления статуса)
- **etf**: float (текущий показатель Emergent Trust Factor по расчету сети/peer-агрегациям)

## 2. Поля и ограничения

| Entity             | Field             | Type      | Unique | Required | Notes / Constraints                       |
|--------------------|-------------------|-----------|--------|----------|-------------------------------------------|
| Event              | id                | string    | YES    | YES      | content+signature hash, PRIMARY           |
| Event              | payload           | bytes     | NO     | YES      | encrypted w/o sender/recipient            |
| Event              | signature         | string    | NO     | YES      | верифицируется по сети                    |
| Event              | ttl               | integer   | NO     | YES      | конфигурируемый диапазон от 10c до 24ч    |
| Event              | created_at        | datetime  | NO     | YES      | ISO8601                                   |
| Event              | propagation_state_id | string | NO     | YES      | связь (один event–одно propagationState)  |
| Node               | public_key        | string    | YES    | YES      | public, но не раскрывается в payload      |
| Node               | peer_url          | string    | NO     | NO       | внутри Node DB, не сетевой event payload  |
| Node               | last_seen         | datetime  | NO     | NO       | только для собственной peer DB            |
| PropagationState   | id                | string    | YES    | YES      | PRIMARY                                   |
| PropagationState   | event_id          | string    | YES    | YES      | FOREIGN KEY                               |
| PropagationState   | count             | integer   | NO     | YES      | >=1                                       |
| PropagationState   | witnesses         | list<str> | NO     | NO       | опционально (без уточнения order)         |
| PropagationState   | last_seen         | datetime  | NO     | YES      | ISO8601                                   |
| PropagationState   | etf               | float     | NO     | YES      | динамический ETF [0..1]                   |

## 3. Валидационные правила
- Event.id: хеш должен быть уникален во всей сети (конфликт — reject).
- Event.payload: НЕ должен содержать открытых ID отправителя/получателя, только зашифрованные данные.
- Event.ttl: значение конфигурируемое, по умолчанию >= 10 секунд и <= 24 часа (86400 секунд).
- PropagationState.count: >= 1, увеличивается при каждой ретрансляции.
- PropagationState.witnesses: должны быть анонимизированы (только публичные ключи, без других метаданных).
- Node.public_key НЕ МОЖЕТ быть открыт в data payload (только для сигнатуры).

## 4. Карта связей (упрощённая ER):
```
Node 1–* Event 1–1 PropagationState
                 ∟—— [witnesses]: список Node.public_key (анонимно)
```
- Каждый event имеет одно уникальное propagation state.
- Один Node может создать много Event-ов, но связь только через signature (неявная — нельзя по payload восстановить автора!).
- Witnesses — набор public_key, не раскрывающий дополнительных данных о структуре сети.

## 5. Константы и спецификации
- **Лимит публикаций/ретрансляций по узлу**: по умолчанию 1/30s, конфигурируемый.
- **Диапазон TTL**: min 10 секунд, max 24ч (86400 сек).
- **ETF**: расчет определяется в фазе contracts/logic, диапазон [0..1].
- **Запрет ID/маршрутизации**: все payload без открытых адресов, маршрутов и любых дотождествлений. Вся сигнатура-анонимна, все идентификаторы скрыты.

_Version: v1.0.0_
