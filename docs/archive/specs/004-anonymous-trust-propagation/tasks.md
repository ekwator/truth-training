<!-- Archived from [specs/004-anonymous-trust-propagation/tasks.md](specs/004-anonymous-trust-propagation/tasks.md) -->

# ATPP: Ordered Tasks

## Setup
T001. [X] Инициализировать проект/рабочую ветку для ATPP в src/p2p, src/models, src/services, src/tests
T002. [X] Настроить CI для выполнения контрактных и интеграционных тестов

## Contract Tests ([P] — параллельно)
T003. [X] Написать контрактный тест для создания события (tests/contract/test_event_creation.rs)
T004. [X] Написать контрактный тест для ретрансляции события (tests/contract/test_relay_event.rs)
T005. [X] Написать контрактный тест для наблюдения ETF (tests/contract/test_observe_etf.rs)
T006. [X] Написать контрактный тест для проверки TTL-истечения (tests/contract/test_ttl_expiry.rs)
T007. [X] Написать контрактный тест для rate limiting (tests/contract/test_rate_limit_violation.rs)

## Models ([P])
T008. [X] Реализовать модель Event (src/models/event.rs), покрыть unit-тестами
T009. [X] Реализовать модель PropagationState (src/models/propagation_state.rs), покрыть unit-тестами
T010. [X] Реализовать модель Node (src/models/node.rs), покрыть unit-тестами

## Endpoints
T011. [X] Реализовать endpoint POST /event — создание события, обработка payload/signature/ttl
T012. [X] Реализовать endpoint POST /event/relay — обработка ретрансляции, обновление propagation-state
T013. [X] Реализовать endpoint GET /event/etf — выдача ETF и propagation-state
T014. [X] Реализовать endpoint GET /event/ttl-status — выдача статуса TTL и отражения
T015. [X] Реализовать поведение rate-limiting для /event и /event/relay (core сервис, error 429)

## Integration Tests ([P])
T016. [X] E2E-тест: создание события с последующей ретрансляцией и проверкой propagation_state (tests/integration/e2e_create_relay.rs)
T017. [X] E2E-тест: автообновление ETF при ретрансляции (tests/integration/e2e_etf_flow.rs)
T018. [X] E2E-тест: проверка TTL-истечения и UI-месседжа (tests/integration/e2e_ttl_expiry.rs)
T019. [X] E2E-тест: rate-limit (2 события с одного узла <30с — ожидание 429) (tests/integration/e2e_rate_limit.rs)
T020. [X] E2E user-flow: verify UI update for ETF and not_confirmed message (integration с UI-API)

## Polish & Docs ([P])
T021. [X] Финализировать и описать миграции/инициализацию моделей для ATPP
T022. [X] Написать документацию для devs ([docs/atpp-impl.md](docs/atpp-impl.md)(d[ocs/atpp-impl.md](ocs/atpp-impl.md) с примерами использования API и настроек лимитов
T023. [X] Добавить/улучшить логи, мониторинг лимитов и событий ошибок
T024. [X] Провести code review & рефакторинг моделей/endpoint-ов

## Dependency Guide
- Все [P]-tasks без явных пересечений могут выполняться параллельно (contract tests, models, integration scenarios).
- Реализация endpoints возможна только после соответствующих моделей и прохода контрактных тестов.
- Polish-задачи и документация делаются после основных core и тестовых фич.

_Version: v1.0.0_

