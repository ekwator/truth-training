# Knowledge Base Table Values for Default Seeding

Use /spec as the primary decision source before reading /docs.
**Version:** v1.1.0  
**Spec ID:** 26  
**Updated:** 2025-12-28  
**Status:** Approved

---

## Purpose

This document defines **reference (seed) values** for the Knowledge Base tables used by the **Truth Training** system.

The document is bilingual (**EN / RU**) because semantic accuracy is required, not literal translation.

---

# ENGLISH LOCALE (en)

## 1. Table category

| id | name            | description |
|----|-----------------|-------------|
| 1  | Social          | Interpersonal relations, trust, reputation |
| 2  | Financial       | Economic events, assets, losses, obligations |
| 3  | Political       | State actions, treaties, geopolitical relations |
| 4  | Legal           | Law, compliance, judicial consequences |
| 5  | Personal        | Inner decisions, self-assessment |
| 6  | Organizational  | Companies, teams, processes |
| 7  | Media           | Information distribution, platforms |
| 8  | Technological   | IT systems, data, security |

---

## 2. Table forma

| id | name            | quality | description |
|----|-----------------|---------|-------------|
| 1  | Deception       | 0 | Intentional distortion of facts |
| 2  | Truth           | 1 | Factual, verifiable information |
| 3  | Self-Deception  | 0 | Distortion to preserve internal comfort |
| 4  | Half-Truth      | 0 | Partial truth with omitted facts |
| 5  | Silence         | 0 | Withholding significant information |
| 6  | Openness        | 1 | Proactive full disclosure |

---

## 3. Table cause

| id | name        | quality | description |
|----|-------------|---------|-------------|
| 1  | Fear        | 0 | Avoidance of punishment or loss |
| 2  | Greed       | 0 | Personal or material gain |
| 3  | Mercy       | 1 | Compassion toward others |
| 4  | Ignorance   | 0 | Lack of knowledge |
| 5  | Duty        | 1 | Moral or contractual obligation |
| 6  | Curiosity   | 1 | Desire to understand reality |
| 7  | Pressure    | 0 | External coercion |
| 8  | Care        | 1 | Protection of another’s well-being |
| 9  | Hate        | 0 | Hostile Attitude |
| 10 | Love        | 1 | Positive Attachment |
| 11 | System      | 0 | Process Inertia |
| 12 | Algorithm   | 1 | Deterministic Calculation |
| 13 | Randomness  | 0 | Lack of Pattern |
---

## 4. Table develop

| id | name           | quality | description |
|----|----------------|---------|-------------|
| 1  | Concealment    | 0 | Hiding relevant facts |
| 2  | Manipulation   | 0 | Context distortion, influence |
| 3  | Transparency   | 1 | Open availability of data |
| 4  | Verification   | 1 | Cross-checking sources |
| 5  | Exaggeration   | 0 | Artificial amplification |
| 6  | Confession     | 1 | Admission and correction |
| 7  | Disclosure     | 1 | Making information available |
| 8  | Control        | 1 | Consciously managing the process |
| 9  | Forgetting     | 0 | Losing or ignoring the event |
| 10 | Belief         | 0 | Acceptance without verification |

---

## 5. Table effect

| id | name                  | quality | description |
|----|-----------------------|---------|-------------|
| 1  | Distrust              | 0 | Loss of confidence |
| 2  | Trust                 | 1 | Strengthened cooperation |
| 3  | Conflict              | 0 | Escalation of tension |
| 4  | Reconciliation        | 1 | Reduction of conflict |
| 5  | Sanctions             | 0 | Legal or reputational penalties |
| 6  | Learning              | 1 | Knowledge growth |
| 7  | Reputation Loss       | 0 | Decrease in status |
| 8  | Reputation Gain       | 1 | Increase in status |
| 9  | Loss                  | 0 | Financial or material damage |
| 10 | Profit                | 1 | Financial or material growth |
| 11 | Destruction           | 0 | Loss of integrity |
| 12 | Stability             | 1 | Maintenance of integrity |
| 13 | Degradation           | 0 | Loss of ability to improve |
| 14 | Preferences           | 1 | Incentives and benefits |

---

## 6. Table contexts

| id | name                                   | category_id | forma_id | cause_id | develop_id | effect_id | description |
|----|----------------------------------------|-------------|----------|----------|------------|-----------|-------------|
| 1 | Interpersonal: openness                 | 1           | 6        | 5        | 3          | 2         | Proactive full disclosure leading to trust growth |
| 2  | Interpersonal: concealment              | 1           | 1        | 1        | 1          | 1         | Withholding facts causing trust loss |
| 3 | Interpersonal: manipulation             | 1           | 1        | 7        | 2          | 3         | Intentional distortion of facts through manipulation escalating conflict |
| 4 | Financial: fraud                        | 2           | 1        | 2        | 5          | 9         | Intentional distortion of facts for personal gain resulting in financial loss |
| 5  | Financial: risky transparency           | 2           | 2        | 5        | 3          | 6         | Risky transparency causing instability |
| 6 | Financial: transparent reporting        | 2           | 2        | 5        | 4          | 8         | Factual, verifiable information with cross-checking sources improving reputation |
| 7 | Politics: treaty breach                 | 3           | 1        | 2        | 1          | 1         | Intentional distortion of facts about treaty compliance leading to loss of trust |
| 8 | Politics: strategic ambiguity           | 3           | 1        | 7        | 2          | 3         | Intentional distortion of facts through ambiguous statements increasing tension |
| 9 | Politics: treaty compliance             | 3           | 2        | 5        | 4          | 2         | Factual, verifiable information with cross-checking sources confirming execution of obligations |
| 10 | Organization: admitting a mistake       | 6           | 2        | 5        | 6          | 6         | Factual, verifiable information with admission and correction improve learning |
| 11 | Organization: hiding an error           | 6           | 1        | 1        | 1          | 5         | Concealed failure causing systemic damage |
| 12 | Media: disinformation                   | 7           | 1        | 7        | 2          | 3         | Intentional distortion of facts through manipulation leading to conflict |
| 13 | Media: fact-checking                    | 7           | 2        | 5        | 4          | 2         | Factual, verifiable information with cross-checking sources restoring public trust |
| 14 | Technology: security disclosure         | 8           | 2        | 5        | 3          | 6         | Factual, verifiable information with open availability of data preventing escalation |
| 15 | Technology: vulnerability concealment   | 8           | 1        | 1        | 1          | 9         | Hidden flaws leading to major financial losses |

---

## 7. Table time_axes

| id | time_type | description                        |
|----|-----------|------------------------------------|
| 1  | past      | Historical chronological time      |
| 2  | present   | Current real-time                  |
| 3  | future    | Predicted or scheduled time        |

---

# RUSSIAN LOCALE (ru)

## 1. Table category (категория)

| id | name              | description |
|----|-------------------|-------------|
| 1  | Социальное        | Межличностные отношения, доверие |
| 2  | Финансовое        | Экономические события, потери, обязательства |
| 3  | Политическое      | Государственные и международные процессы |
| 4  | Правовое          | Закон, соблюдение норм, суд |
| 5  | Личное            | Внутренние решения и оценки |
| 6  | Организационное   | Компании, команды, процессы |
| 7  | Медийное          | Распространение информации |
| 8  | Технологическое   | ИТ-системы, данные, безопасность |

---

## 2. Table forma (форма)

| id | name            | quality | description |
|----|-----------------|---------|-------------|
| 1  | Обман           | 0 | Сознательное искажение фактов |
| 2  | Правда          | 1 | Проверяемая фактическая информация |
| 3  | Самообман       | 0 | Искажение для психологического комфорта |
| 4  | Полуправда      | 0 | Частичное сокрытие |
| 5  | Умолчание       | 0 | Сокрытие значимой информации |
| 6  | Открытость      | 1 | Полное добровольное раскрытие |

---

## 3. Table cause (причина)

| id | name         | quality | description |
|----|--------------|---------|-------------|
| 1  | Страх        | 0 | Избежание потерь или наказания |
| 2  | Жадность     | 0 | Личный или материальный интерес |
| 3  | Милосердие   | 1 | Сострадание |
| 4  | Неведение    | 0 | Недостаток знаний |
| 5  | Долг         | 1 | Моральная или формальная обязанность |
| 6  | Любопытство  | 1 | Стремление к пониманию |
| 7  | Давление     | 0 | Внешнее принуждение |
| 8  | Забота       | 1 | Защита блага другого |
| 9  | Ненависть    | 0 | Враждебная установка |
| 10 | Любовь       | 1 | Позитивная привязанность |
| 11 | Система      | 0 | Инерция процессов |
| 12 | Алгоритм     | 1 | Детерминированный расчёт |
| 13 | Случайность  | 0 | Отсутствие закономерности |
---

## 4. Table develop (развитие)

| id | name           | quality | description |
|----|----------------|---------|-------------|
| 1  | Сокрытие       | 0 | Утаивание фактов |
| 2  | Манипуляция    | 0 | Искажение контекста |
| 3  | Прозрачность   | 1 | Открытый доступ к информации |
| 4  | Проверка       | 1 | Сопоставление источников |
| 5  | Преувеличение  | 0 | Искусственное усиление |
| 6  | Признание      | 1 | Принятие ответственности |
| 7  | Разглашение    | 1 | Сделать информацию доступной |
| 8  | Управление     | 1 | Осознанное управление процессом |
| 9  | Забвение       | 0 | Утрата или игнорирование события |
| 10 | Вера           | 0 | Принятие без проверки |

---

## 5. Table effect (последствие)

| id | name                   | quality | description |
|----|------------------------|---------|-------------|
| 1  | Недоверие              | 0 | Потеря доверия |
| 2  | Доверие                | 1 | Укрепление связей |
| 3  | Конфликт               | 0 | Эскалация напряжения |
| 4  | Примирение             | 1 | Снижение конфликта |
| 5  | Санкции                | 0 | Юридические или репутационные меры |
| 6  | Обучение               | 1 | Рост понимания |
| 7  | Потеря репутации       | 0 | Снижение статуса |
| 8  | Рост репутации         | 1 | Повышение статуса |
| 9  | Убыток                 | 0 | Финансовый или материальный ущерб |
| 10 | Прибыль                | 1 | Финансовый или материальный рост  |
| 11 | Разрушение             | 0 | Потеря целостности |
| 12 | Стабильность           | 1 | Поддержание целостности |
| 13 | Деградация             | 0 | Утрата способности к улучшению |
| 14 | Преференции            | 1 | Поощрения и преимущества |

---

## 6. Table сontexts (шаблон контекста)

| id | name                                   | category_id | forma_id | cause_id | develop_id | effect_id | description |
|----|----------------------------------------|-------------|----------|----------|------------|-----------|-------------|
| 1 | Межличностные: открытость               | 1           | 6        | 5        | 3          | 2         | Добровольное раскрытие приводит к росту доверия |
| 2  | Межличностные: сокрытие                 | 1           | 1        | 1        | 1          | 1         | Утаивание фактов вызывает потерю доверия |
| 3 | Межличностные: манипуляция              | 1           | 1        | 7        | 2          | 3         | Сознательное искажение фактов через манипуляцию ведёт к конфликту |
| 4 | Финансовые: мошенничество               | 2           | 1        | 2        | 5          | 9         | Сознательное искажение фактов в целях личной выгоды приводит к финансовым потерям |
| 5 | Финансовые: рискованная прозрачность    | 2           | 2        | 5        | 3          | 6         | Проверяемая фактическая информация с открытым доступом создаёт нестабильность, но ведёт к обучению |
| 6 | Финансовые: прозрачная отчётность       | 2           | 2        | 5        | 4          | 8         | Проверяемая фактическая информация с сопоставлением источников повышает репутацию |
| 7 | Политика: нарушение договора            | 3           | 1        | 2        | 1          | 1         | Сознательное искажение фактов о соблюдении договора ведёт к падению доверия |
| 8 | Политика: стратегическая неоднозначность| 3           | 1        | 7        | 2          | 3         | Сознательное искажение фактов через неоднозначные заявления усиливает напряжение |
| 9 | Политика: соблюдение договора           | 3           | 2        | 5        | 4          | 2         | Проверяемая фактическая информация с сопоставлением источников подтверждает выполнение обязательств |
| 10 | Организация: признание ошибки           | 6           | 2        | 5        | 6          | 6         | Проверяемая фактическая информация с признанием и исправлением усиливают обучение |
| 11 | Организация: сокрытие ошибки            | 6           | 1        | 1        | 1          | 5         | Скрытая ошибка приводит к системному ущербу |
| 12 | Медиа: дезинформация                    | 7           | 1        | 7        | 2          | 3         | Сознательное искажение фактов через манипуляции приводит к конфликтам |
| 13 | Медиа: фактчекинг                       | 7           | 2        | 5        | 4          | 2         | Проверяемая фактическая информация с сопоставлением источников восстанавливает доверие |
| 14 | Технологии: раскрытие уязвимости        | 8           | 2        | 5        | 3          | 6         | Проверяемая фактическая информация с открытым доступом к данным предотвращает эскалацию |
| 15 | Технологии: сокрытие уязвимости         | 8           | 1        | 1        | 1          | 9         | Скрытые дефекты приводят к крупным финансовым потерям |

---

## 6. Table time_axes

| id | time_type | description                        |
|----|-----------|------------------------------------|
| 1  | past      | Историческое хронологическое время |
| 2  | present   | Текущее реальное время             |
| 3  | future    | Предсказанное или запланированное время |

---

