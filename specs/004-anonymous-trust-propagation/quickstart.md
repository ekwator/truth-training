# ATPP: Quickstart (Минимальный e2e-тест)

## 1. Цель
Проверить базовый end-to-end поток ATPP: создание события, ретрансляция, расчёт ETF, TTL-экспирация и реакция rate-limit.

## 2. Минимальные шаги

### 1. Сгенерировать ключи ноды:
```
atpp-cli gen-keypair > keys.json
```
(ожидаемый результат: JSON c открытым/закрытым ключом)

### 2. Создать событие (event) с TTL:
```
atpp-cli create-event --payload="<binary>" --ttl=60 --key=keys.json > event.json
```
(ожидаемый результат: ID события, статус created/pending)

### 3. Ретранслировать событие через peer:
```
atpp-cli relay-event --event=event.json --peer-url=https://peer1.example > relayed.json
```
(ожидаемый результат: propagation_state updated: count>=2)

### 4. Наблюдать ETF в UI или через API:
```
atpp-cli observe-etf --event-id=<event_id>
# или открыть UI, убедиться, что ETF отображается и обновляется
```
(ETF = float [0..1], должен меняться при ретрансляциях)

### 5. Проверить истечение TTL:
- Ждать TTL+секunda
- Обновить UI/API: должно появиться сообщение «Cобытие не было подтверждено/доверено», если оно не было отражено обратно

### 6. Проверка срабатывания rate limiting:
- Совершить публикацию более 1 события менее чем за 30 секунд от одного узла
- Ожидать ошибку: status=blocked, message="rate limit exceeded", retry_after > 0

## 3. Требуемые параметры:
- Лимит публикаций по умолчанию: 1/30s (настройка через конфиг)
- TTL: от 10c до 24ч

## 4. Результат
Если все пункты работают как описано — реализация протокола ATPP проходит quickstart/минимальный smoke-test.

_Version: v1.0.0_
