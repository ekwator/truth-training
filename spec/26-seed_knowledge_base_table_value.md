# Knowledge Base Table Values for Default Seeding

Use /spec as the primary decision source before reading /docs.
**Version:** v1.1.0  
**Spec ID:** 26  
**Updated:** 2025-12-28  
**Status:** Approved

---

## Purpose

This document defines **reference (seed) values** for the Knowledge Base tables used by the **Truth Training / Gossip** system.

The tables represent **context decomposition** in a strict logical order:

1. **category** — *domain of the event*  
2. **forma** — *form of information expression*  
3. **cause** — *motivational cause*  
4. **develop** — *process / dynamic of unfolding*  
5. **effect** — *impact / outcome*

The document is bilingual (**EN / RU**) because semantic accuracy is required, not literal translation.

---

# ENGLISH LOCALE (en)

## 1. Category (event domain)

| ID | Name            | Description |
|----|-----------------|-------------|
| 1  | Social          | Interpersonal relations, trust, reputation |
| 2  | Financial       | Economic events, assets, losses, obligations |
| 3  | Political       | State actions, treaties, geopolitical relations |
| 4  | Legal           | Law, compliance, judicial consequences |
| 5  | Personal        | Inner decisions, self-assessment |
| 6  | Organizational  | Companies, teams, internal processes |
| 7  | Media           | Information distribution, platforms |
| 8  | Technological   | IT systems, data, security |

---

## 2. Forma (form of expression)

| ID | Name            | Quality | Description |
|----|-----------------|---------|-------------|
| 1  | Deception       | 0 | Intentional distortion of facts |
| 2  | Truth           | 1 | Factual, verifiable information |
| 3  | Self-Deception  | 0 | Distortion to preserve internal comfort |
| 4  | Half-Truth      | 0 | Partial truth with omitted facts |
| 5  | Silence         | 0 | Withholding significant information |
| 6  | Openness        | 1 | Proactive full disclosure |

---

## 3. Cause (motivation)

| ID | Name        | Quality | Description |
|----|-------------|---------|-------------|
| 1  | Fear        | 0 | Avoidance of punishment or loss |
| 2  | Benefit     | 0 | Personal or material gain |
| 3  | Mercy       | 1 | Compassion toward others |
| 4  | Ignorance   | 0 | Lack of knowledge |
| 5  | Duty        | 1 | Moral or contractual obligation |
| 6  | Curiosity   | 1 | Desire to understand reality |
| 7  | Pressure    | 0 | External coercion |
| 8  | Care        | 1 | Protection of another’s well-being |

---

## 4. Develop (process)

| ID | Name           | Quality | Description |
|----|----------------|---------|-------------|
| 1  | Concealment    | 0 | Hiding relevant facts |
| 2  | Manipulation   | 0 | Context distortion, influence |
| 3  | Transparency   | 1 | Open availability of data |
| 4  | Verification   | 1 | Cross-checking sources |
| 5  | Exaggeration   | 0 | Artificial amplification |
| 6  | Confession     | 1 | Admission and correction |

---

## 5. Effect (impact / outcome)

| ID | Name                  | Quality | Description |
|----|-----------------------|---------|-------------|
| 1  | Distrust              | 0 | Loss of confidence |
| 2  | Trust                 | 1 | Strengthened cooperation |
| 3  | Conflict              | 0 | Escalation of tension |
| 4  | Reconciliation        | 1 | Reduction of conflict |
| 5  | Sanctions             | 0 | Legal or reputational penalties |
| 6  | Learning              | 1 | Knowledge growth |
| 7  | Reputation Loss       | 0 | Decrease in status |
| 8  | Reputation Gain       | 1 | Increase in status |
| 9  | Financial Loss        | 0 | Direct economic damage |
| 10 | Financial Stability   | 1 | Preservation or recovery of assets |

---

## 6. Time Axes (time_axes table)

| ID | Time Type | Description                        |
|----|-----------|------------------------------------|
| 1  | past      | Historical chronological time      |
| 2  | present   | Current real-time                  |
| 3  | future    | Predicted or scheduled time        |

---

# RUSSIAN LOCALE (ru)

## 1. Category (область события)

| ID | Name              | Description |
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

## 2. Forma (форма подачи информации)

| ID | Name            | Quality | Description |
|----|-----------------|---------|-------------|
| 1  | Обман           | 0 | Сознательное искажение фактов |
| 2  | Правда          | 1 | Проверяемая фактическая информация |
| 3  | Самообман       | 0 | Искажение для психологического комфорта |
| 4  | Полуправда      | 0 | Частичное сокрытие |
| 5  | Умолчание       | 0 | Сокрытие значимой информации |
| 6  | Открытость      | 1 | Полное добровольное раскрытие |

---

## 3. Cause (причина)

| ID | Name         | Quality | Description |
|----|--------------|---------|-------------|
| 1  | Страх        | 0 | Избежание потерь или наказания |
| 2  | Выгода       | 0 | Личный или материальный интерес |
| 3  | Милосердие   | 1 | Сострадание |
| 4  | Неведение    | 0 | Недостаток знаний |
| 5  | Долг         | 1 | Моральная или формальная обязанность |
| 6  | Любопытство  | 1 | Стремление к пониманию |
| 7  | Давление     | 0 | Внешнее принуждение |
| 8  | Забота       | 1 | Защита блага другого |

---

## 4. Develop (развитие события)

| ID | Name           | Quality | Description |
|----|----------------|---------|-------------|
| 1  | Сокрытие       | 0 | Утаивание фактов |
| 2  | Манипуляция    | 0 | Искажение контекста |
| 3  | Прозрачность   | 1 | Открытый доступ к информации |
| 4  | Проверка       | 1 | Сопоставление источников |
| 5  | Преувеличение  | 0 | Искусственное усиление |
| 6  | Признание      | 1 | Принятие ответственности |

---

## 5. Effect (последствие / воздействие)

| ID | Name                   | Quality | Description |
|----|------------------------|---------|-------------|
| 1  | Недоверие              | 0 | Потеря доверия |
| 2  | Доверие                | 1 | Укрепление связей |
| 3  | Конфликт               | 0 | Эскалация напряжения |
| 4  | Примирение             | 1 | Снижение конфликта |
| 5  | Санкции                | 0 | Юридические или репутационные меры |
| 6  | Обучение               | 1 | Рост понимания |
| 7  | Потеря репутации       | 0 | Снижение статуса |
| 8  | Рост репутации         | 1 | Повышение статуса |
| 9  | Финансовая потеря      | 0 | Экономический ущерб |
| 10 | Финансовая устойчивость| 1 | Сохранение или восстановление ресурсов |

---

## 6. Time Axes (time_axes table)

| ID | Time Type | Description                        |
|----|-----------|------------------------------------|
| 1  | past      | Историческое хронологическое время |
| 2  | present   | Текущее реальное время             |
| 3  | future    | Предсказанное или запланированное время |

---

