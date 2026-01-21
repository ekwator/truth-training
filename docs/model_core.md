# Formalized - Model Core and Database Schema
## Truth Training

**Document Version:** v1.1.0  
**Status:** Specification  
**Updated:** 2025-12-28  
**Status:** Approved

**Purpose:** :  
• Formal **description** of data structure used
in **Truth Training application** and its compliance with **model** of **event**, **consequence** and **truth assessment**  
• This document **describes** the **formal model** of **Truth Training application** with **emphasis** on **relational database structure**  

The **model** is **designed** for :  
• **formalization** of **application entities**  
• **description** of **relationships** between **event**, **consequences** and **judgment**  
• ensuring **reproducibility** of **truth calculation**  
• alignment of **Core** / **Desktop** / **Mobile** implementations  

## 1 Truth Training system is based on the following principles :  
• Truth is *computed*, not stored  
• Errors are allowed locally; stability arises globally  
• No trusted authority; robustness is statistical  
• Two orthogonal axes :  
 ◦ **Consequences axis** → *impact*  
 ◦ **Truth axis** → *judgment*

Each entity is mapped to one or more relational database tables  
Model reflects **one-to-many** principle :  
• one **event** → **multiple interpretations**  
• one **source** → **multiple consequences**  
• one **observatio**n → **multiple judgment**

Each node maintains a local database, evaluates event independently, and participates in **P2P circulation**

**By analogy** :  
• **neural network** = **vector** + **relational structure**  
• **system nodes** = **assessors**  
• **connections** = j**udgment** and **consequences**  

Application **model** includes the following **main entity classes** :  
• **Event**  
• **impact**  
• **judgment**  
• **Participant**  
• **Consensus** / **Aggregation**

Document is coordinated and should be used jointly with :  
• **Canonical SQL schema specifications for implementers [spec/04-data-model.md](../spec/04-data-model.md)**  
• **Canonical markdown schema specifications for implementers [doc/Data_Schema.md](Data_Schema.md)**  
• **Security and verification requirements [SECURITY.md](../SECURITY.md)**  
• **Quality and testing requirements [CONTRIBUTING.md](../CONTRIBUTING.md)**  
• **Minimum requirements for PR acceptance [spec/14-quality-gates.md](../spec/14-quality-gates.md)**

## 2 Basic Model Entities and Service Tables

This chapter **describes** fundamental **database tables** that **provide** object **identification**, their **lifecycle** and **integrity** of **Truth Training model  
Database** structure complies with **following principles** :  
• **Relational model** with explicit **primary** and **foreign** keys  
• **Absence** of **stored** calculated **truth values**  
• All aggregates are **calculated** at **Core** logic level  
• Data **historization**: records are **not overwritten** or **deleted**, but **supplemented**  
• **Support** for **multiple** sources and evaluation **contexts**  

### ⭐️**quantum uncertainty**  
❗This is an **advantage**, not a disadvantage  
⚠️ "small_constants" is **global** **small** **random** in **system** **time** "CURRENT_TIMESTAMP" function value(0, 2)

**Understanding CURRENT_TIMESTAMP in Truth Training System:**

The `CURRENT_TIMESTAMP` in Truth Training system refers to SQLite's built-in function that returns the current date and time in the format 'YYYY-MM-DD HH:MM:SS'. This is used throughout the system as a fundamental time reference for :

• Recording when events, impacts, and judgments are created or modified  
• Calculating temporal relationships between different assessments  
• Implementing time-based decay functions for trust weights and influence  
• Establishing chronological ordering of system activities  
• Supporting the temporal dynamics of truth evolution  

In SQLite, `CURRENT_TIMESTAMP` is equivalent to `datetime('now')` and represents the current time in UTC. The system uses this timestamp for :

• Creating time-stamped records in all major tables (truth_event, impact, judgment, etc.)  
• Calculating time intervals needed for decay functions  
• Determining staleness of node records using TTL mechanisms  
• Synchronizing temporal aspects of event assessment  

**Temporal Dynamics:**

The system implements time-based decay using expressions like :
```
w(t) = w₀ * e^(-λt)
```
where t is calculated as `(CAST(CURRENT_TIMESTAMP AS REAL) - CAST(previous_time AS REAL))` to determine how much time has elapsed since a previous event.

```sql
-- SQL implementation of small random constant in system time for CURRENT_TIMESTAMP value (0, 2) - excluding 0 and 2
-- This implementation uses SQLite built-in functions to generate a random value between 0 and 2, excluding both endpoints
-- Using the current time as a basic seed for randomness
-- The expression combines Unix epoch time with fractional seconds to create a pseudo-random value
-- Add a small epsilon to avoid 0, and ensure it's always less than 2

-- For use in triggers and queries, we define the following expression:
-- ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 )

-- To avoid 0 and ensure less than 2, we use CASE logic:
CASE
    WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
    THEN 0.000001  -- smallest positive value to exclude 0
    ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )  -- ensure it's always less than 2
END

-- For convenience in complex queries, this can be defined as a view:
CREATE VIEW IF NOT EXISTS small_constants_view AS
SELECT
    CASE
        WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
        THEN 0.000001
        ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
    END AS small_constant;

-- Or for direct use in expressions without creating a view:
-- Replace calls to small_constants() with the CASE expression above
```

This SQL implementation replaces the Rust function to maintain the same quantum uncertainty behavior while enabling use in SQL triggers and stored procedures across desktop and mobile platforms. The implementation uses SQLite's built-in time functions to generate a time-based pseudo-random value in the range (0, 2), maintaining the quantum uncertainty property essential to the model.

**Schema Version Tracking**: Both databases "truth_training.sqlite" and "discovery_nodes.sqlite" for desktop application include "schema_version" tables for migration tracking (described in [spec/04-data-model.md](../spec/04-data-model.md)). These tables are not part of the functional model and are not described in detail here. Android uses a single Room database called truth_database, located at /data/data/com.truth.training.client/databases/truth_database , so it contains one table, "schema_version"

##### ⚠️ Attention :  
The truth_event table may be subject to migrations that add additional fields beyond the base schema. As can be seen from the run_migrations function in core/src/storage.rs, during database migration these fields may be added. For detailed information about the migration process and schema version tracking, please refer to the [Migration Documentation](migration_documentation.md) which provides comprehensive details about the database evolution process, including :

- Schema version tracking mechanisms  
- Migration procedures for adding new fields  
- Table renaming procedures (such as "nodes" → "discovery_nodes" in v1.1.0)  
- Compatibility considerations between versions  
- Rollback strategies

The migration process ensures that the database schema evolves properly while maintaining backward compatibility. The fields that may be added through migrations include "global_id", "signature", "participant_id", and "collective_score", among others. These fields are the result of migrations and not part of the initial schema, but may be present in working databases after migration execution.

This explains why in some code parts (e.g. "get_truth_event" function) these fields are already partially accounted for.

### triggers

This section describes the trigger system for data recalculation and aggregation that occurs when new information is added to the system. The triggers are activated when data is added through various pathways :

•  By **participants** through the core system (core/) by entering data using desktop, mobile (android, iOS) applications when creating :  
 ◦ **Events** (**event**)  
 ◦ **Impacts** (**impact**)  
 ◦ **Judgments** (**judgment**)

•  By the system server (src/) during node synchronization :  
 ◦ **P2P Network Layer**  
 ◦ **Node Discovery**

•  By client applications (desktop, mobile android, iOS) during node synchronization :  
 ◦ **P2P Network Layer**  
 ◦ **Node Discovery**

These triggers ensure that all data aggregations and recalculations happen automatically in response to data changes, maintaining data consistency and system efficiency across all nodes in the network.

Conceptually, the trigger system operates on the principle that any new information entering the system will automatically trigger the appropriate recalculation and aggregation processes. This ensures that the system's **collective intelligence** functions operate continuously and consistently, without requiring manual intervention to update **scores** and **metrics**.

The trigger system represents a shift from the previous implementation where recalculation functions were called manually in Rust code. In the new v1.1.0 model, SQL triggers provide a more efficient and reliable mechanism for maintaining data consistency and ensuring that all nodes have up-to-date assessment scores.

This SQL-trigger-based approach offers several advantages :  
• Automatic execution without requiring explicit function calls in application code  
• Atomic execution within the same transaction as the data modification  
• Consistent behavior across all nodes in the distributed system  
• Reduced complexity in the application logic layer  
• Improved performance through database-level optimizations

The triggers are designed to work seamlessly with the existing data model and maintain the same computational logic as the previous Rust-based implementation, but with improved reliability and consistency.

The system implements two distinct categories of triggers based on the source of the data :

#### **1. Participant-initiated triggers**

Activate when a **participant** creates a new **event** through the user interface. These triggers handle the initial creation of the **event** and all associated calculations :

**Triggers from [docs/model_core_collective_assessment.md](model_core_collective_assessment.md):**

• `create_event_record` - creates the initial **event** record in the "truth_event" table when a **participant** submits a new **event**. This trigger also creates the corresponding entry in the "event_ci" table (**event neuron**) with default values.

• `initialize_event_metrics` - initializes all **metric fields** ("collective_score", "impact_score", "judgment_score") to default values when a new **event **is inserted. Sets "collective_score" to 0.5 (neutral), "impact_score" to 0.0, and "judgment_score" to NULL.

•  `update_participant_reputation_on_impact` - updates **participant's reputation** based on the accuracy of their **impact assessments**, comparing against the "collective_score" as a reference value. Increases "accurate_impact" counter when the **impact aligns** with the **collective assessment**.

•  `update_participant_reputation_on_judgment` - updates **participant's reputation** based on the accuracy of their **judgment assessments**, comparing against the "collective_score" as a reference value. Increases "accurate_judgment" counter when the **judgmen**t aligns with the **collective assessment**.

•  `create_impact_prediction_on_impact_creation` - creates a new record in the "impact_predictions" table when a **participant** creates an **impact** record that relates to a future predicted factual **consequence**. This trigger calculates **prediction** values based on the **event's impact** data and **collective score** when a new **impact** is recorded.

•  `create_impact_predictions_on_status_change` - creates new records in the "impact_predictions" table when an **event's** status changes in "event_ci.status" (e.g. from "active"/"resolved" to "archived"). This trigger preserves historical **prediction** data and adjusts **prediction probabilities** based on the actual outcomes compared to expected values when the **event** reaches a resolution state.

•  `update_participant_reputation_on_prediction_accuracy` - updates **participant reputation** based on the accuracy of **impact predictions**. This trigger aggregates prediction accuracy across all **events** where the **participant** created **impact** records, comparing "impact_predictions.expected_strength" against actual "truth_event.collective_score". The calculation considers the "horizon" value: **predictions** made earlier (with larger "horizon" values) receive greater weight in **reputation** calculations. **Reputation** is updated when **events** transition to "resolved" or "archived" status.

•  `aggregate_local_scores_for_global` - populates the statements table with local assessments for global calculation when "truth_event" is updated. This trigger fires when the "collective_score" changes and updates the statements table for cross-node aggregation.

**Triggers from [docs/model_core_scoring.md](model_core_scoring.md):**

•  `update_impact_score_after_impact_change` - activates when a new entry is added to the "impact" table and automatically recalculates the "impact_score" for the associated "truth_event".

•  `update_judgment_score_after_judgment_change` - activates when a new entry is added to the "judgment" table and automatically recalculates the "judgment_score" for the associated **event**.

**Triggers from [docs/model_core_aggregated_metrics.md](model_core_aggregated_metrics.md):**

•  `update_progress_metrics_after_event` - updates progress **metrics** when a new **event** is processed, recalculating total counts and trends.

#### **2. Synchronization-initiated triggers**

Activate when **events** are received from other **nodes** during synchronization. These triggers may have different algorithms optimized for bulk processing and validation :

**Triggers from [docs/model_core_collective_assessment.md](model_core_collective_assessment.md):**

• `validate_incoming_event` - validates the structure and cryptographic signatures of **events** received from other **nodes** before processing. Verifies that required fields are present, "global_id" is properly formatted, **signatures** exist, and **context** fields reference valid entries in their respective tables.

• `process_sync_event_record` - handles the creation of **event** records from other **nodes** during synchronization, potentially with different validation rules. Checks for duplicate **events**, creates corresponding entries in the "event_ci" table, and updates **participant** activity timestamps.

**Triggers from [docs/model_core_aggregated_metrics.md](model_core_aggregated_metrics.md):**

•  `update_heuristic_weight` - updates the weight of **expert heuristics** based on their proven accuracy when the accuracy changes.

•  `update_progress_metrics_after_impact` - updates **progress metrics** when a new **impact** is recorded, recalculating **impact totals** and **trends**.

**Triggers from [docs/model_core_network_tables.md](model_core_network_tables.md):**

•  `update_trust_score_after_rating_change` - automatically recalculates **trust score** and propagation priority based on new **event** counts when node ratings are updated.

•  `cleanup_expired_tokens` - removes **tokens** that have exceeded their expiration time.

•  `update_peer_synchronization_after_sync` - automatically updates peer history with new synchronization information when sync logs are created.

•  `update_node_performance_after_sync` - updates **performance metrics** when synchronization **events** are logged.

### 2.1 Participants

#### Table: participants

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Stores information about collective intelligence system participants  
**Fields** :
```
public_key         (TEXT, PK, UNIQUE, NOT NULL) — unique participant's public key - identifier
signature          (TEXT, NOT NULL) — cryptographic signature
reputation_score   (REAL, NOT NULL, DEFAULT 0.5) — participant's reputation score
reputation_history (INTEGER, NOT NULL) — FK → reputation_history.id — participant's reputation history
total_judgment     (INTEGER, NOT NULL, DEFAULT 0) — total number of judgment made
accurate_judgment  (INTEGER, NOT NULL, DEFAULT 0) — number of accurate judgment
total_impact       (INTEGER, NOT NULL, DEFAULT 0) — total number of impact made
accurate_impact    (INTEGER, NOT NULL, DEFAULT 0) — number of accurate impact
created_at         (INTEGER, NOT NULL) — registration time
last_activity      (INTEGER) — timestamp of last activity
```
🏠 Database: truth_training.sqlite

**Notes** :  
• Participant is not tied to identity  
• Authentication is based on cryptographic key  
• Reputation is calculated based on accuracy of judgment  

**Model "participants"** :  
**Source relation**
```sql
participants.public_key = judgment.participant_id
participants.public_key = impact.participant_id
participants.reputation_history = reputation_history.id
```
**Base participant mapping**
```sql
base_participant_id = participants.public_key
-- This represents the participant whose reputation is being calculated
-- Used as reference in reputation calculation formulas below
```
**Aggregation formulas "reputation_score"**
```sql
-- Reputation score is calculated via triggers when impact or judgment records are added
-- The actual calculation happens in the following triggers:
-- 1. update_participant_reputation_on_impact (for impact assessments)
-- 2. update_participant_reputation_on_judgment (for judgment assessments)

participants.reputation_score = CASE
    WHEN (total_impact + total_judgment) > 0 THEN
        (accurate_impact + accurate_judgment) * 1.0 / (total_impact + total_judgment)
    ELSE 0.5  -- Default neutral score when no assessments have been made
END

-- This score is updated automatically via SQL triggers when:
-- - A new impact record is added (checked against collective_score for accuracy)
-- - A new judgment record is added (checked against collective_score for accuracy)
-- The trigger recalculates the reputation based on the combined accuracy of both impact and judgment assessments
```
**Aggregation formulas "total_judgment"**
```sql
participants.total_judgment = (
    -- Incremented via trigger update_participant_reputation_on_judgment when new judgment records are added
    -- Total count of judgment assessments made by the participant
    -- Updated automatically when participant creates new judgment records
)
```
**Aggregation formulas "accurate_judgment"**
```sql
participants.accurate_judgment = (
    -- Incremented via trigger update_participant_reputation_on_judgment when judgment assessments are accurate
    -- Accuracy is determined by comparing judgment assessment with the collective_score from truth_event
    -- Updated automatically when participant's judgment aligns with collective assessment
)
```
**Aggregation formulas "total_impact"**
```sql
participants.total_impact = (
    -- Incremented via trigger update_participant_reputation_on_impact when new impact records are added
    -- Total count of impact assessments made by the participant
    -- Updated automatically when participant creates new impact records
)
```
**Aggregation formulas "accurate_impact"**
```sql
participants.accurate_impact = (
    -- Incremented via trigger update_participant_reputation_on_impact when impact assessments are accurate
    -- Accuracy is determined by comparing impact value with the collective_score from truth_event
    -- Updated automatically when participant's impact aligns with collective assessment
)
```
**Aggregation formulas "created_at"**
```sql
participants.created_at = CURRENT_TIMESTAMP
```

#### Model: Participant Reputation Tracking

**Participant Reputation Model** :
```
R(u) = A(u) / T(u)
```
Where:
- R(u) — "reputation_score" of **participant** (u)
- A(u) — **number** of **accurate assessments** by **participant** (u)
- T(u) — **total number** of **assessments** by **participant** (u)

**Reputation Update Rule** :
```
R_new = (R_old * N_old + accuracy_contribution) / (N_old + 1)
```

**Source tables** :  
• "participants"  
• "judgment"  
• "impact"

**Accuracy calculation** :
```sql
IF judgment.accuracy_confirmed = TRUE
    accurate_judgment = accurate_judgment + 1
    total_judgment = total_judgment + 1
ELSE
    total_judgment = total_judgment + 1

IF impact.accuracy_confirmed = TRUE
    accurate_impact = accurate_impact + 1
    total_impact = total_impact + 1
ELSE
    total_impact = total_impact + 1
```

**Rules** :  
• "reputation_score" ∈ [0, 1]  
• **Default** "reputation_score" is 0.5 (neutral)  
• **Participants** with **higher reputation** have **more weight** in **consensus calculations**  
• "reputation_score" is **updated asynchronously** after accuracy confirmation

**Participant Interaction Model** :  
• Each **participant** creates **events** in "truth_event" table  
• Each **participant** makes **judgments** in "judgment" table  
• Each **participant** assesses **impacts** in "impact" table  
• **Participant reputation** affects "judgment_weights" in **collective assessments**

**Trust Propagation** :  
• **High-reputation participant** have **more** influence on **consensus**  
• **Low-reputation participant** contributions are **weighted less**  
• **Reputation** affects propagation **priority in network**

**Participant Lifecycle** :  
• **Registration** → "reputation_score" = 0.5  
• **First assessment** → "reputation_score" updated **based on accuracy**  
• **Continuous assessment** → "reputation_score" **evolves over time**  

**Key constraints** :  
• "reputation_score" must be between 0 and 1  
• "public_key" must be **unique** across **all participants**  
• "reputation_history" must **reference** valid records in "reputation_history" table

**Notes** :  
• **Participant** identity is **pseudonymous** "public_key" based  
• **Reputation** is calculated based on **historical accuracy**  
• **Reputation** affects the **participant** influence in the **system**  
• Cryptographic **signatures** ensure **authenticity** of **participant** actions  

#### Table: reputation_history

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
**Tracking changes** in collective intelligence system **participant reputations** for **auditing and analyzing** changes in **participant reputations**, understanding reasons for **reputation changes**, analyzing **participant behavior** and **judgment impactiveness**, and **ensuring transparency** of **reputation system**  
**Fields** :
```
id             (INTEGER, PK, AUTOINCREMENT) — unique history record identifier
old_reputation (REAL, NOT NULL) — previous reputation score
new_reputation (REAL, NOT NULL) — new reputation score
change_reason (TEXT, NOT NULL) — reason for reputation change
updated_at     (INTEGER, NOT NULL) — timestamp of update
```
🏠 Database: truth_training.sqlite

**Model "reputation_history"** :  
**Source relation**
```sql
reputation_history.id = participants.reputation_history
```
**Base participant mapping**
```sql
base_participant_id =
SELECT participants.public_key
FROM participants
WHERE participants.reputation_history = reputation_history.id
```
**Aggregation formulas "old_reputation"**
```sql
reputation_history.old_reputation = (
    SELECT reputation_score
    FROM participants
    WHERE participants.public_key = base_participant_id
)
```
**Aggregation formulas "new_reputation"**
```sql
reputation_history.new_reputation = (
    SELECT CASE
        WHEN (total_impact + total_judgment) > 0 THEN
            (accurate_impact + accurate_judgment) * 1.0 / (total_impact + total_judgment)
        ELSE 0.5
    END
    FROM participants
    WHERE participants.public_key = base_participant_id
)
```
**Aggregation formulas "change_reason"**
```sql
reputation_history.change_reason = (
    SELECT CASE
        WHEN (SELECT reputation_score FROM participants WHERE public_key = base_participant_id) >
             (SELECT old_reputation FROM reputation_history WHERE id = reputation_history.id)
        THEN 'accuracy_confirmation'
        WHEN (SELECT total_judgment FROM participants WHERE public_key = base_participant_id) % 10 = 0
        THEN 'assessment_completed'
        WHEN (SELECT reputation_score FROM participants WHERE public_key = base_participant_id) >
             (SELECT AVG(reputation_score) FROM participants) * 1.2
        THEN 'consensus_alignment'
        ELSE 'reputation_update'
    END
)
```
**Aggregation formulas "updated_at"**
```sql
reputation_history.updated_at = CURRENT_TIMESTAMP
```

#### Model: Reputation History Tracking

**Reputation Change Model** :
```
ΔR = R_new - R_old
```

**Change Reasons** :  
• "accuracy_confirmation" — reputation updated based on confirmed accuracy  
• "assessment_completed" — reputation updated after completing assessment  
• "consensus_alignment" — reputation updated based on alignment with consensus  
• "penalty_application" — reputation reduced due to detected manipulation

**Reputation History Rules** :
```sql
IF judgment.confirmed_accuracy = TRUE
    change_reason = "accuracy_confirmation"
    old_reputation = participant.reputation_score
    new_reputation = updated_reputation_score

IF impact.confirmed_accuracy = TRUE
    change_reason = "accuracy_confirmation"
    old_reputation = participant.reputation_score
    new_reputation = updated_reputation_score
```

**History Analysis** :  
• Trend analysis for participant behavior  
• Detection of reputation manipulation attempts  
• Verification of reputation evolution consistency  
• Audit trail for reputation changes

**Key constraints** :  
• old_reputation and new_reputation must be between 0 and 1  
• change_reason must be one of predefined values  
• updated_at must be current timestamp

**Notes** :  
• Used for auditing and transparency of reputation changes  
• Tracks historical changes for analysis  
• Enables detection of reputation manipulation  
• Supports reputation trend analysis  

### 2.2 Axes

• **Truth** in **Truth Training** system is **not static** value  
• It exists as **function of time**, **context** and **accumulated experience**

#### 2.2.1 Time Axis

•  Each **event**, **judgment** and each **consequences** has temporal extent, and their significance changes as new data arrives  
→ Details in the section **3 Temporal Dynamics and Truth Evolution**

#### 2.2.2 Event Axis

**Purpose** :  
Defines the **event axis** that represents the **chain** of **related events** over **time**, allowing for **tracking** of **event evolution** and **relationships**

**Notes** :  
• **Event axis** enables **tracking** of **event sequences** and **dependencies**  
• **Events** can be **linked** to form chains of **related occurrences**  
• **Supports analysis** of **event causality** and **temporal relationships**  

##### Model: Event Axis Relationships
```
E = {e_1, e_2, ..., e_n}
```
Where **each** eᵢ is an **event** with temporal and causal **relationships** to **other events**

#### 2.2.3 Impact Axis (Consequences Axis)

• **Impact axis** describes **measurable**, **observable** and **predictable impact** of **event over time**  
• **Unlike truth axis**, **impact axis** does not operate with opinions or interpretations — it **records changes** in **system** and **environment states**

##### Model: Impact Axis
```
I(E, t) = Σ [mᵢ(t)]
```
**Where** :  
- I(E, t) — **impact** of **event** E at **time** t  
- mᵢ(t) — **magnitude** of **impact** type i at **time** t

**Impact Types** :  
• **Quantifiable consequences**  
• **Observable changes**  
• **Measurable outcomes**  
• **Predictable effects**  
• **Unpredictable consequences**

**Notes** :  
• **Impact axis** records **objective changes** rather than **subjective opinions**  
• **Supports verification** through **observable evidence**  
• Enables **prediction** of future **consequences**  
• Enables **identify** hidden **manipulation** of **consequences**  
• **Hidden manipulation** occurs when the **trend** of the **context elements** ("N","P") does **not align** with the **final effect** ("R"), suggesting **external interference** or **secondary event**  
• Such **discrepancies** are **flagged** for further investigation as they may represent attempts to **influence** the system **without proper alignment** between **cause**, **development** and **effect**  
• The **system uses** these **inconsistencies** to **identify potential manipulation** attempts and adjust **trust metrics accordingly**  
**Impact Axis Trend** :  
• The **model** and **rules** for trend determining **impact** ("impact.trend") directly **depend** on the **context**, for more details see "Model "quality"" the section → "2.4 Context as Semantic Space reference knowledge-base"

**Impact Axis Rules** :
```sql
N = (
    SELECT COUNT(*)
    FROM forma
    WHERE forma.id = (
        SELECT truth_event.forma_id
        FROM truth_event
        WHERE truth_event.id = impact.event_id
    ) AND forma.quality = 0
) + (
    SELECT COUNT(*)
    FROM cause
    WHERE cause.id = (
        SELECT truth_event.cause_id
        FROM truth_event
        WHERE truth_event.id = impact.event_id
    ) AND cause.quality = 0
) + (
    SELECT COUNT(*)
    FROM develop
    WHERE develop.id = (
        SELECT truth_event.develop_id
        FROM truth_event
        WHERE truth_event.id = impact.event_id
    ) AND develop.quality = 0
)

P = (
    SELECT COUNT(*)
    FROM forma
    WHERE forma.id = (
        SELECT truth_event.forma_id
        FROM truth_event
        WHERE truth_event.id = impact.event_id
    ) AND forma.quality = 1
) + (
    SELECT COUNT(*)
    FROM cause
    WHERE cause.id = (
        SELECT truth_event.cause_id
        FROM truth_event
        WHERE truth_event.id = impact.event_id
    ) AND cause.quality = 1
) + (
    SELECT COUNT(*)
    FROM develop
    WHERE develop.id = (
        SELECT truth_event.develop_id
        FROM truth_event
        WHERE truth_event.id = impact.event_id
    ) AND develop.quality = 1
)

R = (
    SELECT effect.quality
    FROM effect
    WHERE effect.id = impact.type_id
)
```
**Rules** :  
🔒 Canonical definition of "impact.trend"  
- "impact.trend" = 0 → logical_negative  
- "impact.trend" = 1 → logical_positive  
- "impact.trend" = 2 → illogical_negative  
- "impact.trend" = 3 → illogical_positive

N — count of negative context qualities  
P — count of positive context qualities  
R — actual effect of the event (effect.quality)  
"impact.trend" — expected direction of consequences (See section "Model "quality"" )  
"impact.value" ∈ {NULL, 0, 1}

🔸 Logical Negative outcomes (Expectation matches context. Context is negative, effect is negative)
```sql
IF N > P AND R = 0 AND impact.trend = 0
    impact.value = 0
```
✔ correct prediction of negative consequences

🔸 Logical Positive outcomes (expectation matches context. Context is positive, effect is positive)
```sql
IF N < P AND R = 1 AND impact.trend = 1
    impact.value = 1
```
✔ correct prediction of positive consequences

🔸 Illogical Negative outcomes (Discrepancy between expectations and results. Context is positive, effect is negative)
```sql
IF N < P AND R = 0 AND impact.trend = 2
    impact.value = NULL
```
⚠ degradation under favorable conditions  
→ potential manipulation or sabotage

🔸 Illogical Positive outcomes (Discrepancy between expectations and results. Context is negative, effect is positive)
```sql
IF N > P AND R = 1 AND impact.trend = 3
    impact.value = NULL
```
⚠ result contradicts context  
→ possible external compensation or hidden intervention

🔁 Correction rules (checking false trend)

These rules do not create new knowledge, but detect inconsistencies
between the stated impact.trend and reality.

False logical_positive (expected plus, got minus)
```sql
IF N > P AND R = 0 AND impact.trend = 1
    impact.value = NULL
```
False logical_negative (expected minus, got plus)
```sql
IF N < P AND R = 1 AND impact.trend = 0
    impact.value = NULL
```
Illogical trend Negative, corrected by reality to positive
```sql
IF N < P AND R = 1 AND impact.trend = 2
    impact.value = 1
```
Illogical trend Positive, corrected by reality to negative
```sql
IF N > P AND R = 0 AND impact.trend = 3
    impact.value = 0
```

🧠 Semantic summary Impact Axis (used for calculating 
participants.reputation_score as )

- "impact.trend" — expectation / direction of participant thinking  
- "impact.value" — correspondence to reality  
-"NULL" — prediction error or suspicion of manipulation

• **impact** ≠ **judgment** but connected by **neuron function** table "event_ci"  
• **impact** trains the ability to **predict consequences**, **not** to **be right**

🔗 Connection with "participants.reputation_score"
```
IF impact.value (0/1)

	participants.accurate_impact += 1

	impact_metrics.total_magnitude += 1

IF impact.value (NULL)

	accurate_impact -= 1
	
	impact_metrics.total_magnitude -= 1
```
→ prediction error  
→ decrease in trust in participant's ability to anticipate consequences

Correction rules :  
→ show participant's retrainability  
→ more important than single error

📌 Thus :  
Reputation grows not from "good effects", but from prediction accuracy in context.

**Design decision** :  
• The **Impact Axis** deliberately does **NOT** use fixed **weights**  
• Any **attempt to assign static importance coefficients** would **introduce subjective value** systems into the **consequence** layer and break **axis orthogonality**

**Significance of impact emerges only through** :  
• **temporal** accumulation  
• **collective** observation  
• **logical consistency** with **context**


#### 2.2.4 Judgment Axis (Truth Axis)

**Truth axis** describes process of **collective** and **individual event assessment**  
**Unlike consequence axis**, truth is not an **objective value** and is **formed** through system participant judgment**

**Purpose** :  
**judgment** table **structure** in project is **implemented** to store **judgment** of system **participant** about **event**  
Table is used to store **judgment** that **collective intelligence** system **participants** issue about **event**  
Each **judgment** represents **assessment** of specific **event** by specific **participant** and includes type of **assessment**, **confidence level**, **reasoning**

Table is **integrated** into **collective intelligence** system and **used** for **collecting participant** opinions about **event**, which **subsequently** allows **calculating consensus** and updating **participant reputations** based on **accuracy** of their **judgment**

##### Model: Judgment Axis
```
J_{u,i} = ⟨a_{u,i}, c_{u,i}⟩
```
Where:
- a_{u,i} ∈ {-1, +1} — assessment direction (false/true)
- c_{u,i} ∈ [0,1] — confidence level

**Local judgment metric** :
```
cjᵢ^{(u)} = a_{u,i} ⋅ c_{u,i}
```

**Judgment Processing Model** :
```
J(E) = f({J_1, J_2, ..., J_n})
```
**Where** :  
- J(E) — **collective judgment** of **event** E  
- {J_1, J_2, ..., J_n} — **individual judgment** of **event** E  
- f — **aggregation function**

**Judgment Confidence Model** :
```
C_j = Σ(w_k * c_k) / Σ(w_k)
```
**Where** :  
- C_j — collective confidence in **judgment**  
- w_k — **weight** of **participant** k  
- c_k — **confidence** of **participant** k

**Judgment Update Rule** :
```
IF new_judgment.confirmed_by_evidence = TRUE
    confidence = confidence * (1 + reinforcement_factor)
ELSE
    confidence = confidence * (1 - penalty_factor)
```

**Judgment Classification** :  
• **Truth assessment** (true/false)  
• **Confidence level** (0-1)  
• **Reasoning** (textual justification)  
• **Source verification** (participant reputation)

**Notes** :  
• **Judgment axis** reflects **subjective assessment** of **truth**  
• **Supports collective intelligence** through **aggregation**  
• Enables **reputation** based **weighting** of **assessments**

### 2.3 Axis Intersection: Judgment (Truth Axis) × Impact (Consequences Axis)

Each **event** in **Truth Training** system exists **simultaneously** in **two independent spaces** :  
- **Truth space** ("Judgment")  
- **Impact space** ("Impact")

These **axes** are **orthogonal** and cannot be derived from each other.
**Event E is represented as point** :
```
E = ( T(E), I(E) )
```
**Where** :  
- T(E) ∈ [0,1] — **aggregated truth**  
- I(E) ∈ ℝ — **cumulative impact**

**On Truth × Impact plane, 4 basic classes are identified** :

• 1. High truth / High impact  
• 2. High truth / Low impact  
• 3. Low truth / High impact  
• 4. Low truth / Low impact  
| Truth | Impact | Interpretation                 |
| ----- | ------ | ----------------------------- |
| High  | High   | Critical real event           |
| High  | Low    | Fact without significant consequences |
| Low   | High   | Dangerous disinformation      |
| Low   | Low    | Noise / information garbage   |

**Intersection Model** :
```
Intersection(E) = T(E) × I(E)
```

**Quadrant Classification** :
```
Q1: T(E) ≥ θ_T AND I(E) ≥ θ_I → Critical real event
Q2: T(E) ≥ θ_T AND I(E) < θ_I → Fact without significant consequences
Q3: T(E) < θ_T AND I(E) ≥ θ_I → Dangerous disinformation
Q4: T(E) < θ_T AND I(E) < θ_I → Noise / information garbage
```

#### 2.3.1 Table: event_projection

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Stores the **projection** of an **event** in **truth–impact** space for **classification**  
**Fields** :
```
event_id      (INTEGER, NOT NULL) — FK → event_ci.id — event reference
truth_score   (REAL, NOT NULL) — aggregated truth score
impact_score  (REAL, NOT NULL) — aggregated impact score
quadrant      (TEXT) — quadrant classification (Q1/Q2/Q3/Q4)
calculated_at (INTEGER, NOT NULL) — timestamp of calculation
```
🏠 Database: truth_training.sqlite

**Model "event_projection"** :  
**Source relation**
```sql
event_projection.event_id = event_ci.id
event_projection.truth_score = (
    SELECT consensus_ci.confidence_score
    FROM consensus_ci
    WHERE consensus_ci.event_id = event_projection.event_id
)
event_projection.impact_score = (
    SELECT impact_metrics.total_magnitude
    FROM impact_metrics
    WHERE impact_metrics.event_id = event_projection.event_id
)
```

**Note**: The `event_projection` table utilizes pre-calculated aggregated values from the `consensus_ci` and `impact_metrics` tables rather than performing direct computations. This approach ensures consistency and avoids redundant calculations while maintaining the integrity of the projection model.

**Aggregation formulas "quadrant"**
```sql
event_projection.quadrant = (
    SELECT CASE
        WHEN truth_score >= 0.5 AND impact_score >= 0 THEN 'Q1'
        WHEN truth_score >= 0.5 AND impact_score < 0 THEN 'Q2'
        WHEN truth_score < 0.5 AND impact_score >= 0 THEN 'Q3'
        ELSE 'Q4'
    END
)
```
**Aggregation formulas "calculated_at"**
```sql
event_projection.calculated_at = CURRENT_TIMESTAMP
```

##### Model: Event Projection Classification

**Projection Model** :
```
P(E) = (T(E), I(E))
```

**Quadrant Assignment** :
```
quadrant(E) =
 Q1 if T(E) ≥ θ_T and I(E) ≥ θ_I
 Q2 if T(E) ≥ θ_T and I(E) < θ_I
 Q3 if T(E) < θ_T and I(E) ≥ θ_I
 Q4 otherwise
```

**Threshold Values** :  
• θ_T — **truth threshold** (typically 0.5)  
• θ_I — **impact threshold** (typically 0)

**Classification Rules** :
```
IF truth_score >= 0.5 AND impact_score >= 0
    quadrant = 'Q1'  -- Critical real event
ELSE IF truth_score >= 0.5 AND impact_score < 0
    quadrant = 'Q2'  -- Fact without significant consequences
ELSE IF truth_score < 0.5 AND impact_score >= 0
    quadrant = 'Q3'  -- Dangerous disinformation
ELSE
    quadrant = 'Q4'  -- Noise / information garbage
```

**Quadrant Properties** :  
• Q1: High truth, High impact → **Action required**, verified information  
• Q2: High truth, Low impact → **Historical facts**, low priority  
• Q3: Low truth, High impact → **Disinformation**, potential threat  
• Q4: Low truth, Low impact → **Noise**, ignore

**Notes** :  
• Used for **event prioritization** and **response planning**  
• Enables targeted resource allocation  
• Supports automated **event** categorization

#### 2.3.2 Table: statements  
📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

###### Truthfulness as Statistical Function (global)

**Purpose** :  
Aggregates local training metrics (csᵢ) for global processing in group training  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique statement identifier
event_id    (INTEGER, NOT NULL) — FK to truth_event.id — event reference
truth_score (REAL) — aggregated truth score (from local nodes)
created_at  (INTEGER, NOT NULL) — timestamp of creation
updated_at  (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: truth_training.sqlite

**Model "statements"** :  
**Source relation**
```sql
statements.event_id = truth_event.id
statements.truth_score = (
    SELECT AVG(truth_event.collective_score)
    FROM truth_event
    WHERE truth_event.id = statements.event_id
)
```
**Base event mapping**
```sql
base_event_id =
SELECT truth_event.id
FROM truth_event
WHERE truth_event.id = statements.event_id
```
**Aggregation formulas "truth_score"**  
```sql
statements.truth_score = (
    SELECT AVG(collective_score)
    FROM truth_event
    WHERE truth_event.global_id = (
        SELECT truth_event.global_id
        FROM truth_event
        WHERE truth_event.id = base_event_id
    )
)
```
**Aggregation formulas "created_at"**
```sql
statements.created_at = CURRENT_TIMESTAMP
```
**Aggregation formulas "updated_at"**
```sql
statements.updated_at = CURRENT_TIMESTAMP
```

##### Model: Global Truth Aggregation

**Global Truth Model** :
```
T_global(E) = f_local({cs_1, cs_2, ..., cs_n})
```
**Where** :  
- T_global(E) — **global truth score** for **event** E  
- {cs_1, cs_2, ..., cs_n} — **local collective scores** from different **nodes**  
- f_local — aggregation function

**Aggregation Function** :  
```
T_global(E) = Σ(wᵢ * csᵢ) / Σ(wᵢ)
```
**Where** :  
- wᵢ — **weight** of **node** i  
- csᵢ — **collective score** from **node** i

**Statistical Model** :
```
truth_score_global =
    IF number_of_nodes >= minimum_threshold
        THEN aggregated_score
    ELSE NULL (insufficient data)
```

**Update Rules** :
```
IF new_local_score.arrives
    recalculate_global_aggregation()
    update_participant_reputations()
    update_event_classification()
```

**Note on Implementation**: The above code represents pseudocode rather than actual SQL syntax. The actual implementation uses documented triggers and views that achieve similar functionality :

- The `aggregate_local_scores_for_global` trigger populates the "statements" table with local **assessments** for global calculation when "truth_event" is updated. This trigger fires when the "collective_score" changes and updates the statements table for cross-node aggregation.

- The `update_participant_reputation_on_prediction_accuracy` trigger updates **participant reputations** based on the accuracy of **impact predictions** by aggregating **prediction accuracy** across all **events**.

- The `global_truth_score_calculation` view performs the actual global aggregation by calculating "truth_score_global" as the average of local **collective scores** from different **nodes**.

- Event classification updates are handled by the "event_classification_calculation" view and related triggers that update the "event_ci" table's "resolution_data" field based on the convergence of **impact** and **judgment** axes.

The pseudocode represents the conceptual model for how global **truth** aggregation should work, but the actual implementation uses different trigger names and follows a more complex architecture involving the "statements" table for cross-node aggregation.

**Notes** :  
• **Aggregates scores** across distributed **nodes**  
• Maintains **statistical validity** of **truth assessments**  
• Enables **cross-node verification**

#### 2.3.3 Table: group_ratings  
📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Stores **group assessment ratings** for **collective progress metrics**  
**Fields** :
```
group_id     (INTEGER, PK, AUTOINCREMENT) — unique group identifier
members      (INTEGER, NOT NULL) — list of group member IDs
avg_score    (REAL, NOT NULL) — average score of the group
coherence    (REAL, NOT NULL) — coherence of group assessments
last_updated (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: truth_training.sqlite

**Model "group_ratings"** :  
**Source relation**
```sql
group_ratings.group_id = participants.group_membership
group_ratings.avg_score = (
    SELECT AVG(participants.reputation_score)
    FROM participants
    WHERE participants.group_membership = group_ratings.group_id
)
group_ratings.coherence = (
    SELECT 1 - (SUM(ABS(p.reputation_score - avg_score)) / (COUNT(*) * 2))
    FROM participants p
    WHERE p.group_membership = group_ratings.group_id
)
```
**Base group mapping**
```sql
base_group_id =
SELECT group_ratings.group_id
FROM group_ratings
WHERE group_ratings.group_id = participants.group_membership
```
**Aggregation formulas "avg_score"**
```sql
group_ratings.avg_score = (
    SELECT AVG(reputation_score)
    FROM participants
    WHERE group_membership = base_group_id
)
```
**Aggregation formulas "coherence"**
```sql
group_ratings.coherence = (
    SELECT 1 - (SUM(ABS(reputation_score - avg_score)) / (COUNT(*) * 2))
    FROM participants
    WHERE group_membership = base_group_id
)
```
**Aggregation formulas "last_updated"**
```sql
group_ratings.last_updated = CURRENT_TIMESTAMP
```

##### Model: Group Assessment Dynamics

```
R_group = (Σ Rᵢ) / N
```
**Where** :  
- R_group — **group rating**  
- Rᵢ — **rating of member** i  
- N — **number of members**

**Coherence Measurement** :
```
C_group = 1 - (Σ |Rᵢ - R_group|) / (N * max_deviation)
```

**Group Performance Metrics** :
```
Performance = f(avg_score, coherence, consistency)
```

**Coherence Rules** :
```sql
IF coherence > 0.8
    group_decision_reliability = HIGH
ELSE IF coherence > 0.6
    group_decision_reliability = MEDIUM
ELSE
    group_decision_reliability = LOW
```

**Notes** :  
• Measures collective intelligence effectiveness  
• Tracks group consensus quality  
• Enables group performance optimization

#### 2.3.4 Table: progress_metrics  
📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Aggregates system-wide progress metrics (event counts and reaction totals)  
**Fields** :
```
id                           (INTEGER, PK, AUTOINCREMENT) — unique metric record identifier
total_events                 (INTEGER, NOT NULL) — total number of events processed
total_events_group           (INTEGER, NOT NULL) — total number of group events
total_positive_impacts       (REAL, NOT NULL) — total positive impacts observed
total_positive_impacts_group (REAL, NOT NULL) — positive impacts in group events
total_negative_impacts       (REAL, NOT NULL) — total negative impacts observed
total_negative_impact_group  (REAL, NOT NULL) — negative impacts in group events
trend                        (REAL, NOT NULL) — overall trend metric
trend_group                  (REAL, NOT NULL) — trend metric for group events
last_updated                 (INTEGER, NOT NULL) — timestamp of last update metric
```
🏠 Database: truth_training.sqlite  

##### Model: System Progress Tracking  

**Progress Metrics Model** :
```
M_system = (M_individual, M_group, M_trend)
```

**Trend Calculation** :
```
Trend = (Σ P - Σ N) / total_events
```
**Where** :  
- P — positive impacts  
- N — negative impacts

**Impact Balance** :
```
Balance = total_positive_impacts - total_negative_impacts
```

**Group vs Individual Comparison** :
```
IF total_events_group / total_events > threshold
    system_efficiency = HIGH (group collaboration effective)
ELSE
    system_efficiency = LOW (individual assessment dominant)
```

**Progress Update Rules** :
```sql
IF new_event_processed
    total_events = total_events + 1
    IF event.is_group_event
        total_events_group = total_events_group + 1
    update_impact_metrics()
    recalculate_trends()
    last_updated = CURRENT_TIMESTAMP
```

**Trend Interpretation** :  
• Positive trend → system improving, truth convergence  
• Negative trend → system degrading, truth divergence  
• Near-zero trend → system stable, truth stable

**Notes** :  
• Tracks overall system health and performance  
• Enables early detection of system degradation  
• Supports system optimization decisions

### 2.4 Context as Semantic Space reference knowledge-base

📝 **System-level** table of the Collective Intelligence Layer  
It is **not accept direct participant input**, and is **not transmitted over the network**  
• These tables **are populated** with reference information by the **initialization function** in the application **core module** core/src/storage.rs  
• **Context** is defined by 5 tables  
• "quality" ∈ {0,1} — **semantic valence** (positive/negative). This is **not a truth metric**, used for analytics/filtering and trends. **category not include** "quality"  
• Default values for these tables are specified in: [Knowledge Base Table Values for Default Seeding](../spec/26-seed_knowledge_base_table_value.md)

#### 2.4.1 Table: category

**Purpose** :  
**Storage record** reference **categories** for **event** in the **context vocabulary**. This **main table** in **context**  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique category identifier
name        (TEXT, NOT NULL) — category name
description (TEXT, NOT NULL) — category description
```
🏠 Database: truth_training.sqlite  

#### 2.4.2 Table: forma

**Purpose** :  
**Storage record** reference **forms** is trend **development** in **context**  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique forma identifier
name        (TEXT, NOT NULL) — forma name
quality     (INTEGER, NOT NULL) — semantic valence (0 = negative, 1 = positive)
description (TEXT, NOT NULL) — forma description
```
🏠 Database: truth_training.sqlite  

#### 2.4.3 Table: cause

**Purpose** :  
**Storage record** reference **causes** for **development** in **context**  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique cause identifier
name        (TEXT, NOT NULL) — cause name
quality     (INTEGER, NOT NULL) — semantic valence (0 = negative, 1 = positive)
description (TEXT, NOT NULL) — cause description
```
🏠 Database: truth_training.sqlite  

#### 2.4.4 Table: develop

**Purpose** :  
**Storage record** reference **development** states in **context**  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique development identifier
name        (TEXT, NOT NULL) — development name
quality     (INTEGER, NOT NULL) — semantic valence (0 = negative, 1 = positive)
description (TEXT, NOT NULL) — development description
```
🏠 Database: truth_training.sqlite  

#### 2.4.5 Table: effect

**Purpose** :  
**Storage record** reference **effect** for **categories** over **forms** → **causes** → **development** in **context**  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique effect type identifier
name        (TEXT, NOT NULL) — effect type name
quality     (INTEGER, NOT NULL) — semantic valence (0 = negative, 1 = positive)
description (TEXT, NOT NULL) — effect type description
```
🏠 Database: truth_training.sqlite  

**Notes "quality" trends** :  
• For each **category** there are **forms** of trend for **development** through **causes** ultimately leading to a certain **effect**

##### Model "quality" :  
• The **trend** of the **context** is **calculated** for the **selected record** of the "truth_event" table  
• The "quality" **model shows** the tendency of an **event participant** to **achieve** the truth of the **consequence**. The **total number** of **selected records** in the "forma," "cause," and "develop" tables with a "quality" **value of 0** (value "N") and a "quality" **value of 1** (value "P") is **compared** to the "quality" **value** "R" of the **selected record** from the "effect" table.

**Source tables** :  
• "forma"  
• "cause"  
• "develop"  
• "effect"

**Impact Axis Rules** :  

**Quality calculation model** :
```sql
N = (
    SELECT COUNT(*)
    FROM forma
    WHERE forma.id = (
        truth_event.forma_id AND forma.quality = 0 )
) + (
    SELECT COUNT(*)
    FROM cause
    WHERE cause.id = (
        truth_event.cause_id AND cause.quality = 0 )
) + (
    SELECT COUNT(*)
    FROM develop
    WHERE develop.id = (
        truth_event.develop_id AND develop.quality = 0 )
)

P = (
    SELECT COUNT(*)
    FROM forma
    WHERE forma.id = (
        truth_event.forma_id AND forma.quality = 1 )
) + (
    SELECT COUNT(*)
    FROM cause
    WHERE cause.id = (
        truth_event.cause_id AND cause.quality = 1 )
) + (
    SELECT COUNT(*)
    FROM develop
    WHERE develop.id = (
        truth_event.develop_id AND develop.quality = 1 )
)

R = (
    SELECT effect.quality
    FROM effect
    WHERE effect.id = (
        truth_event.effect_id )
)
```

**Rules** :  
• The **context** defined by these **rules** can have **4 possible outcomes** of the **event**, **two** of which are **logical** and **two** of which are **illogical**  
• The **first logical** result **contains more negative** "quality" and has a **negative effect (impact)**
```sql
IF "N" > "P" AND "R" = 0
    impact.trend = 0

```
• The **second logical** result **contains more positive** "quality" and has a **positive effect (impact)**
```sql
IF "N" < "P" AND "R" = 1
    impact.trend = 1
```
• The **fourth illogical** result **contains more positive** "quality" and has a **negative effect (impact)**
```sql
IF "N" < "P" AND "R" = 0
    impact.trend = 2
```
• The **third illogical** result **contains more negative** "quality" and has a **positive effect (impact)**
```sql
IF "N" > "P" AND "R" = 1
    impact.trend = 3
```

**Notes** :  
• "quality" **values** are semantic valences **(0 = negative, 1 = positive)**  
• "impact.trend" **reflects** the **participant** tendency toward **achieving true consequences**  
• The **model compares** negative **(N)** vs positive **(P)** trends in **context** elements  
• The "effect" "quality" **(R)** determines the **final impact** trend **direction**  
• An **illogical result** of an **event** is **possible** due to **force majeure**, the **presence** of a third **unspecified party**, **secondary event** or the **hidden cunning** of a **participant**, which **achieving false consequences**

##### Model: Context

• Context is a fixed 5‑tuple "category", "forma", "cause", "develop", "effect"  
• Each element is a foreign key to the corresponding reference table "category.id", "forma.id", "cause.id", "develop.id", "effect.id"

#### 2.4.6 Context, Observers and System Learning

📝 **User-level** table of the Collective Intelligence Layer  
• It is **direct editing for participant input**, and is **not transmitted over the network**  
• This table is **populated** with **context template** examples by the **initialization function** in the application **core module** core/src/storage.rs  
• Default values for context templates are specified in: [Knowledge Base Table Values for Default Seeding](../spec/26-seed_knowledge_base_table_value.md)

**Purpose** :  
• Defines **contexts** (logical, temporal, thematic) within which **event** and **impact** are assessed and **judgment** are formed  
• **Context** provides an interpretation frame for consequences but does not directly affect **truth**  
• It acts as a coordinate system for **event**, **impact** and **judgment**

#### Table: context

**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique context identifier
name        (TEXT, NOT NULL) — context name
category_id (INTEGER, NOT NULL) — FK → category.id  — category reference
forma_id    (INTEGER, NOT NULL) — FK → forma.id — forma reference
cause_id    (INTEGER, NOT NULL) — FK → cause.id — cause reference
develop_id  (INTEGER, NOT NULL) — FK → develop.id — develop reference
effect_id   (INTEGER, NOT NULL) — FK → effect.id — effect reference
description (TEXT, NOT NULL) — context description
```
🏠 Database: truth_training.sqlite  

**Notes** :  
• **Context** sets interpretation frame for consequences but does **not affect truth** directly  
• **Context** table structure in project is implemented to store **context** templates used for classifying and describing **event** in Truth Training system  
• **Event context** can be filled manually by selecting appropriate records from subordinate tables with PK → FK filling  
• For **automatic** filling of **event context** fields "context" table is used

### 2.5 event

• System is distributed: multiple independent **nodes** N = {N₁, N₂, …}  
• Each **node** stores a local copy of the database, separately evaluates **event**, **participates** in **P2P exchange**  
• **Event** does not have a single **truth** — **truth** emerges statistically as a stable form of **collective judgment**  
• System has two independent axes of **event** evaluation :  
 ◦ **Consequence axis → impact**  
 ◦ **Truth axis → judgment**  
• Local metrics (csᵢ, ciᵢ, cjᵢ) are used for training and aggregation — they are not equal to the **final truth**  
• **Event** is central entity of **Truth Training model**  
• It represents a **fixed fact** or **statement around** which **truth assessments** and **observable consequences** are formed

#### Model: event

**Signature** :
```
Eᵢ=⟨gidᵢ,authorᵢ,descᵢ,ctxᵢ,vᵢ,dᵢ,cᵢ,csᵢ,ciᵢ,cjᵢ⟩
```
**Where** :  
- gidᵢ — global **event** identifier
```
	truth_event.global_id  
```
- authorᵢ - creator **public key**
```
	truth_event.participant_id  
```
📝 gidᵢ and authorᵢ are the combined **global identifier** of the **event**  
- descᵢ — **event description**
```
	truth_event.description  
```
- ctxᵢ — semantic **context**  ctx=⟨category,forma,cause,develop,effect⟩
```
	truth_event.category_id
	truth_event.forma_id
	truth_event.cause_id
	truth_event.develop_id
	truth_event.effect_id
```
- vᵢ ∈{0,1} — direction **vector**
```
	truth_event.vector  
```
- dᵢ ∈ {∅,0,1} — **detection flag**  
• **detection flag**, used by participants to indicate that they recognize and affirm the **event** as significant and truthful according to their understanding  
• This flag is used by the transport system to determine the relevance and priority of further event propagation through the network  
• When a participant sets this flag, it indicates that in their opinion the event and its judgments/impacts align with the truth and are relevant from their perspective  
• This flag should be characterized as "I Confirm" in the application interface  
```
	truth_event.detected
```
- cᵢ ∈ [0,255] — **circulation code**  
• **Circulation code**, used by the transport system to control event propagation through the P2P network  
• This field operates as an 8-bit value where upper 2 bits represent service codes (00/01/10/11) and lower 6 bits serve as counter/metadata (0..63)  
• Code `01` indicates a "permanent" event that gets relayed through the network, originally assigned by the author on creation  
• The code controls distribution protocol but does not participate directly in truth calculation - it affects only transport logic  
• Code transitions (00↔01, 01→11, etc.) are determined automatically based on event evaluation scores (S_e) and threshold rules (T_up, T_down)  
• When forwarding events, nodes may decrement counter bits to control propagation scope, or temporarily change codes to manage network traffic  
• For detailed algorithm see [docs/event_rating_protocol.md](event_rating_protocol.md) and [spec/07-event-rating-protocol.md](../spec/07-event-rating-protocol.md)
```
	truth_event.code
```
- csᵢ - learning **progress metric**
```
	truth_event.collective_score  
```
csᵢ - Calculated value of training progress  
• **Calculated** by corresponding algorithm working at **local level** individually for **participant without using network**  
• On **global level** used for calculating **group training**  
📌 csᵢ — is **not** the final **truth**, but a **training metric** based on **event** at **local level**  
- ciᵢ - **impact metric**
```
	truth_event.impact_score
```
ciᵢ - Calculated value of **event impact**  
• **Calculated **by corresponding algorithm working at **local level**  
• When transmitted **over network** aggregated at **local nodes** for averaging and subsequent transmission to **next nodes**  
📌 ciᵢ - is **not** the final **truth**, but a **impact metric** of **event** at **local level**  
- cjᵢ - **judgment metric**
```
	truth_event.judgment_score
```
cjᵢ - **Calculated** value of **judgment** about **event**  
• **Calculated** by corresponding algorithm working at **local level**  
• When transmitted **over network** aggregated at **local nodes** for averaging and subsequent transmission to **next nodes**  
📌 cjᵢ - is **not** the final **truth**, but a **judgment metric** about **event** at **local level**  

**Calculation details for impact_score and judgment_score** :

The "impact_score" field represents the cumulative **impact assessment** of the **event** at the **local node** level. It is calculated based on the **impact** records stored in the "impact" table that are associated with this **event**. The calculation algorithm aggregates the individual **impact values** taking into account their types, timestamps, and the **reputation** of the **participants** who made the **impact assessments**.

The "judgment_score" field represents the cumulative **truth assessment** of the **event** at the **local node** level. It is calculated based on the **judgment** records stored in the "judgment" table that are associated with the corresponding **event** in the "event_ci" table. The calculation algorithm aggregates the individual **judgments taking** into account their **confidence levels**, **assessment types**, and the **reputation** of the **participants** who made the **judgments**

**Both scores** are continuously updated as new **impact** and **judgment** data becomes available at the **local node**. They reflect the **local node's** current understanding of the **event's impact** and **truth value** based on available information, but are **not** the final **global truth** values

- crᵢ - **correction flag**, used locally to review the **impact** for **training progress**  
📝 Not transmitted over the network
```
	truth_event.corrected
```
⭐️ **All** of the above **parameters**, **except descᵢ and crᵢ**, are used for **filtering** and **sorting** when **viewing event**

#### ⚠️ Important :  
• Fields "code" (cᵢ), "detected" (dᵢ), "corrected" (crᵢ) do **not participate** in **truth calculation directly**, **only** in **transport logic**  
• Fields "collective_score" (csᵢ), "impact" (ciᵢ), "judgment" (cjᵢ) influence **event relevance**, **training progress calculation** and **truth determination**

#####   Collective Event Assessment

**Set of event impact** :
```
Iᵢ(Eᵢ)={Iᵢ(1),Iᵢ(2),…,Iᵢ(n)}  
```
**where** :  
I(Eᵢ) — set of **participant impact**

**Divide by sign** :  
```
Pᵢ=∑Iᵢ(ij)^(+), Nᵢ=∑Iᵢ(ij)^(-)  
```
**where** :  
- Pᵢ — **sum of positive impacts** for event i (∑ of Iᵢ(ij) where **impact** is positive)  
- Nᵢ — **sum of negative impacts** for event i (∑ of Iᵢ(ij) where **impact** is negative)  
- Iᵢ(ij) — individual **impact assessments** for **event** i by **participant** j  
- The superscripts (+) and (-) denote positive and negative **impact** values respectively

###### Truthfulness as Statistical Function (local)
```
csᵢ = f-local(I(Eᵢ))  
```
• **function** "f-local" depends **only on local data**  
• **network not used**

###### Truthfulness as Statistical Function (global)

• At **global level**, using network infrastructure, statements **table** is used, "statements.truth_score" is **transferred to global level** for calculating **group training**, calculated based on **all event** and csᵢ field "truth_event.collective_score" :
```
truth_scoreᵢ-global = f-global({ csᵢ-local_j })
```
**Where** :  
{csᵢ-local_j} — local assessments of different nodes

-aggregated without trust to source  
-stability arises statistically

**progress_metrics** aggregates :  
-individual  
-group  
-comparative trends

**Event truthfulness** is not stored but calculated :
```
Truth(Eᵢ) = (Pᵢ − Nᵢ) / (|I(Eᵢ)| + ε)
```
**Where** :  
ε — protection from division by zero  
result ∈ (−1, +1)

**Interpretation** :  
→ +1 : stably confirmed  
→ −1 : stably refuted  
≈ 0 : conflict / lack of data

**Model Collective Event Assessment** :
 
##### ⚠️ for more details see 👇 [model_core_collective_assessment.md](model_core_collective_assessment.md)

**For detailed SQL implementation see** 👇:  
- [model_core_views_collective_assessment.md](model_core_views_collective_assessment.md) — Views for collective assessment calculations  
- [model_core_collective_assessment.md](model_core_collective_assessment.md) — Collective assessment logic implementation  
- [model_core_views_scoring.md](model_core_views_scoring.md) — Impact and judgment scoring calculations  
- [model_core_scoring.md](model_core_scoring.md) — Trigger implementation for scoring calculations  
- [model_core_aggregated_metrics.md](model_core_aggregated_metrics.md) — System metrics and expert functions schema  
- [model_core_views_aggregated_metrics.md](model_core_views_aggregated_metrics.md) — Views for aggregated metrics  
- [model_core_network_tables.md](model_core_network_tables.md) — Node discovery and network tables schema  
- [model_core_views_network_tables.md](model_core_views_network_tables.md) — Views for network operations  

#### Table: truth_event

📝 **User-level** table of the Collective Intelligence Layer  
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
• **Storing** main data about **event** in **Truth Training system**, including **their description**, **context**, **timestamps**, **vector** (incoming/outgoing), **detection** and **correction status**, and **various assessments**  
**Fields** :
```
id               (INTEGER, PK, AUTOINCREMENT) — unique truth_event identifier
description      (TEXT, NOT NULL) — event description
global_id        (TEXT, NOT NULL, UNIQUE) — global event identifier for network identification
participant_id   (TEXT, NOT NULL) — FK → participants.public_key
signature        (TEXT, NOT NULL) — cryptographic signature
category_id      (INTEGER, NOT NULL) — FK → category.id
forma_id         (INTEGER, NOT NULL) — FK → forma.id
cause_id         (INTEGER, NOT NULL) — FK → cause.id
develop_id       (INTEGER, NOT NULL) — FK → develop.id
effect_id        (INTEGER, NOT NULL) — FK → effect.id
vector           (INTEGER, NOT NULL) — event direction (0/1) 0-incoming; 1-outgoing
detected         (INTEGER) — detection flag (NULL/0/1)
corrected        (INTEGER, NOT NULL, DEFAULT 0) — correction flag
timeline_id      (INTEGER, NOT NULL) — FK → event_timeline.id
code             (INTEGER, NOT NULL, DEFAULT 1) — circulation code for distribution protocol
collective_score (REAL, NOT NULL) — local training/assessment metric
impact_score     (REAL, NOT NULL) — local impact metric
judgment_score   (REAL) — local judgment metric
```
🏠 Database: truth_training.sqlite  

**Notes** :  
• **Event identity** is defined by ("global_id", "truth_event","participant_id)", **never** by **local autoincrement id**  
• **Event content** is stored in embedded **context fields** ("category", "forma", "cause", "develop", "effect")  
• Circulation **code** controls distribution **protocol**, **not truth** calculation  
• **collective_score**, "impact_score", and "judgment_score" are **local metrics**, **not** final **truth values**  
• "timeline_id"  Each **participant** has **their own chronology** of **event**  

#### Table: event_links

📝 **User-level** table of the Collective Intelligence Layer  
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
Describes logical and causal **relationships** between **event**  
**Fields** :
```
source_impact_id (INTEGER, NOT NULL) — FK → truth_event.id — source event reference
target_impact_id (INTEGER, NOT NULL) — FK → truth_event.id — target event reference
relation_type    (TEXT, NOT NULL) — ENUM (basic / equal / foreign)
created_at       (INTEGER, NOT NULL) — timestamp of creation
```
🏠 Database: truth_training.sqlite  

**Notes** :  
• **Implements** an event **graph**  
• Used to **analyze** primary and secondary **events**

The **event axis** reflects the **chain of events** over **time**  
• An **event** can be **repeated** over **time** with consensus confirmation  
• An **event** is **comparable** to **another event**  
• An **event** forms **feedback** on **system training**

#### Model Truth Event :

The Truth Event model represents the core entity in the Truth Training system that stores event information and associated metrics. The table structure is defined as follows :

**Table: truth_event**

📝 **User-level** table of the Collective Intelligence Layer
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
• **Storing** main data about **event** in **Truth Training system**, including **their description**, **context**, **timestamps**, **vector** (incoming/outgoing), **detection** and **correction status**, and **various assessments**  
**Fields** :
```
id               (INTEGER, PK, AUTOINCREMENT) — unique truth_event identifier
description      (TEXT, NOT NULL) — event description
global_id        (TEXT, NOT NULL, UNIQUE) — global event identifier for network identification
participant_id   (TEXT, NOT NULL) — FK → participants.public_key
signature        (TEXT, NOT NULL) — cryptographic signature
category_id      (INTEGER, NOT NULL) — FK → category.id
forma_id         (INTEGER, NOT NULL) — FK → forma.id
cause_id         (INTEGER, NOT NULL) — FK → cause.id
develop_id       (INTEGER, NOT NULL) — FK → develop.id
effect_id        (INTEGER, NOT NULL) — FK → effect.id
vector           (INTEGER, NOT NULL) — event direction (0/1) 0-incoming; 1-outgoing
detected         (INTEGER) — detection flag (NULL/0/1)
corrected        (INTEGER, NOT NULL, DEFAULT 0) — correction flag
timeline_id      (INTEGER, NOT NULL) — FK → event_timeline.id
code             (INTEGER, NOT NULL, DEFAULT 1) — circulation code for distribution protocol
collective_score (REAL, NOT NULL) — local training/assessment metric
impact_score     (REAL, NOT NULL) — local impact metric
judgment_score   (REAL) — local judgment metric
```
🏠 Database: truth_training.sqlite  

**Notes** :  
• **Event identity** is defined by ("global_id", "truth_event","participant_id)", **never** by **local autoincrement id**  
• **Event content** is stored in embedded **context fields** ("category", "forma", "cause", "develop", "effect")  
• Circulation **code** controls distribution **protocol**, **not truth** calculation  
• **collective_score**, "impact_score", and "judgment_score" are **local metrics**, **not** final **truth values**  
• "timeline_id"  Each **participant** has **their own chronology** of **event**

**Invariant:** identity is defined by ("gid_i", "author_i"), never by local autoincrement "id"

**Field explanations** :  
• "global_id" — mandatory UUID field used to identify same **event** on different **nodes**  
• "participant_id" — creator's public key (FK to "participants" table)  
• "vector" — **event** direction (e.g. "outgoing"/"incoming")  
• "detected", "corrected", "code" — auxiliary transport fields (see [docs/event_rating_protocol.md](event_rating_protocol.md))  
• "collective_score" (csᵢ) — local **training**/**assessment metric** calculated based on **impact** and **judgment** assessments; this score represents the aggregated **local assessment** of the **event's truth** value and is updated through SQL triggers when new **impact** or **judgment** data is added (see [docs/model_core_collective_assessment.md](model_core_collective_assessment.md) for trigger details)  
• "impact_score" (ciᵢ) — local **impact metric** (see section 2.2.3 and 2.6.1)  
• "judgment_score" (cjᵢ) — local **judgment metric** (see section 2.2.4 and 2.6.2)

**Collective Score Calculation** :

The "collective_score" (csᵢ) is calculated using the following approach :

1. **Local Impact Assessment**: Based on the sum of "positive" and "negative" **impacts** associated with the **event**
   ```
   csᵢ = f-local(I(Eᵢ))
   ```
   **Where** I(Eᵢ) represents the set of **participant impacts** for **event i**

2. **Impact Calculation**:
   ```
   Pᵢ=∑Iᵢ(ij)^(+) — sum of positive impacts for event i
   Nᵢ=∑Iᵢ(ij)^(-) — sum of negative impacts for event i
   ```

3. **Truth Calculation**:
   ```
   Truth(Eᵢ) = (Pᵢ − Nᵢ) / (|I(Eᵢ)| + ε)
   ```
   **Where** ε is a **small constant** to protect from division by zero, and result ∈ (−1, +1)

**Trigger Implementation**:

The "collective_score" is automatically updated through SQL triggers that respond to changes in the "impact" and "judgment" tables:

- `update_impact_score_after_impact_change` - updates the "collective_score" when new **impact** data is added (see [docs/model_core_scoring.md](model_core_scoring.md))  
- `update_judgment_score_after_judgment_change` - updates the "collective_score" when new **judgment** data is added (see [docs/model_core_scoring.md](model_core_scoring.md))
- `initialize_event_metrics` - initializes the "collective_score" to a default value (0.5) when a new **event** is created (see [docs/model_core_collective_assessment.md](model_core_collective_assessment.md))


##### Where structure is described :  
• In data schema documentation: docs/Data_Schema.md - contains description of truth_event table as part of base block, including all main fields and their purpose  
• In implementation code: core/src/storage.rs - contains SQL definition of table in "SCHEMA_SQL" constant  
• In event rating protocol documentation: [docs/event_rating_protocol.md](event_rating_protocol.md)  - describes use of "code" field and "score" calculation based on data from this table

### 2.6 impact Assessment and judgment

📌 Key idea :  
• **judgment** are the key to determining **truth**. After an **event** is **created**, its **truth** is determined through the system table: "event_ci"  
• **impact** is observation and **prediction** of consequences for **participant**, not opinion on truthfulness. For each **participant** consequences may be different  
• After an **event** is **created** and its **truth** is determined through, the final **consequences** are determined using the system table: "event_ci"  

#### 2.6.1 Impact

##### Model: impact  
Impact is formalized as a vector :  
```
impact.trend= Quality⟨event_id, type_id, value, t⟩
```
**Where** :  
- "event_id" → truth_event.id (FK)  
- "type_id" → effect.id (FK)  
- "value" ∈{NULL,0,1} — (measurable/negative/positive)  
- "t" - time of recording

**Impact** aggregates into a **local metric** :
```
Is = summarize_impact(I(Eᵢ))
```
**Purpose** :  
• Structure of "impact_type" and "impact" tables in project is implemented to **store event impact** assessments in **Truth Training** system  
• "effect" table - used for classifying types of **event impact** (reputational, financial, moral, etc.). **Serves as reference** for **impact types** that can be applied to **event**  
• "impact" table - used for **storing** subjective assessments **impact** of **event** issued by **participants**  
• Each **record** represents **assessment** of specific **event** by specific **participant**, where :  
 ◦ "impact" — subjective **observation and prediction** of **consequences** from specific **participant**  
 ◦ "impact" is **not** equal to **truth judgment**  

##### ⚠️ Important :  
• Tables are integrated into **event assessment** system and used for calculating **collective event assessments** (S_e) based on weighted average by validators, and for updating author and validator **reputations**.
  See also :  
  ◦ [Concept_Collective_Intelligence.md](Concept_Collective_Intelligence.md) for algorithm **"Wisdom of the Crowd"** (S_e)  
  ◦ [event_rating_protocol.md](event_rating_protocol.md) for algorithm description for calculating assessments based on data from "impact" table  
  ◦ [model_core_scoring.md](model_core_scoring.md) for detailed SQL implementation of "impact_score" calculation

#### Impact Score Calculation Logic

The "impact_score" field in the "truth_event" table represents the cumulative **impact assessment** of the **event** at the **local node level**. The calculation algorithm aggregates the individual **impact values** taking into account their types, timestamps, and the **reputation** of the **participants** who made the **impact assessments**.

**Calculation Formula:**
```
impact_score = Σ(impact_value_i * participant_reputation_i) / N
```

**Where** :  
- "impact_value_i" is the value of the i-th **impact assessment** (1 for "positive", 0 for "negative", NULL for "undefined")  
- "participant_reputation_i" is the **reputation score** of the **participant** who made the i-th **impact assessment**  
- "N" is the total number of **impact assessments** for the **event**

**Implementation Details:**  
- When a new **impact** record is added, the `update_impact_score_after_impact_change` trigger automatically recalculates the "impact_score" for the associated **event**  
- The calculation considers the **participant's reputation** score from the "participants" table  
- **Positive impacts** (value = 1) contribute "positively" to the score
- **Negative impacts** (value = 0) contribute "negatively" to the score
- The score is normalized by the total number of assessments to maintain consistency

**SQL Implementation:**  
The calculation is implemented through the "impact_score_calculation" view and update triggers in [model_core_scoring.md](model_core_scoring.md).

##### Table: impact  
📝 **User-level** table of the Collective Intelligence Layer  
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
**Storing** subjective assessments **impact** of **event** issued by validators, representing **observation and prediction** of **consequences** from specific **participant**, **not** opinion on **truthfulness**.  
**Fields** :
```
id                 (INTEGER, PK, AUTOINCREMENT) — unique impact identifier
event_id           (INTEGER, NOT NULL) — FK → truth_event.id
type_id            (INTEGER, NOT NULL) — FK → effect.id (Reference knowledge-base)
trend              (INTEGER, NOT NULL) — impact trend 0 /1 / 2 / 3  ("logical_negative"/"logical_positive"/"illogical_negative"/"illogical_positive")
value              (INTEGER) — impact value (NULL/0/1 for measurable/negative/positive)
notes              (TEXT) — additional notes about the impact
impact_metrics     (INTEGER, NOT NULL) — FK → impact_metrics.id
impact_predictions (INTEGER, NOT NULL) — FK → impact_predictions.id
timeline_id        (INTEGER, NOT NULL) — FK → impact_timeline.id
```
🏠 Database: truth_training.sqlite  

**Notes** :  
• **Consequence** is always **linked** to specific **event**  
• One **event** can have **multiple consequences**  
• **Consequences** are aggregated at DB level  
• "value" is **boolean** (NULL/0/1) **indicating** measurable/negative/positive **impact**  
• "timeline_id"  Each **participant** has their own **chronology** of **impact**

##### Table: impact_links

📝 **User-level** table of the Collective Intelligence Layer  
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
Allows **linking consequences** to each **other**, forming chains of **cause-and-impact** relationships.   
**Fields** :
```
source_impact_id (INTEGER, NOT NULL) — FK → impact.id — source impact reference
target_impact_id (INTEGER, NOT NULL) — FK → impact.id — target impact reference
relation_type    (TEXT, NOT NULL) — ENUM (supports / contradicts / refines)
created_at       (INTEGER, NOT NULL) — timestamp of creation
```

**Where "relation_type"** :  
- "supports" — **strengthening** of **consequences**  
- "contradicts" — **weakening**  
- "refines" — **clarification**

**This allows us to construct** :  
• **secondary consequences**  
• **delayed effects**  
• **nonlinear chains**

🏠 Database: truth_training.sqlite  

**Notes** :  
• Implements **consequence** graph  
• Used for **analysis** of **secondary impact**  
• **Impact Axis** reflects objective or observable **impacts** of **event** over **time**

**Unlike truth axis** :  
• **impact** can exist **without consensus**  
• **impact** is **measurable** and **comparable**  
• **impact** forms system's **learning feedback**

##### Table: impact_metrics

**Purpose** :  
**Storage record** aggregated **metrics** of **event consequences**  
**Fields** :
```
id              (INTEGER, PK, AUTOINCREMENT) — unique impact_metrics identifier
event_id        (INTEGER, NOT NULL) — FK → event_ci.id
total_magnitude (INTEGER) — Overall impact significance
positive_ratio  (INTEGER) — Positive rating
negative_ratio  (INTEGER) — Negative rating
uncertainty     (INTEGER) — Undefined rating
calculated_at   (INTEGER, NOT NULL) — timestamp of calculated
```
🏠 Database: truth_training.sqlite  

**Model impact_metrics** :  
**Source relation**
```sql
impact.event_id = truth_event.id
truth_event.id = event_ci.created_by
impact_metrics.event_id = event_ci.id
```
**Base event mapping**
```sql
base_event_id =
SELECT event_ci.created_by
FROM event_ci
WHERE event_ci.id = impact_metrics.event_id
```
**Aggregation formulas "total_magnitude"**
```sql
impact_metrics.total_magnitude = (
    SELECT COUNT(*)
    FROM impact
    JOIN truth_event ON impact.event_id = truth_event.id
    JOIN event_ci ON truth_event.id = event_ci.created_by
    WHERE impact.event_id = (
        SELECT event_ci.created_by
        FROM event_ci
        WHERE event_ci.id = impact_metrics.event_id
    )
    AND impact.value IS NOT NULL
    AND (event_ci.status = 'resolved' OR event_ci.status = 'archived' AND event_ci.old_status = 'active')
)

-- Update old_status when transitioning from active to impact type
IF event_ci.status = 'active' AND event_ci.event_type = 'impact'
   event_ci.old_status = event_ci.status
ENDIF

```
**Aggregation formulas "positive_ratio"**
```sql
impact_metrics.positive_ratio =
SELECT COUNT(*)
FROM impact
WHERE impact.event_id = base_event_id
AND impact.value = 1
```
**Aggregation formulas "negative_ratio"**
```sql
impact_metrics.negative_ratio =
SELECT COUNT(*)
FROM impact
WHERE impact.event_id = base_event_id
AND impact.value = 0
```
**Aggregation formulas "uncertainty"**
```sql
impact_metrics.uncertainty =
SELECT COUNT(*)
FROM impact
WHERE impact.event_id = base_event_id
AND impact.value IS NULL
```
**Aggregation formulas "calculated_at"**
```sql
impact_metrics.calculated_at = CURRENT_TIMESTAMP
```

##### Table: impact_predictions

**Purpose** :  
• **Storage** record **predicted consequences** of **event**  
• **Part** of the **event evaluation as part** of **participant training**  
• To provide a **mechanism** for **participants** to demonstrate their **predictive** abilities regarding **consequences**  
• To integrate **predictive accuracy** into the **collective intelligence's** trust and weighting mechanisms by adjusting **participant reputation** based on the **historical** correctness of their **impact** forecasts

**Historical Preservation** :  
• The "impact_predictions" table maintains **historical** records of **predictions** over **time**  
• **Multiple records** can exist for the **same event**, capturing the **evolution of predictions**  
• This **enables aggregation** and analysis of **prediction accuracy** over **time**  
• Records in "impact_predictions" are **created** when a participant creates an "impact" record that relates to a **future**. **predicted**,  **factual** consequence and **changes** state **event** (e.g. from "active"/"resolved" to "archived" in "event_ci.status", **change** the **end date** of an **event**)  
• The **accuracy** of these **predictions**, measured by comparing "impact_predictions.expected_strength" and "impact_predictions.probability" against the actual realized **impact** data stored in the "impact" and "truth_event" tables, influences the **participant's reputation**

**Accuracy Assessment** :  
• During the **impact** assessment of an **event**, the system evaluates the **accuracy** of past **predictions** ("impact_predictions") against the **aggregated** factual **impact** data ("truth_event.collective_score", "impact.value", "impact.type_id")  
• **Predictions** that closely match the actual outcomes increase the **participant's reputation**; inaccurate **predictions** decrease it

**Reputation Fields Affected** :  
• "participants.accurate_impact" (see Table: participants, Table: reputation_history): **Incremented** when a **participant's impact prediction** proves **accurate**  
• "participants.total_impact" (see Table: "participants", Table: "reputation_history"): **Incremented** for each **impact prediction** made by the **participant** (regardless of accuracy)

**Aggregation and Updates** :  
• **Comparison** and subsequent **reputation** adjustments occur as data is received for an **event**  
• **Participant reputation** is calculated by **aggregating** all **prediction records** from the "impact_predictions" table across all events created by the **participant**  
• **Participant identification**: The connection is established through "impact_predictions.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id"  
• **Reputation calculation** considers the **horizon** value: **predictions** made earlier (with larger "horizon" values) have more "weight" in the **reputation calculation**  
• When an **event** status changes to "resolved" or "archived", the system evaluates the accuracy of all **predictions** for that **event** by comparing "impact_predictions.expected_strength" and "impact_predictions.probability" against the actual realized **impact** data ("truth_event.collective_score")  
• **Prediction accuracy** is determined by comparing the difference between "expected_strength" and the actual "collective_score": a **prediction** is considered accurate if the absolute difference is within 20% of the "expected_strength" (with a minimum threshold of 0.1)  
• **Horizon-based weighting**: **Predictions** with larger "horizon" values (made before the **event occurred**, when t_end was NULL) receive greater weight in **reputation calculations**, as they demonstrate **predictive ability**  
• After the **event** end date ("t_end") is determined, all calculations use concrete values based on the **event** status in the "event_ci" table  
• The **weighted accuracy** for reputation is calculated as : 
```
SUM(accurate_predictions * (horizon + 1.0)) / SUM(all_predictions * (horizon + 1.0)), where accurate predictions have horizon-weighted contributions  
``` 
**See also** :  
• **Participant Reputation Model** (Table: "participants", Table: "reputation_history"): Defines how "accurate_impact" and "total_impact" contribute to "reputation_score"  
• **Tables: participants** (Table: "participants"): Contains the "accurate_impact" and "total_impact" counters  
• **Tables: truth_event** (Table: "truth_event"): Source of "collective_score" used for comparison with "expected_strength"  
• **Tables: impact** (Table: "impact"): Source of individual **impact** assessments used for comparison and aggregation into "truth_event"  
• **Tables: event_ci** (Table: "event_ci"): Defines **event** lifecycle ("status" field) which likely triggers the final accuracy assessment and **reputation** update  

- "impact" — **fact** / **observation** / **consequence**  
- "impact_predictions" — **predict** of **consequences**  
- "judgment" / (truth) — **opinion** on **veracity**

**Accuracy of impact_predictions ⟂ correctness of judgment**
```
Impact(E) ⟂ Truth(E)
```

##### Model: impact_predictions
```
I_= ⟨event_id, type_id, value, event_ci, t⟩

```
**Where** :  
 - "event_id" → "truth_event.id" (FK)  
 - "type_id" → "effect.id" (FK)  
 - "value" ∈{NULL,0,1} — ("measurable"/"negative"/"positive")  
 - "event_ci" - The table is used as a **neuron** filter for selection by type of **impact** for **aggregation**  
 - "t" - time of recording

**Total impact of event E** :
```
I_P = probability * horizon
```

**Fields** :
```
id                    (INTEGER, PK, AUTOINCREMENT) — unique impact_predictions identifier
event_id              (INTEGER, NOT NULL) — FK → event_ci.id
predicted_impact_type (INTEGER, NOT NULL) — FK → effect.id
expected_strength     (REAL, NOT NULL) — expected expression, signal strength
probability           (REAL, NOT NULL) — participants confidence that the predicted effect occurred
horizon               (REAL, NOT NULL) — times inevrval, predicted time lag
created_at            (INTEGER, NOT NULL) — timestamp of calculated
```
🏠 Database: truth_training.sqlite

👉 - "impact_predictions.event_id" — **prediction** refers to **systems event**

👉 - "impact_predictions.predicted_impact_type" — **prediction** refers to **systems** type **context** of **consequence**

👉 - "impact_predictions.expected_strength"

```
I_P(E) = Σ (I_Sᵢ / Hᵢ)
```
**Where** :  
 - I_P(E) — "impact_predictions.expected_strength"  
 - I_Sᵢ — "truth_event.collectiVe_score"  
 - Hᵢ —  E("truth_event.timeline_id") - "horizon" — the EARLIER the forecast → the LOWER the "horizon" **Event** is **defined** as **temporal process** :
```
E(t) = { T(J(E, t)), I(E, t) } - Consequences can be delayed
```

👉 - "impact_predictions.horizon"
```
= (t_end − created_at) / (t_end − t_start)
```
**Where** :  
 - "t_start" − "truth_event.timeline_id" → "event_timeline.t_start"  
 - "t_end" − "truth_event.timeline_id" → "event_timeline.t_end"  
 - "created_at" − "impact_predictions.event_id"  →  "event_ci.created_at"

👉 - "impact_predictions.probability"
```
= 1 − |I_F − I_E| / max(I_F, I_E)
weight(i) = BASE * e^(−d(i))
I_F = Σ weight(post_fact_impact)
I_E = expected_strength
```
**Where** :  
d(i) — **impact** depth **from root**  
 - "root": d = 0  
 - "direct consequences": d = 1  
 - "secondary consequences": d = 2, etc.

**Model "impact_predictions"** :
```sql
CREATE TABLE impact_predictions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- System-level event reference (neuron selector, not truth_event)
    event_id INTEGER NOT NULL,
    -- FK → event_ci.id

    -- Predicted consequence type (knowledge-base reference)
    predicted_impact_type INTEGER NOT NULL,
    -- FK → effect.id

    -- Expected signal strength (aggregated expectation)
    expected_strength REAL NOT NULL,
    -- Computed in Core: Σ(I_Sᵢ / Hᵢ)

    -- Participant confidence in prediction [0..1]
    probability REAL NOT NULL,
    CHECK (probability >= 0 AND probability <= 1),

    -- Temporal distance of prediction relevance
    horizon REAL NOT NULL,
    CHECK (horizon >= 0),

    -- Prediction creation time
    created_at INTEGER NOT NULL,

    -- Integrity constraints
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (predicted_impact_type) REFERENCES effect(id)
);
```
**Source relation**
```sql
impact_predictions.event_id = event_ci.id
truth_event.id = event_ci.created_by
impact_predictions.predicted_impact_type = effect.id
```
**Base event mapping**
```sql
base_event_id =
SELECT event_ci.created_by
FROM event_ci
WHERE event_ci.id = impact_predictions.event_id
```

**Aggregation formulas "calculated_at"**
```
impact_predictions.created_at = CURRENT_TIMESTAMP
```

**Aggregation formulas "horizon"**
```sql
impact_predictions.horizon = (
    SELECT (CASE
              WHEN event_timeline.t_end IS NULL
              THEN GREATEST(0, (event_timeline.t_start - (SELECT created_at FROM event_ci WHERE id = impact_predictions.event_id)))
              ELSE GREATEST(0, (event_timeline.t_end - (SELECT created_at FROM event_ci WHERE id = impact_predictions.event_id)))
            END) / (CASE
                      WHEN event_timeline.t_end IS NULL
                      THEN (CASE
                              WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
                              THEN 0.000001
                              ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
                            END)
                      ELSE (event_timeline.t_end - event_timeline.t_start + (CASE
                        WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
                        THEN 0.000001
                        ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
                      END))
                    END)
    FROM truth_event
    JOIN event_timeline ON truth_event.timeline_id = event_timeline.id
    JOIN event_ci ON truth_event.id = event_ci.created_by
    WHERE event_ci.id = impact_predictions.event_id
)
```
**Aggregation formulas "expected_strength"**
```sql
impact_predictions.expected_strength = (
    SELECT SUM(truth_event.collective_score / (impact_predictions.horizon + (
        CASE
          WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
          THEN 0.000001
          ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END)))
    FROM truth_event
    JOIN event_ci ON truth_event.id = event_ci.created_by
    WHERE event_ci.id = impact_predictions.event_id
)
```
**Aggregation formulas "probability"**
```sql
impact_predictions.probability = (
    SELECT 1 - ABS(COALESCE(AVG(i.value), 0) - te.collective_score) / (COALESCE(te.collective_score, 0.5) + (
        CASE
          WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
          THEN 0.000001
          ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END))
    FROM impact i
    JOIN truth_event te ON i.event_id = te.id
    WHERE te.id = (
        SELECT created_by FROM event_ci WHERE id = impact_predictions.event_id
    )
)
```

**Notes** :  
• **Prediction** ≠ **fact**  
• **Prediction** do **not** directly affect **truth**

**System** :  
• measures **consequences** without moral **assessment**  
• allows **uncertainty**  
• supports **prediction** without **truth assertion**

#### 2.6.2 Judgment

##### Model: judgment
Judgment is formalized as a vector :
```
J = ⟨participant_id, event_id, assessment, confidence_level, t⟩
```
**Where** :  
- "participant_id" → "participants.id" (FK)  
- "event_id" → "event_ci.id" (FK)
- "assessment" ∈{NULL,-1,0,1} — ("NULL"/"undefined"/"false"/"true") - used for calculating **local metric** and aggregation into **local metric** :  
```
J_local = f(judgment₁, judgment₂, ..., judgmentₙ), where each judgment includes assessment, confidence_level and time
```
- "confidence_level" ∈ [0,1] — confidence in the assessment  
- "t" - time of recording

**Judgment** aggregates into a **local metric** :
```
J_local = f(judgment₁, judgment₂, ..., judgmentₙ)
```
**Purpose** :  
• Structure of "judgment" table in project is implemented to **store event judgment** assessments in **Truth Training** system  
• "judgment" table - used for **storing** subjective assessments **truth** of **event** issued by **participants**  
• Each **record** represents **assessment** of specific **event** by specific **participant**, where :  
 ◦ "judgment" — subjective **opinion on truthfulness** from specific **participant**  
 ◦ "judgment" is **not** equal to **impact assessment**

##### ⚠️ Important:
• Table is integrated into **event assessment** system and used for calculating **collective event assessments** and for updating **participant reputations**  
  See also :  
 ◦ [Concept_Collective_Intelligence.md](Concept_Collective_Intelligence.md) for algorithm **"Wisdom of the Crowd"**  
 ◦ [event_rating_protocol.md](event_rating_protocol.md) for algorithm description for calculating assessments based on data from "judgment" table  
 ◦ [model_core_scoring.md](model_core_scoring.md) for detailed SQL implementation of "judgment_score" calculation

##### Judgment Score Calculation Logic

The "judgment_score" field in the "truth_event" table represents the cumulative **truth assessment** of the **event** at the **local node** level. The calculation algorithm aggregates the individual **judgments taking** into account their confidence levels, assessment types, and the **reputation of the participants** who made the **judgments**.

**Calculation Formula:**

```
judgment_score = Σ(judgment_value_i * confidence_level_i * participant_reputation_i) / N
```

**Where** :  
- "judgment_value_i" is the value of the i-th **judgment assessment** (1 for 'true', -1 for 'false', 0 for other values)
- "confidence_level_i" is the confidence level of the i-th **judgment** (0.0 to 1.0)
- "participant_reputation_i" is the **reputation score** of the **participant** who made the i-th **judgment**  
- "N" is the total number of **judgments** for the **event**

**Implementation Details** :  
- When a new **judgment** record is added, the `update_judgment_score_after_judgment_change` trigger automatically recalculates the "judgment_score" for the associated **event**  
- The calculation considers both the **participant's** confidence level and **reputation score** from the "participants" table  
- 'True' **judgments** (assessment = "true") contribute "positively" to the score
- 'False' **judgments** (assessment = "false") contribute "negatively" to the score
- The score is normalized by the total number of assessments to maintain consistency

**SQL Implementation:**  
The calculation is implemented through the `judgment_score_calculation` view and update triggers in [model_core_scoring.md](model_core_scoring.md).

##### Table: judgment

📝 **User-level** table of the Collective Intelligence Layer
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
Storing **judgment** that **collective intelligence** system **participants** issue about **event**, representing assessment of specific **event** by specific **participant**  
**Fields** :  
```
id               (INTEGER, PK, AUTOINCREMENT) — unique judgment identifier
participant_id   (INTEGER, NOT NULL) — FK → participants.id
event_id         (INTEGER, NOT NULL) — FK → event_ci.id
assessment       (REAL) — type of assessment ∈{NULL,-1,0,1} — (undefined/false/null/true)
confidence_level (REAL) — confidence level of the assessment
reasoning        (TEXT) — reasoning behind the judgment
consensus_ci     (INTEGER, NOT NULL) — FK → consensus_ci.id
judgment_weights (INTEGER, NOT NULL) — FK → judgment_weights.id
timeline_id      (INTEGER, NOT NULL) — FK → judgment_timeline.id
```
🏠 Database: truth_training.sqlite  

**Constraints** :  
• UNIQUE("participant_id", "event_id") — each **participant** can have only one summarized **judgment** for each **event**

**Notes** :  
• **judgment** is not changed, only new record is possible  
• Absence of **judgment** ≠ **negative judgment**  
• "timeline_id"  Each **participant** has their own **chronology** of **judgment**

**Model "judgment"** :  
**Source relation**
```sql
judgment.participant_id = participants.id
judgment.event_id = event_ci.id
judgment_weights.judgment_id = judgment.id
consensus_ci.judgment_id = judgment.id
```
**Base event mapping**
```sql
base_event_id =
SELECT event_ci.id
FROM event_ci
WHERE event_ci.id = judgment.event_id
```
**Aggregation formulas "assessment"**
```sql
judgment.assessment = (
    SELECT CASE
        WHEN AVG(confidence_level) > 0.7 AND AVG(assessment_value) > 0.5 THEN 'true'
        WHEN AVG(confidence_level) > 0.7 AND AVG(assessment_value) < -0.5 THEN 'false'
        WHEN AVG(confidence_level) > 0.7 THEN 'uncertain'
        ELSE 'undefined'
    END
    FROM (
        SELECT
            CASE
                WHEN assessment = 'true' THEN 1.0
                WHEN assessment = 'false' THEN -1.0
                ELSE 0.0
            END as assessment_value,
            confidence_level
        FROM judgment
        WHERE event_id = base_event_id
    ) assessment_data
)
```
**Aggregation formulas "confidence_level"**
```sql
judgment.confidence_level = (
    SELECT AVG(confidence_level)
    FROM judgment
    WHERE event_id = base_event_id
)
```
**Aggregation formulas "calculated_at"**
```sql
judgment.timeline_id = CURRENT_TIMESTAMP
```

##### Table: judgment_links

**Purpose** :  
Describes logical and causal relationships between judgment  
**Fields** :
```
source_judgment_id (INTEGER, NOT NULL) — FK → judgment.id — source judgment reference
target_judgment_id (INTEGER, NOT NULL) — FK → judgment.id — target judgment reference
relation_type      (TEXT, NOT NULL) — ENUM supports / contradicts / refines
created_at         (INTEGER, NOT NULL) — timestamp of creation
```
🏠 Database: truth_training.sqlite  

**Model "judgment_links"** :  
**Source relation**
```sql
judgment_links.source_judgment_id = judgment.id
judgment_links.target_judgment_id = judgment.id
```
**Base judgment mapping**
```sql
base_judgment_id =
SELECT judgment.id
FROM judgment
WHERE judgment.id = judgment_links.source_judgment_id
```
**Aggregation formulas "relation_type"**
```sql
judgment_links.relation_type = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1 FROM judgment j1, judgment j2
            WHERE j1.id = judgment_links.source_judgment_id
            AND j2.id = judgment_links.target_judgment_id
            AND j1.assessment = j2.assessment
        ) THEN 'supports'
        WHEN EXISTS (
            SELECT 1 FROM judgment j1, judgment j2
            WHERE j1.id = judgment_links.source_judgment_id
            AND j2.id = judgment_links.target_judgment_id
            AND j1.assessment != j2.assessment
        ) THEN 'contradicts'
        ELSE 'refines'
    END
)
```
**Aggregation formulas "created_at"**
```sql
judgment_links.created_at = CURRENT_TIMESTAMP
```

**Notes** :  
• Implements a **judgment graph**  
• Used for secondary **judgment analysis**  
The **judgment axis** reflects emerging **judgments** about an **event** over **time**

##### Table: consensus_ci

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
**Storing** computed consensus on **event** based on **participant judgment**, representing collective opinion formed based on **individual judgment** and used for determining general **event assessment** result  
**Fields** :
```
id                (INTEGER, PK, AUTOINCREMENT) — unique consensus identifier
event_id          (INTEGER, NOT NULL) — FK → event_ci.id
consensus_value   (INTEGER, NOT NULL) — the consensus value reached
confidence_score  (REAL, NOT NULL) — confidence in the consensus
participant_count (INTEGER, NOT NULL) — number of participants involved
calculated_at     (INTEGER, NOT NULL) — timestamp of calculation
algorithm_version (INTEGER, NOT NULL) — version of algorithm used
```
🏠 Database: truth_training.sqlite  

**Model "consensus_ci"** :  
**Source relation**
```sql
consensus_ci.event_id = event_ci.id
judgment.event_id = consensus_ci.event_id
judgment_weights.event_id = consensus_ci.event_id
```
**Base event mapping**
```sql
base_event_id =
SELECT event_ci.id
FROM event_ci
WHERE event_ci.id = consensus_ci.event_id
```
**Aggregation formulas "consensus_value"**
```sql
consensus_ci.consensus_value = (
    SELECT ROUND(AVG(j.assessment_value * jw.weight))
    FROM judgment j
    JOIN judgment_weights jw ON j.participant_id = jw.participant_id
    WHERE j.event_id = base_event_id
)
```
**Aggregation formulas "confidence_score"**
```sql
consensus_ci.confidence_score = (
    SELECT AVG(j.confidence_level * jw.weight)
    FROM judgment j
    JOIN judgment_weights jw ON j.participant_id = jw.participant_id
    WHERE j.event_id = base_event_id
)
```
**Aggregation formulas "participant_count"**
```sql
consensus_ci.participant_count = (
    SELECT COUNT(DISTINCT participant_id)
    FROM judgment
    WHERE event_id = base_event_id
)
```
**Aggregation formulas "calculated_at"**
```
consensus_ci.calculated_at = CURRENT_TIMESTAMP
```

**Notes** :  
• Used for **storing aggregated event** assessment results  
• Enables system to make collective decisions based on individual **participant judgment**

##### Table: judgment_weights

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
• Defines **weight** of **participants judgment** in specific **context**  
• **Weight** reflects system's **trust** in **participant**

##### Model judgment_weights :

Event truth in context Ctx :
```
T(E, Ctx) = Σ (Jᵢ × Wᵢ × Cᵢ) / Σ (|Wᵢ|)
```
**Where**  :  
- Jᵢ — **judgment** value  
- Wᵢ — subject **weight**  
- Cᵢ — subject **confidence**  
- Convergence → consensus formation  
- Divergence → sign of complex or manipulable **event**  
- Absence of convergence ≠ falsehood  

• Assessments become obsolete.  
• Truth has temporal dynamics.

Decay function is introduced  :
```
w(t) = w₀ * e^(-λt)  
```
**Possible states** :  
- consensus  
- polarization  
- uncertainty  
- conflict

**Judgment Axis** ⟂ **Impact Axis**

Truth and consequences are independent :  
• "True" **event** can have catastrophic **consequences**  
• "False" **event** can have no **consequences**  

Both axes intersect in limit of infinite observation  
- **Judgment** → **neuron** activation  
- **Weight** → synaptic coefficient  
- **Collective Truth** → layer output  
- **Human** → activation function  
**Fields** :
```
id             (INTEGER, PK, AUTOINCREMENT) —
participant_id (INTEGER, NOT NULL) — FK → participants.id
event_id       (INTEGER, NOT NULL) — FK → event_ci.id
weight         (REAL) — participant's trust weight in judgment calculations
calculated_at  (INTEGER, NOT NULL) — timestamp of creation
```
🏠 Database: truth_training.sqlite  

**Model "judgment_weights"** :  
**Source relation**
```sql
judgment_weights.participant_id = participants.id
judgment_weights.event_id = event_ci.id
judgment.participant_id = judgment_weights.participant_id
```
**Base participant mapping**
```sql
base_participant_id =
SELECT participants.id
FROM participants
WHERE participants.id = judgment_weights.participant_id
```
**Aggregation formulas "weight"**
```sql
judgment_weights.weight = (
    SELECT reputation_score
    FROM participants
    WHERE participants.id = base_participant_id
)
```
**Aggregation formulas "calculated_at"**
```sql
judgment_weights.calculated_at = CURRENT_TIMESTAMP
```

**Notes** :  
• Weight is not set manually  
• Weight is derivative of action history

**System** :  
• does not impose truth  
• allows parallel contradictions  
• measures not "who is right", but "how stable is opinion"

#### 2.6.3  event_ci

##### Table: event_ci

📝 **System-level** table of the Collective Intelligence Layer  
It is **not accept direct participant input**, and is **not transmitted over the network**  

**Purpose** :  
Each record in "event_ci" represents a **collective intelligence state** associated with a single logical event "truth_event" and serves as the **intersection point of two orthogonal axes** for participant assessment :  
• **classification** using "event_type" field ENUM ("impact", "judgment", "both")  
• tracking **event status** ENUM ("active", "resolved", "archived")  
• storing **event result** data in "resolution_data" field ENUM ("unstable", "suppose", "consent")  
❗ "event_ci" represents a system-level **event neuron**  
**Fields** :
```
id              (INTEGER, PK, AUTOINCREMENT) — unique event identifier
created_by      (INTEGER, NOT NULL) — FK to truth_event.id
event_type      (TEXT, NOT NULL, DEFAULT 'judgment') — type of event
status          (TEXT, NOT NULL, DEFAULT 'active') — event status
old_status      (TEXT, NOT NULL, DEFAULT 'active') — event old status
resolution_data (TEXT, NOT NULL, DEFAULT 'unstable') — data about event resolution
created_at      (INTEGER, NOT NULL) — timestamp of creation
```
🏠 Database: truth_training.sqlite  

##### Model : Collective Intelligence Event Aggregation

 • "event_ci" is created **automatically** when an **event** is created in the "truth_event" table

###### **Default values** :
```sql
event_ci.created_by = truth_event.id
event_ci.created_at = CURRENT_TIMESTAMP
event_ci.event_type = DEFAULT "judgment"
event_ci.status = DEFAULT 'active'
event_ci.resolution_data = DEFAULT 'unstable'
```
• **All further changes are computed asynchronously by Core logic**

###### **"event_type" Definition Model** :  
 • "event_type" reflects which **orthogonal assessment axes** are currently active and is **recomputed** after each **impact** or **judgment** update  
 • It is formed based on the data in the "impact_metrics" and "judgment_weights" tables

**Source tables** :  
 • "impact_metrics"  
 • "judgment_weights"

**Axis presence model** :
```sql
impact_present =
EXISTS (
SELECT 1 FROM impact_metrics
WHERE impact_metrics.event_id = event_ci.id
AND impact_metrics.total_magnitude IS NOT NULL
)

judgment_present =
EXISTS (
SELECT 1 FROM judgment_weights
WHERE judgment_weights.event_id = event_ci.id
AND judgment_weights.weight IS NOT NULL
)
```
**Rules** :
```sql
IF impact_present AND judgment_present
event_ci.event_type = 'both'

IF impact_present AND NOT judgment_present
event_ci.event_type = 'impact'

IF judgment_present AND NOT impact_present
event_ci.event_type = 'judgment'
```
**Notes** :  
 • "event_type" is a computed system state  
 • values indicate axis activity, not semantic classification  
 • only "both" allows full orthogonal convergence  

###### **"status" Definition Model** :  
 • status represents the temporal phase of the **event neuron** and is evaluated based on the associated **timeline** of the source **event**

**Time source** :
```sql
time_start =
SELECT event_timeline.time_start
FROM truth_event
JOIN event_timeline ON event_timeline.id = truth_event.timeline_id
WHERE truth_event.id = event_ci.created_by

time_end =
SELECT event_timeline.time_end
FROM truth_event
JOIN event_timeline ON event_timeline.id = truth_event.timeline_id
WHERE truth_event.id = event_ci.created_by
```
**Rules** :  
```sql
IF time_end IS NULL
event_ci.status = 'active'
ELSE
    IF time_end >= CURRENT_TIMESTAMP
    event_ci.status = 'resolved'
    ELSE
    event_ci.status = 'archived'
    ENDIF
ENDIF
```
**Interpretation** :  
 • "active" — signal accumulation  
 • "resolved" — **prediction** and evaluation based on accumulated signals  
 • "archived" — evaluation based on factual results

###### **"resolution_data" Orthogonal Convergence Model** :  
 • resolution_data reflects the degree of convergence between active axes and is evaluated only when "event_ci.event_type" and "event_ci.status" are known

**Preconditions** :
```sql
event_ci.event_type IN ('impact','judgment','both')
event_ci.status IN ('active','resolved','archived')
```

**Source tables** :  
 • "impact_metrics"  
 • "judgment_weights"

**Axis convergence model** :
```sql
impact_converged =
event_ci.event_type IN ('impact','both')
AND EXISTS (
SELECT 1 FROM impact_metrics
WHERE impact_metrics.event_id = event_ci.id
AND (positive_ratio IS NOT NULL
 OR negative_ratio IS NOT NULL
 OR uncertainty IS NOT NULL)
)

judgment_converged =
event_ci.event_type IN ('judgment','both')
AND EXISTS (
SELECT 1 FROM judgment_weights
WHERE judgment_weights.event_id = event_ci.id
AND judgment_weights.weight IS NOT NULL
)
```
**Rules** :
```sql
IF NOT impact_converged AND NOT judgment_converged
event_ci.resolution_data = 'unstable'

IF (impact_converged XOR judgment_converged)
event_ci.resolution_data = 'suppose'

IF impact_converged AND judgment_converged
AND event_ci.event_type = 'both'
AND (event_ci.status = 'resolved' OR event_ci.status = 'archived')
event_ci.resolution_data = 'consent'
```
**Key constraints** :  
• "resolution_data" does NOT affect status  
• consent is impossible without **both axes**

######  Notes :  
• **event** are created by **participants**  
• **Participants** never interact with **event** directly. They interact only with **judgment** (truth axis), **impact** (consequence axis)  
• **Consensus** is calculated in the "consensus_ci" table  
• **Judgment Axis** (truth axis) is subjective and dynamic  
• **event** are not exposed directly to **participants**. All **participant** interaction with **event context** occurs indirectly through **judgment** and **impact** projections  
• "event_type" is an internal semantic classifier used exclusively 
by the **Core** logic to select processing rules and heuristics

**"event_type" defines which orthogonal axes are currently active** :  
**"impact"**  
**Consequence** :  
• **event** are recorded  
• **judgment** are permitted, but not mandatory  
• **trust weights** slowly  
• **event** is unstable  
**Used for** :  
• technical **impact**  
• physical **consequences**  
• **infrastructural** changes

**"judgment"**  
**Consequence** :  
• **truth** heuristics are active  
• **impact** may be absent  
• high noise **weights** is allowed  
• the system awaits **confirmation** by reality  
**Used for** :  
• **rumors**  
• **hypotheses**  
• **gossip**

**"both"**  
**Consequence** :  
• **both axes** are active  
• **collective stabilization** is activated  
• **trust weights** are strengthened  
• the **event** can be **recorded as stable**  
📌 Only this "state" allows for the **consent** of the **event** "status" defines the temporal phase of the **event** :  
 • "resolved" - stable convergence of axes  
 • "archived" - frozen historical state

• An **event** may be **resolved** only when **both axes** converge

**"resolution_data" contains the final state of the event** :  
• "unstable" - no resolved  
• "suppose" - partial resolved  
• "consent" - full resolved

**Key properties** :  
• **Truth** is formed collectively  
• **Truth** can change over time  
• High **confidence** ≠ high **truth**  
• **Consensus** does not guarantee correspondence to reality

######  Semantic Summary

✔ "event_type" — axis activity state  
✔ status — temporal phase of learning  
✔ "resolution_data" — orthogonal convergence result  
✔ **Truth** is not asserted — it is **trained**  
✔ **System learns through consequence, not belief**

### 2.7 Aggregated System Metrics and Expert Functions

#### Table: progress_metrics

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Aggregates system-wide progress metrics (event counts and reaction totals) to track overall system performance and evolution over time

**Fields** :
```
id                           (INTEGER, PK, AUTOINCREMENT) — unique metric record identifier
total_events                 (INTEGER, NOT NULL) — total number of events processed
total_events_group           (INTEGER, NOT NULL) — total number of group events
total_positive_impacts       (REAL, NOT NULL) — total positive impacts observed
total_positive_impacts_group (REAL, NOT NULL) — positive impacts in group events
total_negative_impacts       (REAL, NOT NULL) — total negative impacts observed
total_negative_impact_group  (REAL, NOT NULL) — negative impacts in group events
trend                        (REAL, NOT NULL) — overall trend metric
trend_group                  (REAL, NOT NULL) — trend metric for group events
last_updated                 (INTEGER, NOT NULL) — timestamp of last update metric
```
🏠 Database: truth_training.sqlite  

##### Model: System Progress Tracking

**Progress Metrics Model** :
```
M_system = (M_individual, M_group, M_trend)
```

**Trend Calculation** :
```
Trend = (Σ P - Σ N) / total_events
```
**Where** :  
- P — "positive" **impacts**  
- N — "negative" **impacts**

**Impact Balance** :
```
Balance = total_positive_impacts - total_negative_impacts
```

**Group vs Individual Comparison** :
```sql
IF total_events_group / total_events > threshold
    system_efficiency = HIGH (group collaboration effective)
ELSE
    system_efficiency = LOW (individual assessment dominant)
```

**Progress Update Rules** :
```sql
IF new_event_processed
    total_events = total_events + 1
    IF event.is_group_event
        total_events_group = total_events_group + 1
    update_impact_metrics()
    recalculate_trends()
    last_updated = CURRENT_TIMESTAMP
```

**Trend Interpretation** :  
• Positive trend → system improving, truth convergence  
• Negative trend → system degrading, truth divergence  
• Near-zero trend → system stable, truth stable

**Notes** :  
• Tracks overall system health and performance  
• Enables early detection of system degradation  
• Supports system optimization decisions

#### Event movement dynamics

Event can move across plane over time :
```
E(t₀) ≠ E(t₁)
```
**Causes - event re-assessment** :  
- new **judgment**  
- new **impact**

**Truth**(E, t) and **impact**(E, t) are updated independently

**Truth** can stabilize earlier or later than **consequences**  
• "True" **event** without consequences → **historical fact**  
• "False" **event** with consequences → **social catastrophe**  
• "Undefined" **event** → active system learning zone

**System amplifies attention to event** :
```
attention(E) ∝ |∂I/∂t| × uncertainty(T)
```

**Where** :  
|∂I/∂t| — absolute value of rate of change of **impact assessment** over **time**  

#### Neural network analogy

- **Truth axis** → **confidence weight**  
- **impact axis** → **activation output**  
- **Event** → **neuron**  
- **Event graph** → **neural network**

**System** :  
• **does not delete event**  
• **does not suppress axes**
• allows coexistence of **truth** and **lies**

#### Expert Functions and Assessment Heuristics

**Truth Training** does not use centralized expert system  
Expert role is performed by network **participants**, and their assessments form distributed interpretation field

**Expert function F is mapping** :
```
F: (E, C) → J
```
**where** :  
- E — **event**  
- C — **context**  
- J — **judgment**

**Expert function can be** :  
- human  
- algorithmic  
- hybrid

**Heuristic** — is approximate rule used when complete information is absent  
**In system** :  
• **heuristics** are not considered **truth** 
• they are considered contribution to **collective assessment**

**Heuristic types** :  
- logical  
- statistical  
- empirical  
- contextual  
- domain

#### Table: judgment_heuristics

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Linking judgment to applied heuristics to track which heuristics influenced specific judgments

**Fields** :
```
id           (INTEGER, PK, AUTOINCREMENT) — unique record identifier
judgment_id  (INTEGER, NOT NULL) — FK → judgment.id
heuristic_id (INTEGER, NOT NULL) — FK → expert_heuristics.id
influence    (REAL, NOT NULL) — influence or impact of the heuristic on the judgment
created_at   (INTEGER, NOT NULL) — timestamp of creation
```
🏠 Database: truth_training.sqlite  

##### Model: Heuristic Application

**Heuristic Influence Model** :
```
H(E, C) = Σ (influenceᵢ)
```
**Where** :  
- H(E, C) — **heuristic** function for **event** E in **context** C
- influenceᵢ — influence value of **heuristic i** on **judgment**

**Expert Function Mapping** :
```
F: (E, C) → J
```
Where:
- E — **event**
- C — **context**
- J — **judgment**

**Heuristic Types** :  
• Logical: formal logical rules  
• Statistical: probability-based rules  
• Empirical: experience-based rules  
• Contextual: context-dependent rules  
• Domain: specialized knowledge rules

**Heuristic Application Rules** :
```sql
IF expert_heuristics.confidence > threshold
    THEN apply_heuristic()
ELSE
    weight = reduced_weight
```

**Conflict Resolution** :
```sql
IF conflicting_heuristics_detected
    THEN uncertainty = TRUE
    ELSE uncertainty = FALSE
```

**Notes** :  
Final **event** assessment is aggregated function of all applied **heuristics** and **judgment**

**Expert function** :  
• is **not** source of **truth**  
• does not have priority over **collective assessment**  
• Conflict of **heuristics** is admissible and recorded in system as state of uncertainty  
• Expertise — is contribution, not authority

#### Table: expert_heuristics

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Storing descriptions of heuristics and expert rules for consistent application across the system

**Fields** :
```
id              (INTEGER, PK, AUTOINCREMENT) — unique heuristic identifier
name            (TEXT, NOT NULL) — name of the heuristic
description     (TEXT, NOT NULL) — detailed description of the heuristic
domain          (TEXT, NOT NULL) — domain or context where heuristic applies
weight          (REAL, NOT NULL) — current weight/importance of the heuristic
confidence      (REAL, NOT NULL) — confidence level of the heuristic
proven_accuracy (REAL, NOT NULL) — proven accuracy rate of the heuristic
created_at      (INTEGER, NOT NULL) — timestamp of creation
updated_at      (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: truth_training.sqlite

##### Model: Expert Heuristic Storage

**Heuristic Storage Model** :
```
Hᵢ = {nameᵢ, descriptionᵢ, domainᵢ, weightᵢ}
```

**Heuristic Weight Model** :
```
wᵢ = f(accuracyᵢ, reliabilityᵢ, domain_relevanceᵢ)
```

**Domain Classification** :  
• "logic" — formal logic rules  
• "statistical" — statistical inference rules  
• "empirical" — experience-based rules  
• "contextual" — context-sensitive rules  
• "domain_specific" — specialized knowledge rules  

**Weight Calculation Rules** :
```sql
IF expert_heuristics.proven_accuracy > 0.8
    weight = HIGH
ELSE IF expert_heuristics.proven_accuracy > 0.6
    weight = MEDIUM
ELSE
    weight = LOW
```

**Notes** :  
• **Heuristics** are stored separately from their applications  
• **Weights** may be updated based on accuracy feedback  
• **Domain** classification enables targeted **heuristic** selection  
• **Heuristic** is not applied directly to **event**, but through **judgment**

**Model Aggregated System Metrics and Expert Functions** :  
**For detailed SQL implementation see** 👇 [model_core_aggregated_metrics.md](model_core_aggregated_metrics.md)

## 3 Temporal Dynamics and Truth Evolution

• **Event** in **Truth Training** are **not static objects**  
• Each **event** exists in time and changes its position in **Judgment × impact** space as new data arrives

### 3.1 Table: time_axes

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**  
• This **table** are **populated** with reference information by the **initialization function** in the application **core module** core/src/storage.rs
• Consists of three records.  
• Default values for time axes are specified in: [Knowledge Base Table Values for Default Seeding](../spec/26-seed_knowledge_base_table_value.md)

**Purpose** :  
**Defines** independent time **scales** ('past','present','future') for **analysis**  
**Fields** :
```
id          (INTEGER, PK, AUTOINCREMENT) — unique time axis identifier
description (TEXT, NOT NULL) — description of the time axis
time_type   (TEXT, NOT NULL) — type of time ('past' / 'present' / 'future')
created_at  (INTEGER, NOT NULL) — timestamp of creation
```
🏠 Database: truth_training.sqlite  

##### Model: Time Axis Classification

**Time Axis Model** :
```
T = {T_p, T_pr, T_f}
```
**Where** :  
- T_p — **past time** (**historical** chronological time)  
- T_pr — **present time** (**current** real-time)  
- T_f — **future time** (**predicted** or scheduled time)

**Temporal Dynamics** :
```
Past time ≠ Present time ≠ Future time
```

**Time Type Rules** :
```sql
IF time_type = 'past'
    time_scale = historical record
    time_flow = fixed
    
IF time_type = 'present'
    time_scale = current timestamp
    time_flow = real-time
    
IF time_type = 'future'
    time_scale = scheduled/planned time
    time_flow = predictive
```

**Notes** :  
• time_type its ENUM ("past" / "present" / "future")  
• **Past time** ≠ **present time**  
• **Future time** represents planned/scheduled **events**  

**Temporal Integration** :  
• **Events** are **timestamped according** to their associated **time axis**  
• Different **time axes** may be **used** for different types of **analysis**  
• **Time axis** selection **affects** how **event relationships** are interpreted

### 3.2 Table: event_timeline

**Event** is defined as temporal process :
```
E(t) = { T(J(E, t)), I(E, t) }
```
**Where** :  
- t — discrete or continuous **time**  
- T(J(E, t)) — **event truth** in **time**  
- J(E, t) — **event judgment** in **time**  
- I(E, t) — **event impact** in **time**  
• System must preserve entire **history of changes**, **not overwriting** previous **event states**
**Constraints time** :  
**Time** boundaries determined by following **rules** :  
- "t_start" - non-empty value, for **new event** can have any value, for **existing event** cannot be changed  
- "t_end" - for **new event** can be undefined (empty value NULL), cannot be less than "t_start", for **existing event** can be changed and if date was already set automatically flag is set for field "corrected" in table "truth_event"

📝 **User-level** table of the Collective Intelligence Layer
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
Records the **time range** of **events** on each **time axis**  
**Fields** :
```
id           (INTEGER, PK, AUTOINCREMENT) — unique timeline record ID
time_axis_id (INTEGER, NOT NULL) — FK → time_axes.id — time axis reference
t_start      (INTEGER, NOT NULL) — event start time on this axis
t_end        (INTEGER) — event end time on this axis (if set)
```
🏠 Database: truth_training.sqlite  

##### Model: Event Timeline Management

**Timeline Model** :
```
Timeline(E) = {t_start, t_end}
```

**Timeline Constraints** :
```sql
IF t_end IS NULL
    event_status = 'active'
ELSE
    IF t_end >= CURRENT_TIMESTAMP
        event_status = 'active'
    ELSE
        event_status = 'completed'
```

**Timeline Validation Rules** :
```sql
IF t_start > t_end AND t_end IS NOT NULL
    ERROR "Timeline start cannot be after end"
    
IF t_start > CURRENT_TIMESTAMP
    event_type = 'scheduled'
ELSE
    event_type = 'active'
```

**Notes** :  
• An **event** cannot be repeated in time without reaching **consensus**  
• An **event** is identified as identical through the field "relation_type" = "equal" in table "event_links"

### 3.3 Table: impact_timeline

📝 **User-level** table of the Collective Intelligence Layer
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
Records the time range of **impact** on each **time axis**  
**Fields** :
```
id           (INTEGER, PK, AUTOINCREMENT) — unique timeline record ID
time_axis_id (INTEGER, NOT NULL) — FK → time_axes.id — time axis reference
t_start      (INTEGER, NOT NULL) — impact start time on this axis
t_end        (INTEGER) — impact end time on this axis (if set)
```
🏠 Database: truth_training.sqlite  

##### Model: Impact Timeline Dynamics

**Impact Timeline Model** :
```
Timeline(I) = {t_start, t_end}
```

**Impact Duration** :
```
Duration = t_end - t_start (if t_end IS NOT NULL)
Duration = CURRENT_TIMESTAMP - t_start (if t_end IS NULL)
```

**Timeline Validation Rules** :
```sql
IF t_start > t_end AND t_end IS NOT NULL
    ERROR "Impact timeline start cannot be after end"
```

**Notes** :  
• A single value cannot have multiple states  
• Changing the value does not delete the history  

### 3.4 Table: judgment_timeline

📝 **User-level** table of the Collective Intelligence Layer
It is **direct editing for participant input**, and is **not transmitted over the network**

**Purpose** :  
Records the **time range** of **judgment** on each **time axis**  
**Fields** :
```
id           (INTEGER, PK, AUTOINCREMENT) — unique timeline record ID
time_axis_id (INTEGER, NOT NULL) — FK → time_axes.id — time axis reference
t_start      (INTEGER, NOT NULL) — judgment start time on this axis
t_end        (INTEGER) — judgment end time on this axis (if set)
```
🏠 Database: truth_training.sqlite  

##### Model: Judgment Timeline Dynamics

**Judgment Timeline Model** :
```
Timeline(J) = {t_start, t_end}
```

**Judgment Duration** :
```
Duration = t_end - t_start (if t_end IS NOT NULL)
Duration = CURRENT_TIMESTAMP - t_start (if t_end IS NULL)
```

**Timeline Validation Rules** :
```sql
IF t_start > t_end AND t_end IS NOT NULL
    ERROR "Judgment timeline start cannot be after end"
```

**Notes** :  
• A single value cannot have multiple states  
• Changing the value does not delete the history  

### 3.5 Table: truth_state

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Stores aggregated **truth state** of an **event** at a given **point in time**  
**Fields** :
```
id            (INTEGER, PK, AUTOINCREMENT) — unique record identifier
event_id      (INTEGER, NOT NULL) — FK → event_ci.id — event reference
time_axis_id  (INTEGER, NOT NULL) — FK → time_axes.id — time axis reference
truth_state   (TEXT) — ENUM (active / resolved / archived)
truth_score   (REAL, NOT NULL) — aggregated truth score
dispersion    (REAL, NOT NULL) — dispersion in the truth score
confidence    (REAL, NOT NULL) — confidence in the truth score
calculated_at (INTEGER, NOT NULL) — timestamp of calculation
```
🏠 Database: truth_training.sqlite  

##### Model: Truth State Evolution

**Truth State Model** :
```
Truth(E, t) = {state, score, confidence, dispersion}
```

• In **Truth Training**, **truth** is not determined by single act.
• It arises as result of aggregation of independent assessments in distributed system of observers.
• **Collective truth** — is not value, but distribution of assessments in **judgment** space

**Truth is represented** :  
• as vector ("truth_state","truth_score")  
• as density ("dispersion")  
• as dynamic state ("confidence")  

**Let** :
```
J = {j₁, j₂, ..., jₙ} — set of judgment of event E
```
**Then aggregated assessment T(E)** :
```
T(E) = A(J)
```
**where** :  
"A" — aggregating function.

**Possible aggregators** :  
• weighted average  
• median  
• quantile distribution  
• bayesian aggregation  
• neuron-like function  

Each **judgment** has **weight** w, depending on :
• node trust  
• consistency with other assessments  
• time  
• applied heuristics

**State Transition Rules** :
```sql
IF confidence > threshold AND |dispersion| < dispersion_limit
    truth_state = 'resolved'
ELSE IF calculated_at < (CURRENT_TIMESTAMP - stability_period)
    truth_state = 'archived'
ELSE
    truth_state = 'active'
```

**Notes** :  
- "truth_score" ≠ **verdict**  
- confidence reflects assessment stability

Polarization is recorded when assessments form multiple clusters  
**Truth** — is not result  
**Truth** — is process of alignment

### 3.6 Table: event_state_history

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Records **snapshots** of an **event state** over **time** to track how **event** assessments evolve

Use this to **trace** how an **event judgment** and **impact metrics** evolve

❗ **Truth** has a temporal dynamic
• **Over long periods** of **inactivity** (without new **judgments** or **impacts**), confidence in the **event** fades, and estimates become outdated. A **decay function** is introduced :  

IF "λT" and "λI" fall below the "small_constants" threshold, specifically "εT" and "εI" (where "small_constants" is implemented as the SQL expression :
```sql
CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END)
```
the **event** is **stable** in terms of **truth or impact**.
```
w(t) = w₀ * e^(-λt)
```
meaning an event's stored influence diminishes without fresh evidence

**Fields** :
```
id             (INTEGER, PK, AUTOINCREMENT) — unique metric record identifier
event_id       (INTEGER, NOT NULL) — FK → event_ci.id
judgment_count (INTEGER, NOT NULL) — number of judgments recorded so far
truth_score    (REAL, NOT NULL) — truth at this time
impact_count   (INTEGER, NOT NULL) — number of impacts recorded so far
impact_score   (REAL, NOT NULL) — impact at this time
recorded_at    (INTEGER, NOT NULL) — timestamp of recorded
```
🏠 Database: truth_training.sqlite  

##### Model: Event State History Tracking

**State History Model** :
```
H(E, t) = {judgment_count, truth_score, impact_count, impact_score}
```

**State Evolution Function** :
```
ΔH = H(t₂) - H(t₁)
```

**Stability Detection** :
```
IF |∂T/∂t| < ε_T AND |∂I/∂t| < εᵢ
    event_state = 'stable'
```
**Where** :  
- ∂T/∂t is the rate of change of **truth assessment** over **time**  
- ∂I/∂t is the rate of change of **impact assessment** over **time**  
- ε_T and ε_I are threshold values for stability determination

**History Aggregation Rules** :
```sql
IF new_judgment_recorded
    judgment_count = judgment_count + 1
    recalculate_truth_score()
    recorded_at = CURRENT_TIMESTAMP
    
IF new_impact_recorded
    impact_count = impact_count + 1
    recalculate_impact_score()
    recorded_at = CURRENT_TIMESTAMP
```

**Impact(E, t)** :  
• can be monotonic (accumulative)  
• can be impulsive  
• can decay

**Truth(E, t)** :  
• not required to be monotonic  
• allows revision  

Between event appearance and its assessment there is temporal lag :
```
Δt_truth ≠ Δt_impact
```
**Reasons** :  
• observation delay  
• social propagation  
• cognitive inertia

**Event **is considered stabilized if :
```
|∂T/∂t| < ε_T
|∂I/∂t| < ε_I
```
**Where** :  
ε_T, ε_I — stabilization thresholds derived from "small_constants" using SQL implementation :
```sql
CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END)
```
|∂T/∂t| — absolute value of rate of change of **truth assessment** over **time**
|∂I/∂t| — absolute value of rate of change of **impact assessment** over **time**

**Notes** :  
• Maintains complete **history** of **event** state changes  
• Enables temporal analysis of **event evolution**  
• Supports stability detection algorithms

### 3.7 Table: event_stability

📝 **System-level** table of the Collective Intelligence Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
• **Records** when an **event** has become **stable** in **truth and/or impact**  
• This **helps flag** **factors** historical resolved misinformation that **no longer require active monitoring**  
• Even when stable, events can be "reactivated" by new **judgments** or **impacts**, at which point entries in history or stability may be updated

**Fields** :
```
id            (INTEGER, PK, AUTOINCREMENT) — unique record identifier
event_id      (INTEGER, NOT NULL) — FK → event_ci.id
truth_stable  (INTEGER) — BOOLEAN 0/1  — true if truth is stabilized.
impact_stable (INTEGER) — BOOLEAN 0/1  — true if impact is stabilized.
stabilized_at (INTEGER, NOT NULL) — when stabilization was detected.
```
🏠 Database: truth_training.sqlite  

##### Model: Event Stability Detection

**Stability Model** :
```
Stable(E) = {truth_stable, impact_stable, stabilized_at}
```

**Stability Conditions** :
```
IF |∂T/∂t| < ε_T AND confidence > min_confidence
    truth_stable = TRUE
    
IF |∂I/∂t| < ε_I AND impact_significance > min_significance
    impact_stable = TRUE
```
**Where** :  
ε_T = "small_constants" using SQL implementation : 
```sql
CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END)
```
 — truth stability threshold  
ε_I = "small_constants" using SQL implementation :
```sql
CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END) 
```
— impact stability threshold  
|∂T/∂t| — absolute value of rate of change of **truth assessment** over **time**  
|∂I/∂t| — absolute value of rate of change of **impact assessment** over **time**


**Reactivation Rules** :
```sql
IF new_judgment_arrives AND truth_stable = TRUE
    truth_stable = FALSE
    stabilized_at = NULL
    
IF new_impact_arrives AND impact_stable = TRUE
    impact_stable = FALSE
    stabilized_at = NULL
```

**Decay Function** :
```
Decay(T, t) ∝ e^(−λt)
```

**Notes** :  
• Tracks when events reach stable states  
• Enables efficient resource allocation by focusing on unstable events  
• Supports reactivation when new evidence emerges

## 4 Node Discovery and Network Tables

**Note** :  
• The "Node Discovery" tables participate indirectly in P2P information exchange. For details on the relationship between these tables and P2P exchange implementation, see [docs/p2p_release.md](p2p_release.md) which describes how these tables are used in the implementation of P2P information exchange.  
• Additionally, for Android-specific implementation details of the Node Discovery system, see [docs/android_discovery_architecture.md](android_discovery_architecture.md) which describes the architecture of the Node Discovery system for the Android mobile application

**Note on table naming**: In version v1.1.0, the tables were renamed for clarity :  
- "nodes" → "discovery_nodes"  
- "node_discovery" → "discovery_history"  
- "node_metrics" → "node_performance"  
- "sync_log" → "sync_operations"  
- "sync_logs" → "sync_attempts"  
- "peer_history" → "peer_synchronization"

### Table: discovery_nodes

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **transmitted over the network**

**Purpose** :  
Storing information about **discovered nodes** in **Truth Training** network for tracking **peer nodes**, **their addresses**, **types**, availability and other **discovery metadata**

**Fields**:
```
id         (INTEGER, PK, AUTOINCREMENT) — unique node identifier
address    (TEXT, NOT NULL, UNIQUE) — URL or ip:port of node (e.g. http://192.168.1.100:8080/api/v1)
type       (TEXT, NOT NULL) — node type (LAN, WIFI, GLOBAL, RELAY, CLIENT)
reachable  (INTEGER, NOT NULL) — availability flag (0 = down, 1 = up)
last_seen  (INTEGER, NOT NULL) — time of last successful contact
ttl        (INTEGER, NOT NULL) — record lifetime before automatic deletion
source     (TEXT) — source of node discovery
node_id    (TEXT, NOT NULL) — FK → participants.public_key  — node's public key
created_at (INTEGER, NOT NULL) — timestamp of record creation
updated_at (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: discovery_nodes.sqlite

#### Model: Node Discovery and Management

**Node Discovery Model** :
```
N = {address, type, public_key, availability}
```

**Node Lifecycle** :
```
IF last_seen < (CURRENT_TIMESTAMP - ttl)
    node_status = 'expired'
    eligible_for_cleanup = TRUE
    
IF reachable = 0 AND consecutive_failures > threshold
    node_status = 'unreachable'
```

**Node Type Classification** :  
• LAN — Local Area Network **nodes** (typically 192.168.x.x or 10.x.x.x)  
• WIFI — Wireless network **nodes**  
• GLOBAL — Public internet **nodes**  
• RELAY — Relay **nodes** that forward traffic  
• CLIENT — End-user client **nodes**

**Node Validation Rules**:
```
IF address NOT valid_url_format
    ERROR "Invalid address format"
    
IF type NOT IN (LAN, WIFI, GLOBAL, RELAY, CLIENT)
    ERROR "Invalid node type"
    
IF node_id NOT IN participants.public_key
    ERROR "Node ID not registered as participant"
```
**Notes** :  
• **Node** addresses are validated for proper URL format  
• TTL ensures stale **node** records are cleaned up  
• **Node discovery** supports multiple network types

**SQL Implementation Example** :
```sql
-- Create discovery_nodes table
CREATE TABLE discovery_nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,
    reachable INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    source TEXT,
    node_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES participants(public_key)
);

-- Insert new node
INSERT INTO discovery_nodes (address, type, reachable, last_seen, ttl, source, node_id, created_at, updated_at)
VALUES (?, ?, 1, ?, 3600, ?, ?, ?, ?);

-- Update node reachability
UPDATE discovery_nodes
SET reachable = CASE
    WHEN last_seen > (strftime('%s', 'now') - 300) THEN 1 -- 5 minutes
    ELSE 0
END,
last_seen = strftime('%s', 'now')
WHERE id = ?;

-- Get active nodes
SELECT * FROM discovery_nodes
WHERE last_seen > (strftime('%s', 'now') - ttl)
AND reachable = 1;
```

### Table: discovery_history

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
**Tracking changes** in network **node discovery** for **auditing and analyzing** changes in **node discovery**, understanding reasons for **discovery changes**, analyzing **node behavior** and **availability effectiveness**, and **ensuring transparency** of **discovery system**

**Fields** :
```
id             (INTEGER, PK, AUTOINCREMENT) — unique discovery record identifier
node_id        (INTEGER, NOT NULL) — FK → discovery_nodes.id
discovery_type (TEXT, NOT NULL) — type of discovery (beacon, manual, api)
discovered_at  (INTEGER, NOT NULL) — timestamp of discovery
ttl            (INTEGER, NOT NULL) — time to live for discovery record
status         (TEXT, NOT NULL) — discovery status (active, expired, unreachable)
source         (TEXT, NOT NULL) — source of discovery information
```
🏠 Database: discovery_nodes.sqlite  

**Model "discovery_history"** :  
**Source relation**
```
discovery_history.node_id = discovery_nodes.id
```
**Base node mapping**
```
base_node_id =
SELECT discovery_nodes.id
FROM discovery_nodes
WHERE discovery_nodes.id = discovery_history.node_id
```
**Aggregation formulas "discovered_at"**
```
discovery_history.discovered_at = (
    SELECT discovery_nodes.created_at
    FROM discovery_nodes
    WHERE discovery_nodes.id = base_node_id
)
```
**Aggregation formulas "ttl"**
```
discovery_history.ttl = (
    SELECT 3600  -- Default 1 hour
    FROM discovery_nodes
    WHERE discovery_nodes.id = base_node_id
)
```
**Aggregation formulas "status"**
```
discovery_history.status = (
    SELECT CASE
        WHEN discovered_at > (CURRENT_TIMESTAMP - ttl) THEN 'active'
        ELSE 'expired'
    END
)
```
**Aggregation formulas "source"**
```
discovery_history.source = (
    SELECT discovery_nodes.source
    FROM discovery_nodes
    WHERE discovery_nodes.id = base_node_id
)
```

**SQL Implementation Example** :
```sql
-- Create discovery_history table
CREATE TABLE discovery_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    node_id INTEGER NOT NULL,
    discovery_type TEXT NOT NULL,
    discovered_at INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    status TEXT NOT NULL,
    source TEXT NOT NULL,
    FOREIGN KEY (node_id) REFERENCES discovery_nodes(id)
);

-- Insert new discovery record
INSERT INTO discovery_history (node_id, discovery_type, discovered_at, ttl, status, source)
VALUES (?, ?, ?, 3600, 'active', ?);

-- Update discovery status based on TTL
UPDATE discovery_history
SET status = CASE
    WHEN discovered_at > (strftime('%s', 'now') - ttl) THEN 'active'
    ELSE 'expired'
END
WHERE id = ?;

-- Get active discoveries
SELECT dh.*, dn.address, dn.type
FROM discovery_history dh
JOIN discovery_nodes dn ON dh.node_id = dn.id
WHERE dh.status = 'active'
AND dh.discovered_at > (strftime('%s', 'now') - dh.ttl);

-- Clean up expired discoveries
DELETE FROM discovery_history
WHERE discovered_at < (strftime('%s', 'now') - ttl);
```

##### Model: Node Discovery Tracking

**Discovery Change Model** :
```
ΔD = D_new - D_old
```

**Discovery Types** :  
• "beacon" — discovery through UDP beacons  
• "manual" — manually added **nodes**  
• "api" — discovery through API calls

**Discovery Rules** :
```
IF beacon_received
    discovery_type = "beacon"
    discovered_at = CURRENT_TIMESTAMP
    ttl = 3600  -- 1 hour

IF manual_addition
    discovery_type = "manual"
    discovered_at = CURRENT_TIMESTAMP
    ttl = 86400  -- 24 hours
```

**Discovery Analysis** :  
• Trend analysis for **node discovery**  
• Detection of **discovery manipulation attempts**  
• Verification of **discovery evolution consistency**  
• Audit trail for **discovery changes**

**Key constraints** :  
• "discovered_at" and "ttl" must be valid timestamps  
• "discovery_type" must be one of predefined values  
• "status" must be current based on "ttl"

**Notes** :  
• Used for auditing and transparency of **discovery changes**  
• Tracks **historical changes** for **analysis**  
• Enables **detection** of **discovery manipulation**  
• Supports **discovery trend analysis**


### Table: node_ratings

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Storing **node reputation** and trust for evaluating **node reliability** based on their **activity** and **assessment accuracy**

**Fields**:
```
node_id              (TEXT, NOT NULL) — FK → discovery_nodes.node_id — unique node identifier (public key)
events_true          (INTEGER, NOT NULL, DEFAULT 0) — number of true event
events_false         (INTEGER, NOT NULL, DEFAULT 0) — number of false event
validations          (INTEGER, NOT NULL, DEFAULT 0) — number of confirmations
reused_events        (INTEGER, NOT NULL, DEFAULT 0) — number of reused event
trust_score          (REAL, NOT NULL, DEFAULT 0.0) — overall trust rating (-1.0 .. 1.0)
propagation_priority (REAL, NOT NULL, DEFAULT 0.0) — distribution priority (0.0 .. 1.0)
last_updated         (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: discovery_nodes.sqlite  

#### Model: Node Reputation and Trust

**Trust Score Model** :
```
Trust(n) = (events_true - events_false) / (events_true + events_false + ε)
```

**Trust Calculation** :
```
IF events_true + events_false = 0
    trust_score = 0.0  (neutral trust)
ELSE
    trust_score = (events_true - events_false) / (events_true + events_false)
```

**Priority Calculation** :
```
Priority(n) = f(trust_score, validation_count, reuse_frequency)
```

**Rating Update Rules** :
```
IF new_validation_received
    IF validation_correct
        events_true = events_true + 1
    ELSE
        events_false = events_false + 1
    recalculate_trust_score()
    last_updated = CURRENT_TIMESTAMP
```
**Notes** :  
• **Trust scores** range from -1.0 (completely untrustworthy) to +1.0 (completely trustworthy)  
• **Neutral trust** is represented by 0.0  
• **Propagation priority** is derived from **trust** and **activity metrics**

**SQL Implementation Example** :
```sql
-- Create node_ratings table
CREATE TABLE node_ratings (
    node_id TEXT NOT NULL,
    events_true INTEGER NOT NULL DEFAULT 0,
    events_false INTEGER NOT NULL DEFAULT 0,
    validations INTEGER NOT NULL DEFAULT 0,
    reused_events INTEGER NOT NULL DEFAULT 0,
    trust_score REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    last_updated INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES discovery_nodes(node_id)
);

-- Insert or update node rating
INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, propagation_priority, last_updated)
VALUES (?, 0, 0, 0, 0, 0.0, 0.0, strftime('%s', 'now'))
ON CONFLICT(node_id) DO UPDATE SET
    last_updated = strftime('%s', 'now');

-- Update trust score based on events
UPDATE node_ratings
SET
    trust_score = CASE
        WHEN events_true + events_false > 0 THEN (events_true - events_false) * 1.0 / (events_true + events_false)
        ELSE 0.0
    END,
    propagation_priority = (CASE
        WHEN events_true + events_false > 0 THEN (events_true - events_false) * 1.0 / (events_true + events_false)
        ELSE 0.0
    END) * 0.7 + (validations * 0.3 / (SELECT MAX(validations + 1) FROM node_ratings)),
    last_updated = strftime('%s', 'now')
WHERE node_id = ?;

-- Get top rated nodes
SELECT n.address, nr.trust_score, nr.propagation_priority, nr.events_true, nr.events_false
FROM node_ratings nr
JOIN discovery_nodes n ON nr.node_id = n.node_id
ORDER BY nr.trust_score DESC
LIMIT 10;

-- Update validation count
UPDATE node_ratings
SET
    validations = validations + 1,
    last_updated = strftime('%s', 'now')
WHERE node_id = ?;
```


### Table: node_performance

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Monitoring **node performance** and **status** for tracking **node** performance **metrics** for **synchronization optimization**

**Fields** :
```
pubkey               (TEXT, NOT NULL) — FK → discovery_nodes.node_id — unique node identifier (public key)
last_seen            (INTEGER, NOT NULL) — time of last contact
relay_success_rate   (REAL, NOT NULL, DEFAULT 0.0) — percentage of successful transfers
quality_index        (REAL, NOT NULL, DEFAULT 0.0) — quality index (0.0 .. 1.0) - continuity of trust indicator
propagation_priority (REAL, NOT NULL, DEFAULT 0.0) — distribution priority (0.0 .. 1.0)
```
🏠 Database: discovery_nodes.sqlite  

#### Model: Node Performance Metrics

**Performance Model** :
```
P(n) = {success_rate, quality_index, priority}
```

**Success Rate Calculation** :
```
success_rate = successful_operations / total_operations
```

**Quality Index Model** :
```
Q(n) = α * recent_performance + β * historical_consistency + γ * trust_factor
```

**Metrics Update Rules** :
```
IF synchronization_attempt
    IF successful
        relay_success_rate = (previous_successes + 1) / total_attempts
    ELSE
        relay_success_rate = previous_successes / total_attempts
    last_seen = CURRENT_TIMESTAMP
```
**Notes** :  
• Quality index represents a weighted combination of **performance metrics**  
• **Metrics** are **updated** during **synchronization operations**  
• **Lower quality nodes** may be deprioritized for critical operations  

**SQL Implementation Example** :
```sql
-- Create node_performance table
CREATE TABLE node_performance (
    pubkey TEXT NOT NULL,
    last_seen INTEGER NOT NULL,
    relay_success_rate REAL NOT NULL DEFAULT 0.0,
    quality_index REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (pubkey) REFERENCES discovery_nodes(node_id)
);

-- Insert or update node performance
INSERT INTO node_performance (pubkey, last_seen, relay_success_rate, quality_index, propagation_priority)
VALUES (?, ?, 0.0, 0.0, 0.0)
ON CONFLICT(pubkey) DO UPDATE SET
    last_seen = excluded.last_seen;

-- Update relay success rate
UPDATE node_performance
SET
    relay_success_rate = (
        SELECT
            CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END
        FROM sync_attempts
        WHERE sync_attempts.peer_url = (SELECT address FROM discovery_nodes WHERE node_id = node_performance.pubkey)
    ),
    quality_index = (
        SELECT
            (CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END) * 0.5 +
            (CASE
                WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1
                THEN 0.5
                ELSE 0.1
            END)
        FROM sync_attempts
        WHERE sync_attempts.peer_url = (SELECT address FROM discovery_nodes WHERE node_id = node_performance.pubkey)
    ),
    propagation_priority = (
        SELECT
            ((CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END) * 0.5 +
            (CASE
                WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1
                THEN 0.5
                ELSE 0.1
            END)) * trust_score
        FROM node_ratings
        WHERE node_id = node_performance.pubkey
    )
WHERE pubkey = ?;

-- Get node performance with ratings
SELECT
    n.address,
    np.relay_success_rate,
    np.quality_index,
    np.propagation_priority,
    nr.trust_score
FROM node_performance np
JOIN discovery_nodes n ON np.pubkey = n.node_id
LEFT JOIN node_ratings nr ON n.node_id = nr.node_id
ORDER BY np.propagation_priority DESC;
```


### Table: active_tokens

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :
Managing authentication sessions based on **JWT tokens** for storing active refresh **tokens** allowing access **token** renewal without **re-authentication**

**Fields** :
```
public_key    (TEXT, NOT NULL) — FK → discovery_nodes.node_id
refresh_token (TEXT, NOT NULL, UNIQUE) — refresh token value
expires_at    (INTEGER, NOT NULL) — expiration timestamp
```
🏠 Database: discovery_nodes.sqlite  

#### Model: Authentication Token Management

**Token Lifecycle Model** :
```
Token = {public_key, refresh_token, expires_at}
```

**Expiration Rules** :
```
IF CURRENT_TIMESTAMP > expires_at
    token_status = 'expired'
    eligible_for_cleanup = TRUE
```

**Token Validation** :
```
IF refresh_token NOT valid_jwt_format
    ERROR "Invalid token format"
    
IF public_key NOT IN discovery_nodes.node_id
    ERROR "Token public key not associated with known node"
```
**Notes** :  
• Refresh tokens are stored securely and uniquely  
• Expired tokens are automatically cleaned up  
• Tokens are tied to specific node public keys for security  

**SQL Implementation Example**:
```sql
-- Create active_tokens table
CREATE TABLE active_tokens (
    public_key TEXT NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    expires_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES discovery_nodes(node_id)
);

-- Insert new refresh token
INSERT INTO active_tokens (public_key, refresh_token, expires_at)
VALUES (?, ?, (strftime('%s', 'now') + 86400)); -- Token expires in 24 hours

-- Validate token
SELECT COUNT(*) > 0 as is_valid
FROM active_tokens
WHERE refresh_token = ?
AND expires_at > strftime('%s', 'now');

-- Clean up expired tokens
DELETE FROM active_tokens
WHERE expires_at < strftime('%s', 'now');

-- Refresh token
UPDATE active_tokens
SET expires_at = (strftime('%s', 'now') + 86400) -- New expiry in 24 hours
WHERE refresh_token = ?;
```


### Table: peer_synchronization

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :  
Storing peer **synchronization history** for tracking interaction **history** with each **node** for **diagnostics** and **reliability analysis**

**Fields** :
```
id                 (INTEGER, PK, AUTOINCREMENT) — unique history record identifier
peer_url           (TEXT, NOT NULL) — FK → discovery_nodes.address
mode               (TEXT, NOT NULL) — synchronization mode
status             (TEXT, NOT NULL) — status of the synchronization
details            (TEXT, NOT NULL) — details of the synchronization process
last_sync          (INTEGER) — time of last synchronization
success_count      (INTEGER, DEFAULT 0) — counter of successful attempts
fail_count         (INTEGER, DEFAULT 0) — counter of failed attempts
last_quality_index (REAL, DEFAULT 0.0) — last quality index during synchronization
last_trust_score   (REAL, DEFAULT 0.0) — last trust score during synchronization
```
🏠 Database: discovery_nodes.sqlite  

#### Model: Peer Interaction History

**Interaction History Model** :
```
H(peer) = {success_count, fail_count, quality_metrics, trust_metrics}
```

**Synchronization Metrics** :
```
success_rate = success_count / (success_count + fail_count)
```

**History Update Rules** :
```
IF synchronization_attempt
    IF successful
        success_count = success_count + 1
        last_quality_index = current_quality
        last_trust_score = current_trust
    ELSE
        fail_count = fail_count + 1
    last_sync = CURRENT_TIMESTAMP
    mode = current_synchronization_mode
```
**Notes** :  
• Tracks historical performance of peer interactions  
• Supports diagnostic analysis of network issues  
• Quality and trust metrics are captured at time of synchronization  

**SQL Implementation Example**:
```sql
-- Create peer_synchronization table
CREATE TABLE peer_synchronization (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    peer_url TEXT NOT NULL,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    details TEXT NOT NULL,
    last_sync INTEGER,
    success_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    last_quality_index REAL DEFAULT 0.0,
    last_trust_score REAL DEFAULT 0.0,
    FOREIGN KEY (peer_url) REFERENCES discovery_nodes(address)
);

-- Insert new peer synchronization record
INSERT INTO peer_synchronization (peer_url, mode, status, details, last_sync, success_count, fail_count)
VALUES ((SELECT node_id FROM discovery_nodes WHERE address = ?), ?, ?, ?, strftime('%s', 'now'), 0, 0);

-- Update peer sync stats
UPDATE peer_synchronization
SET
    last_sync = strftime('%s', 'now'),
    success_count = success_count + CASE WHEN ? = 'success' THEN 1 ELSE 0 END,
    fail_count = fail_count + CASE WHEN ? != 'success' THEN 1 ELSE 0 END,
    last_quality_index = ?,
    last_trust_score = ?
WHERE peer_url = (SELECT node_id FROM discovery_nodes WHERE address = ?);

-- Get peer sync statistics
SELECT
    n.address,
    ps.mode,
    ps.success_count,
    ps.fail_count,
    CASE
        WHEN (ps.success_count + ps.fail_count) > 0
        THEN ps.success_count * 100.0 / (ps.success_count + ps.fail_count)
        ELSE 0.0
    END as success_rate,
    ps.last_sync,
    ps.last_quality_index,
    ps.last_trust_score
FROM peer_synchronization ps
JOIN discovery_nodes n ON ps.peer_url = n.node_id
ORDER BY ps.last_sync DESC;

-- Get peer with highest success rate
SELECT
    n.address,
    CASE
        WHEN (ps.success_count + ps.fail_count) > 0
        THEN ps.success_count * 100.0 / (ps.success_count + ps.fail_count)
        ELSE 0.0
    END as success_rate
FROM peer_synchronization ps
JOIN discovery_nodes n ON ps.peer_url = n.node_id
WHERE ps.success_count + ps.fail_count > 0
ORDER BY success_rate DESC
LIMIT 5;
```


### Table: sync_operations

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :
Tracking **low-level synchronization** operations for tracking changes at individual record level, **auditing and debugging synchronization**, **checking** data integrity during exchange between **nodes**, tracking authenticity of changes via **digital signatures**

**Fields** :
```
id         (INTEGER, PK, AUTOINCREMENT) — unique log record identifier
op         (TEXT, NOT NULL) — operation type (insert, update, delete)
table_name (TEXT, NOT NULL) — name of the table affected
record_id  (TEXT, NOT NULL) — identifier of the record affected
signature  (TEXT, NOT NULL) — signature of the synchronization participant
public_key (TEXT, NOT NULL) — FK → discovery_nodes.node_id — public key of the synchronization participant
created_at (INTEGER, NOT NULL) — timestamp of the operation
```
🏠 Database: discovery_nodes.sqlite  

#### Model: Synchronization Logging

**Log Entry Model** :
```
Log(entry) = {operation, table, record_id, signature, public_key, timestamp}
```

**Operation Types** :  
• INSERT — New record added during sync  
• UPDATE — Existing record modified during sync  
• DELETE — Record removed during sync  

**Integrity Verification** :
```sql
IF signature_verification(public_key, signature, operation_data) = FALSE
    log_integrity_error()
```

**Log Management Rules** :
```sql
IF created_at < (CURRENT_TIMESTAMP - retention_period)
    eligible_for_cleanup = TRUE
```
**Notes** :  
• Helps monitor network-wide operation and catch failures  
• Enables audit trail for synchronization operations  
• Supports integrity verification of synchronized data  

**SQL Implementation Example**:
```sql
-- Create sync_operations table
CREATE TABLE sync_operations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    op TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT NOT NULL,
    signature TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES discovery_nodes(node_id)
);

-- Insert sync operation
INSERT INTO sync_operations (op, table_name, record_id, signature, public_key, created_at)
VALUES (?, ?, ?, ?, ?, strftime('%s', 'now'));

-- Get all operations for a specific node
SELECT * FROM sync_operations
WHERE public_key = ?
ORDER BY created_at DESC;

-- Verify signature for a specific operation
SELECT s.*, n.address as node_address
FROM sync_operations s
JOIN discovery_nodes n ON s.public_key = n.node_id
WHERE s.id = ?;

-- Get sync operations by table
SELECT table_name, COUNT(*) as operation_count,
       SUM(CASE WHEN op = 'insert' THEN 1 ELSE 0 END) as inserts,
       SUM(CASE WHEN op = 'update' THEN 1 ELSE 0 END) as updates,
       SUM(CASE WHEN op = 'delete' THEN 1 ELSE 0 END) as deletes
FROM sync_operations
WHERE created_at > (strftime('%s', 'now') - 3600) -- Last hour
GROUP BY table_name;
```


### Table: sync_attempts

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :
Records **high-level synchronization events** between nodes for **monitoring** network-wide operations and **catching failures**

**Fields** :
```
id         (INTEGER, PK, AUTOINCREMENT) — unique log record identifier
timestamp  (INTEGER, NOT NULL) — when the sync occurred
peer_url   (TEXT, NOT NULL) — the peer node's URL
mode       (TEXT, NOT NULL) — sync mode or protocol (e.g., "full", "delta")
status     (TEXT, NOT NULL) — result status (e.g. "success" or error code)
details    (TEXT, NOT NULL) — additional info or error message
```
🏠 Database: discovery_nodes.sqlite  

#### Model: Synchronization Event Logging

**Synchronization Event Model** :
```
SyncLog = {timestamp, peer_url, mode, status, details}
```

**Synchronization Monitoring Rules**:
```
IF sync_operation_initiated
    log_sync_event(
        timestamp = CURRENT_TIMESTAMP,
        peer_url = target_node_url,
        mode = synchronization_mode,
        status = initial_status,
        details = operation_details
    )

IF sync_operation_completed
    update_sync_operation(
        status = final_status,
        details = completion_details
    )
```

**Log Analysis** :  
• Tracks synchronization success/failure rates  
• Monitors peer node connectivity  
• Records synchronization mode effectiveness  
• Captures error details for debugging

**Notes** :  
• Helps monitor network-wide operation and catch failures  
• Enables analysis of synchronization patterns  
• Supports network diagnostics and optimization

**SQL Implementation Example**:
```sql
-- Create sync_attempts table
CREATE TABLE sync_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    peer_url TEXT NOT NULL,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    details TEXT NOT NULL,
    FOREIGN KEY (peer_url) REFERENCES discovery_nodes(address)
);

-- Insert sync attempt entry
INSERT INTO sync_attempts (timestamp, peer_url, mode, status, details)
VALUES (strftime('%s', 'now'), ?, ?, ?, ?);

-- Get recent sync attempts for a peer
SELECT * FROM sync_attempts
WHERE peer_url = ?
ORDER BY timestamp DESC
LIMIT 10;

-- Get sync success rate for a peer
SELECT
    peer_url,
    COUNT(*) as total_syncs,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_syncs,
    (SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as success_rate
FROM sync_attempts
WHERE peer_url = ?
GROUP BY peer_url;

-- Clean up old sync attempts
DELETE FROM sync_attempts
WHERE timestamp < (strftime('%s', 'now') - 86400 * 7); -- Delete attempts older than 7 days
```


### Model Implementation References  
**For detailed SQL implementation see** 👇:  
- [model_core_network_tables.md](model_core_network_tables.md) — Node discovery and network tables schema  
- [model_core_views_network_tables.md](model_core_views_network_tables.md) — Views for node discovery and network operations  
- [model_core_collective_assessment.md](model_core_collective_assessment.md) — Collective assessment logic implementation  
- [model_core_views_collective_assessment.md](model_core_views_collective_assessment.md) — Views for collective assessment calculations  
- [model_core_scoring.md](model_core_scoring.md) — Impact and judgment scoring calculations  
- [model_core_views_scoring.md](model_core_views_scoring.md) — Views for impact and judgment score calculations  
- [model_core_aggregated_metrics.md](model_core_aggregated_metrics.md) — System metrics and expert functions schema  
- [model_core_views_aggregated_metrics.md](model_core_views_aggregated_metrics.md) — Views for system metrics and expert functions

## 5 Constraints, Security and Anti-Manipulation Mechanisms

**Truth Training** is designed as **system resistant** to **manipulation**, **centralization** and **substitution** of **collective truth**

**System lacks** :  
• **truth administrator**  
• **global moderator**  
• **centralized source of truth**  
• Any **attempt** at **centralization** is considered architectural **defect**  
• **No node** can have **disproportionate influence** on **aggregated truth**

**Influence is limited by** :  
• **trust weight**  
• **historical consistency**  
• **temporal decay**

### **Weight Propagation and Trust Limits**

**Model: Trust Weight Management** :  
• **The model ensures no node's influence grows unchecked**  
• Each node's **trust weight** (how heavily its information is counted) is **subject to decay** and a **maximum cap**

**Purpose** :  
• Defines per-node **caps** on **influence** to **resist manipulation** (e.g. Sybil attacks)  
• **System** is **resistant** to **creation** of **mass fake nodes**  
• **Together** with **behavioral analyses**, these **limits prevent** any one **node** from **disproportionately swaying system outcomes**  

#### Table: node_trust_limits

**Purpose** :  
Limiting **maximum influence** of **nodes**

**Fields** :
```
node_id          (TEXT, NOT NULL) — FK → discovery_nodes.node_id — node's public key
max_weight       (REAL, NOT NULL) — maximum allowable trust weight for this node
decay_factor     (REAL, NOT NULL) — per-period decay factor for that node's weight
small_constants  (REAL, NOT NULL) — small random constant in system time (using SQL implementation: CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END)
last_adjusted_at (INTEGER, NOT NULL) — timestamp when these limits were last updated
```
🏠 Database: discovery_nodes.sqlite  

**Used** :  
• behavioral signatures  
• temporal correlations  
• network patterns  
• cross-checking **impact** ↔ **judgment**

**Decay Function** :
```sql
UPDATE node_trust_limits
SET
    decay_factor = decay_factor * EXP(- (CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END) * (CAST(CURRENT_TIMESTAMP AS REAL) - CAST(last_adjusted_at AS REAL))),
    max_weight = GREATEST(max_weight, (
        SELECT MIN(node_trust_limits.max_weight, p.reputation_score * node_trust_limits.decay_factor)
        FROM impact_metrics im
        JOIN truth_event te ON im.event_id = te.id
        JOIN participants p ON te.participant_id = p.public_key
        WHERE im.event_id = node_trust_limits.node_id
    )),
    last_adjusted_at = CURRENT_TIMESTAMP
WHERE (CAST(CURRENT_TIMESTAMP AS REAL) - CAST(last_adjusted_at AS REAL)) > 0;
```

**Parameters** :  
• "node_trust_limits.decay_factor" ∈ (0,1) **controls temporal decay** of **weight**  
• **Older trust** evidence **weakens** over **time** by **multiplication** with **decay factor**  
• "node_trust_limits.max_weight" — **caps** the **maximum allowed weight** for a **node**  
• "CURRENT_TIMESTAMP" - "last_adjusted_at" — **time elapsed** since **last update** in seconds

**Trust Limit Mechanism** :
```sql
UPDATE node_trust_limits
SET decay_factor = CASE
    WHEN decay_factor > 0 AND decay_factor <= 1 AND max_weight > 0
        THEN decay_factor * EXP(- (CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 THEN 0.000001 ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) END) * (CAST(CURRENT_TIMESTAMP AS REAL) - CAST(last_adjusted_at AS REAL)))
    WHEN max_weight <= 0
        THEN decay_factor  -- No change if max_weight is invalid
    ELSE
        decay_factor  -- No change if decay_factor is out of range
END,
last_adjusted_at = CURRENT_TIMESTAMP
WHERE node_id IN (SELECT node_id FROM discovery_nodes)
  AND (CAST(CURRENT_TIMESTAMP AS REAL) - CAST(last_adjusted_at AS REAL)) > 0;
```

**Model Constraints** :  
• Each **node's** influence cannot exceed its allotted maximum  
• Influence naturally fades if not reinforced  
• Parameters are stored per-node in "node_trust_limits" table  
• Prevents Sybil attacks and disproportionate influence  

**Notes** :  
• Ensures **resistance** to **manipulation** attempts  
• Maintains **balanced distribution** of influence  
• **Supports** system **stability** over **time**

#### Table: node_behavior_patterns

**Purpose** :  
Storing behavioral **characteristics of nodes**

**Fields** :
```
node_id         (TEXT, NOT NULL) — FK → discovery_nodes.node_id
pattern_signature (TEXT, NOT NULL) — cryptographic signature
stability_score (REAL, NOT NULL) —
anomaly_score   (REAL, NOT NULL) —
updated_at      (INTEGER, NOT NULL) —
```
🏠 Database: discovery_nodes.sqlite  

**Manipulation** is **determined** not by content, but by **behavior structure**

**Examples** :  
• synchronous assessments  
• repeating patterns  
• sharp weight jumps  
• unnatural consistency

#### Table: manipulation_indicators

**Purpose** :  
Tracking **suspicious patterns**

**Fields** :
```
id             (INTEGER, PK, AUTOINCREMENT) —
node_id        (TEXT, NOT NULL) — FK → discovery_nodes.node_id
indicator_type (TEXT) — ENUM
severity       (INTEGER, NOT NULL) —
detected_at    (INTEGER, NOT NULL) —
```
🏠 Database: discovery_nodes.sqlite  

**Important** :  
System does **NOT block participants**

**Possible impact** :  
• **weight** reduction  
• temporary influence **decay**  
• **enhanced verification**  
• increased **consensus requirements**

**event** and **judgment** are **not deleted**

**Deletion is replaced by** :  
• **annotations**  
• **context**  
• **subsequent assessments**

**Attack** — is data.
**Data** — is signal.
System uses **attacks** as **training material**

All **security** mechanisms are part of model, **not external layer**

**Security** :  
• formalized  
• measurable  
• reproducible

## 6 Cognitive and Neural Network Analogies of Model

**Model** of **Truth Training** is intentionally designed to be **isomorphic** to **natural human cognitive** processes and principles of **neural network** operation

**Event** (event) is equivalent to external stimulus exciting cognitive system.

In neural analogy :  
• input signal  
• sensory impulse  
• feature vector  

**Impact** (impact) reflects space of possible event consequences.

Analogy :  
• signal propagation  
• influence on neighboring neurons  
• formation of associative connections  
• impact is not binary — it is continuous  

**Judgment** (judgment) represent truth assessment of event through collective confirmation.

Analogy :  
• activation function  
• neural network response  
• result of signal interpretation  

impact and judgment — two independent axes, intersecting only in limit.

This means :  
• truth is not equal to consequence  
• consequences do not prove truth  
• their consistency is manifested over time  

Each node — is autonomous neuron controlled by human.

Properties :  
• local memory  
• individual assessment function  
• limited bandwidth  
• learning through experience  

Unlike AI, activation function is set by human.

Human :  
• interprets signal  
• applies context  
• makes decision  
• makes judgment  

Set of nodes forms distributed neural network.

Characteristics :  
• no center  
• no global weights  
• learning through correlations  
• noise resistance  

Relational database — is material form of network.

Table connections correspond to :  
• synapses  
• weights  
• temporal delays  

System does not use backpropagation.

Learning occurs through :  
• accumulation of judgment  
• weakening of contradictory nodes  
• strengthening of consistent patterns  

Error is not deleted.

It :  
• is recorded  
• is analyzed  
• affects future weights  

Truth — is not value, but process.

It :  
• is refined  
• is stabilized  
• never freezes  

Truth Training — is hybrid :  
neural network + relational DB + human.

Each component is necessary.  
Removing any destroys system.  

## 7 Connection of Model with Quality Gates

For implementation details of Quality Gates, see [spec/14-quality-gates.md](../spec/14-quality-gates.md) which contains the concrete technical requirements and checks that enforce these model invariants.

Model of Truth Training is not abstract theory — it directly determines quality criteria for code, data and system behavior.  
Quality Gates serve as formalized mechanism for checking that implementation does not violate basic model principles.  
Each Quality Gate — is invariant of model that cannot be violated without loss of system correctness.

Examples :  
• database schema violation → destruction of cognitive connections  
• enum inconsistency → distortion of assessment axes  
• non-deterministic logic → loss of reproducibility 

Quality Gates guarantee that database structure corresponds to formal model schema.

Checked :  
• presence of all tables  
• correctness of foreign keys  
• consistency of types  
• immutability of field semantics  

This is critical because: relational database — physical body of neural network

impact axis requires :  
• impossibility of deleting consequences without reason  
• strict binding to event  
• preservation of temporal ordering  
• support for multiple consequences  

Quality Gate :  
• event → impact integrity tests  
• silent-delete prohibition  
• TTL and lifecycle check  

judgment — truth axis — has strictest requirements.

Quality Gates check :  
• cryptographic signature  
• immutability of judgment after fixation  
• correctness of aggregation  
• absence of history overwrite  

Violation of these rules is equivalent to :  
• damage to neural network activation function

Since human is part of computing circuit, his actions must be verifiable.

Quality Gates ensure :  
• impossibility of automatic judgment without human involvement  
• transparency of assessment source  
• reproducibility of assessment logic  

System must remain distributed.

Quality Gates prohibit :  
• hidden central states  
• global mutable-structures  
• implicit authorities  

Each node :  
• autonomous  
• verifiable  
• isolated  

Without Quality Gates model exists only on paper.

With Quality Gates :  
• model becomes executable  
• architecture — verifiable  
• development — safe  

## 8 Formal Conclusion of Model

Truth Training — is formally defined system consisting of :  
• relational database   
• distributed computing nodes  
• human assessment functions  
• strict quality invariants  

Model states following :  
• Truth is not value — it is process  
• Consequences and truth — are orthogonal  
• Human — is part of computing graph  
• Decentralization — is not option, but requirement  
• Error — is source of information, not failure  

Model intentionally :  
• does not seek instant truth  
• is not optimized for speed  
• does not centralize decision making  

It is optimized for :  
• resilience  
• long-term correlation  
• collective verifiability  

Truth Training is not :  
• voting  
• rating  
• expert system  
• traditional neural network  

This: cognitive infrastructure

Any implementation of Truth Training is considered correct if and only if :  
• database structure corresponds to model  
• Quality Gates are observed  
• impact and judgment axes are independent  
• human participation is not simulated  

This document :  
• is normative  
• is used for architectural decisions  
• serves as reference for PR and review  
• cannot be changed without new specification  

Truth Training — is not application. 

This :  
• way of collective thinking  
• formalized ethical mechanism  
• distributed cognitive system  

> **Truth is not what was said first.
> Truth is what survives circulation.**

