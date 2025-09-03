# Technical Specification: "Truth Training" Platform

## 1. General Concept

The application models the development process of civilization and an individual user toward "vector development" based on Boolean logic. Progress is determined by choosing between two fundamental states:

* **Truth** — forward movement.
* **Falsehood** — circular movement (stagnation).

### Core Principles:

* Context (social, financial, etc.) does not change the development direction; it only clarifies the event's domain.
* The cost of choice is not expressed in absolute financial terms but as subjective influence.
* Event chronology is recorded and used for progress analysis over time.

---

## 2. Main Modules

1. **Knowledge Base** — knowledge repository: contexts, types of truth and falsehood, their causes and consequences.
2. **Event Tracking** — registration of events (truth/falsehood), with the option of reevaluation.
3. **Impact Assessment** — evaluation of subjective influence (reputation, emotions, etc.).
4. **Progress Metrics** — calculation of individual and group progress.
5. **Synchronization & Analytics** — data exchange between users and generation of aggregated statistics.

---

## 3. Workflow Logic

1. The user registers an event (outgoing or incoming) associated with truth or falsehood.
2. The event includes parameters: description, context, vector (outgoing/incoming), start time.
3. The **detected** field is defined later during user evaluation. At detection, the end time is recorded. If adjustments occur, the **corrected** field is updated with a new timestamp.
4. Event impact is recorded through entries in the `impact` table.
5. Progress is calculated based on the number of events, their type, and their dynamics over time.
6. Progress visualization:

   * Individual (personal level).
   * Group (environment level).

---

## 4. Key Requirements

* **Boolean Logic**: forward movement (true) or stagnation (false).
* **Time Analysis**: consideration of dynamics and trends.
* **Localization Support**: RU/EN at the first stage (separate knowledge bases for each locale).
* **Data Synchronization**: tables truth\_events, impact, and progress\_metrics are merged for global analytics.
* **Event Uniqueness**: identifiers include a random user ID generated on first launch.

---

## 5. Calculation Methods

* **Individual Progress** = f(number of events, recognition of falsehood, balance of truth/falsehood).
* **Group Progress** = aggregated data of all participants.
* **Trends**: acceleration or deceleration of development.

---

## 6. Minimum Viable Product (MVP)

* Storage of knowledge base and language switching by replacing the database.
* Adding and editing events with parameters.
* Setting detected and corrected statuses with timestamps.
* Event impact assessment.
* Progress calculation.
* Visualization of dynamics (charts).
* Data export for synchronization.

---

## 7. Technology Stack

* **Language**: Rust.
* **Storage**: SQLite (separate databases for locales).
* **Synchronization**: JSON via REST API.
* **UI**: cross-platform (Tauri + Rust).
