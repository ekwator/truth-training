# 📐 FORMAL MATHEMATICAL MODEL WITH DATABASE STRUCTURE IMPLEMENTATION "TRUTH TRAINING"
Truth Training is a system where truth is not defined but revealed as a statistically stable form of consequences in distributed and local space of independent public and personal observations.

> **Truth Training** is a system where truth is not declared but *emerges* as a statistically stable form of consequences and judgments in a distributed space of independent observers.

# I. Overview — Key Ideas

**Core postulates:**
- Truth is *computed*, not stored.
- Errors are allowed locally; stability arises globally.
- No trusted authority; robustness is statistical.
- Two orthogonal axes:
  - **Consequences axis** → *Impact*
  - **Truth axis** → *Judgments*

• System is distributed: multiple independent nodes N = {N₁, N₂, …}. Each node stores a local copy of the database, separately evaluates events, participates in P2P exchange.
• Event does not have a single "truth" — truth emerges statistically as a stable form of collective judgments.
• System has two independent axes of event evaluation:
◦ Consequence axis → impact
◦ Truth axis → judgments
• Local metrics (csᵢ, ciᵢ, cjᵢ) are used for training and aggregation — they are not equal to the final truth.

The system operates on a distributed set of nodes:

N={N_1, N_2, \dots, N_k\}

Each node maintains a local database, evaluates events independently, and participates in P2P circulation.

J. General Infrastructure Tables

```sql

CREATE TABLE app_config (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE schema_version (
  version    TEXT PRIMARY KEY,
  applied_at INTEGER NOT NULL,
  description TEXT
);

```
Explanation: service tables for configuration and schema version tracking.

# Q. Reference (knowledge-base) context tables
```sql

CREATE TABLE category (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  description TEXT
);

CREATE TABLE cause (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  quality     INTEGER NOT NULL, -- 0/1
  description TEXT
);

CREATE TABLE develop (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  quality     INTEGER NOT NULL, -- 0/1
  description TEXT
);

CREATE TABLE effect (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  quality     INTEGER NOT NULL, -- 0/1
  description TEXT
);

CREATE TABLE forma (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  quality     INTEGER NOT NULL, -- 0/1
  description TEXT
);

```

Explanation:
• quality ∈ {0,1} — semantic valence (positive/negative). This is not a truth metric; used for analytics/filtering and trends.
• Recommended to add indexes on name when needed.

# S. Users / Roles / Nodes
```sql

CREATE TABLE roles (
  role TEXT PRIMARY KEY,
  level INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE users (
  pubkey       TEXT PRIMARY KEY, -- UUID / public key
 role         TEXT NOT NULL,    -- FK → roles.role
  trust_score  REAL NOT NULL DEFAULT 0.0,
  last_updated INTEGER NOT NULL,
  display_name TEXT
);
FOREIGN KEY(users.role) REFERENCES roles(role);

```

Explanation: users.pubkey — unique identifier of participant (can be public key). roles manages access level/node behavior.

## 1. Formal Space of the System

Consider a distributed system of nodes:

N={N_1,N_2,…,N_k}

Each node:
stores a local copy of the database
independently accepts and evaluates events
participates in P2P distribution
Each node is identified by the users table:

```sql

CREATE TABLE users (
    pubkey        TEXT PRIMARY KEY,
    role          TEXT NOT NULL DEFAULT 'observer',
    trust_score   REAL NOT NULL DEFAULT 0.0,
    last_updated  INTEGER NOT NULL,
    display_name  TEXT
);
```

Roles define behavioral constraints, not authority.

Whose role (role: server, app) is identified by the table:

```sql

CREATE TABLE roles (
    role          TEXT PRIMARY KEY,
    level         INTEGER NOT NULL DEFAULT 'app',
);

```

## 2. Event as Mathematical Object
##### Table: truth_events
Event is formalized as a vector:

E_i=⟨gid_i,author_i,desc_i,ctx_i,v_i,d_i,c_i,t_start,t_end,cs_i,ci_i,cj_i⟩

Where:

gid_i — global event identifier
	truth_events.global_id
author_i - creator public key
	user_uuid
ctx_i — semantic context
v_i ∈{0,1} — direction vector
	truth_events.vector
d_i ∈ {0,1,∅} — detection flag
	truth_events.detected
c_i ∈ [0,255] — circulation code
	truth_events.code
cs_i - learning progress metric
	truth_events.collective_score
ci_i - impact metric
	truth_events.impact_score
cj_i - judgment metric
	truth_events.judgments_score

- Used for filtering and sorting when viewing events.

t_start,t_end — time boundaries determined by following rules:
	truth_events.timestamp_start
	truth_events.timestamp_end
- t_start - non-empty value, for new event can have any value, for existing event cannot be changed
- t_end - for new event can be undefined (empty value), cannot be less than t_start, for existing event can be changed and if date was already set automatically flag cr_i (corrected) is set
- Used for filtering and sorting when viewing events.

cs_i - Calculated value of training progress.
	truth_events.collective_score
- Calculated by corresponding algorithm working at local level individually for user without using network
- On global level used for calculating group training.

📌 cs_i — is not the final truth, but a training metric based on event at local level
Detailed mathematical model of training metric calculation described in section 7
 
ci_i - Calculated value of event impact (impact)
- Calculated by corresponding algorithm working at local level.
- When transmitted over network aggregated at local nodes for averaging and subsequent transmission to next nodes.
📌 ci_i - is not the final truth, but a consequence metric of event at local level
Detailed mathematical model of impact described in section 4.1

cj_i - Calculated value of judgments about event (judgments)
- Calculated by corresponding algorithm working at local level.
- When transmitted over network aggregated at local nodes for averaging and subsequent transmission to next nodes.
📌 cj_i - is not the final truth, but a judgment metric about event at local level
Detailed mathematical model of judgments described in section 4.2

##### ⚠️ Important:
Fields code (c_i), detected (d_i), corrected (cr_i) do not participate in truth calculation directly, only in transport logic
Detailed mathematical model of transport logic described in section 5
Fields collective_score (cs_i), impact (ci_i), judgments (cj_i) influence event relevance, training progress calculation and truth determination.

##### Purpose of truth_events table:
The structure of truth_events table in the project is implemented to store main data about events in Truth Training system.
Table is used to store main event records analyzed in Truth Training system. Each event contains information about its description, context (through embedded category, form, cause, manifestation and effect fields), timestamps, vector (incoming/outgoing), detection and correction status, and various assessments and cryptographic data.

##### Table is integrated into main event analysis system and used to store all main event data, including their contextual attributes, timestamps, statuses and cryptographic signatures. Note that in current implementation table contains duplicate fields for signatures and public keys, which may be a design artifact.

##### Table Structure:

```sql

CREATE TABLE truth_events (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    description     TEXT NOT NULL,
    global_id       TEXT NOT NULL UNIQUE,
    user_uuid       TEXT NOT NULL,
    category_id     INTEGER NOT NULL,
    forma_id        INTEGER NOT NULL,
    cause_id        INTEGER NOT NULL,
    develop_id      INTEGER NOT NULL,
    effect_id       INTEGER NOT NULL,
    vector          INTEGER NOT NULL,
    detected        INTEGER,
    corrected       INTEGER NOT NULL DEFAULT 0,
    timestamp_start INTEGER NOT NULL,
    timestamp_end   INTEGER,
    code            INTEGER NOT NULL DEFAULT 1,
    collective_score REAL,
    impact_score    REAL,
    judgments_score REAL,
    signature       TEXT,
    public_key      TEXT
);
```

**Invariant:** identity is defined by (gid_i, author_i), never by local autoincrement `id`.

Field explanations:
• global_id — mandatory UUID field used to identify same event on different nodes.
• vector — event direction (e.g. outgoing/incoming).
• detected, corrected, code — auxiliary transport fields (see section 6 on circulation codes).
• collective_score (csᵢ) — local training/assessment metric (see section 7).
• impact_score (ciᵢ) — local impact metric (see section 4.1).
• judgments_score (cjᵢ) — local judgments metric (see section 4.2).

##### Where structure is described:
In data schema documentation: docs/Data_Schema.md - contains description of truth_events table as part of base block, including all main fields and their purpose
In implementation code: core/src/storage.rs - contains SQL definition of table in SCHEMA_SQL constant
In event rating protocol documentation: docs/event_rating_protocol.md - describes use of code field and score calculation based on data from this table

##### ⚠️ Attention:
Following fields are missing in base schema:

global_id - global event identifier
user_uuid - author's public key
signature - field for storing cryptographic signature
public_key - field for storing public key
impact_score - field for storing impact assessment
judgments_score - field for storing judgment assessment
These fields are not part of initial table definition in SCHEMA_SQL constant.

However, as can be seen from run_migrations function in same file core/src/storage.rs (lines 464-476), during database migration these fields may be added:

```Rust

// Add signatures/keys for truth_events
if !has_column(conn, "truth_events", "signature")? {
    conn.execute("ALTER TABLE truth_events ADD COLUMN signature TEXT", [])?;
}
if !has_column(conn, "truth_events", "public_key")? {
    conn.execute("ALTER TABLE truth_events ADD COLUMN public_key TEXT", [])?;
}
// Add collective_score for truth_events
if !has_column(conn, "truth_events", "collective_score")? {
    conn.execute(
        "ALTER TABLE truth_events ADD COLUMN collective_score REAL",
        [],
    )?;
}

```

These fields are result of migrations, not part of initial schema.
In base schema these fields are indeed missing, but in working database they may be present after migration execution.
Thus, as result of migrations occurring during database initialization, truth_events table may receive additional fields, including signature and public_key. This explains why in some code parts (e.g. get_truth_event function) these fields are already partially accounted for.

## 3. Context as Semantic Space
Structure of tables:Context is fixed 5‑tuple:

ctx=⟨category,forma,cause,develop,effect⟩

Each element is FK:

category_id FK → PK  category.id
forma_id    FK → PK  forma.id
cause_id    FK → PK  cause.id
develop_id  FK → PK  develop.id
effect_id   FK → PK  effect.id

##### Context sets interpretation frame for consequences but does **not** affect truth directly.
Context table structure in project is implemented to store context templates used for classifying and describing events in Truth Training system.
Context can be filled manually by selecting appropriate records from subordinate tables with PK → FK filling:
For automatic filling of context fields context table is used

##### Purpose of context table:
Table is used to store context templates representing predefined combinations of categories, forms, causes, manifestations and effects. These templates allow systematizing and standardizing event description, facilitating their analysis and matching. Contextual templates are used for matching with events based on embedded context fields.

##### Table is integrated into event classification system and used for matching events with predefined context templates, allowing standardization of description and analysis of events in Truth Training system.

##### Table Structure:

```sql

CREATE TABLE context (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    category_id INTEGER,     -- FK → category.id
    forma_id    INTEGER,     -- FK → forma.id
    cause_id    INTEGER,     -- FK → cause.id
    develop_id  INTEGER,     -- FK → develop.id
    effect_id   INTEGER,     -- FK → effect.id
    description TEXT,
    FOREIGN KEY(category_id) REFERENCES category(id),
    FOREIGN KEY(forma_id)    REFERENCES forma(id),
    FOREIGN KEY(cause_id)    REFERENCES cause(id),
    FOREIGN KEY(develop_id)  REFERENCES develop(id),
    FOREIGN KEY(effect_id)   REFERENCES effect(id)
);

```

Where structure is described:
In data schema documentation: docs/Data_Schema.md - contains description of context table as part of knowledge_base block, including all fields and their purpose
In implementation code: core/src/storage.rs - contains SQL definition of table in SCHEMA_SQL constant
In template matching logic: docs/Data_Schema.md - describes template matching system and duplicate detection based on context fields

## 4 Impact Assessment and judgments

### 4.1 Impact as Observation and Prediction, impact is individual for each user
Each impact:

I_{ij} = ⟨event_i, type_j, value_j, t_j⟩

Where:

event_id → truth_events.id (FK)
type_id → impact_type.id
value ∈{0,1} — negative / positive
t_j - time of recording

Impact aggregates into a local metric:

ci_i = g(I(E_i))

##### Purpose of tables:
Structure of impact_type and impact tables in project is implemented to store event impact assessments in Truth Training system.
impact_type table - used for classifying types of event impacts (reputational, financial, moral, etc.). Serves as reference for impact types that can be applied to events.
impact table - used for storing subjective assessments (impacts) of events issued by validators. Each record represents assessment of specific event by specific validator, where:
Concept: impact — subjective observation/prediction of consequences from specific participant. impact is not equal to truth judgment.

##### Tables are integrated into event assessment system and used for calculating collective event assessments (S_e) based on weighted average by validators, and for updating author and validator reputations. See also docs/event_rating_protocol.md for algorithm description for calculating assessments based on data from impact table.

##### Table Structures:

```sql

CREATE TABLE impact_type (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  description TEXT
);

CREATE TABLE impact (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  event_id    INTEGER NOT NULL,  -- FK → truth_events.id
  type_id     INTEGER NOT NULL,  -- FK → impact_type.id
  value       INTEGER NOT NULL,  -- 0/1 (negative/positive)
  notes       TEXT,
  created_at  INTEGER NOT NULL,
  signature   TEXT,
  public_key  TEXT,
 FOREIGN KEY(event_id) REFERENCES truth_events(id),
  FOREIGN KEY(type_id)  REFERENCES impact_type(id)
);

CREATE INDEX idx_impact_type_id    ON impact(type_id);
CREATE INDEX idx_impact_created_at ON impact(created_at);

```

value - boolean value (0/1 or false/true), indicating positive or negative impact
type_id - reference to impact type (reputational, financial, etc.)
event_id - reference to event being assessed
signature and public_key - cryptographic data for verification of assessment authenticity

##### Where structure is described:
In event rating protocol documentation: docs/event_rating_protocol.md - describes purpose of impact table as storage of subjective event assessments, with type_id as impact type identifier and value as boolean value (true = confirmation, false = refutation)
In implementation code: core/src/storage.rs - contains SQL definition of both tables in SCHEMA_SQL constant
In impact processing logic: core/src/storage.rs - add_impact function for adding impact records

📌 Key idea:
Impact is observation and prediction of consequences for user, not opinion on truthfulness. For each user consequences may be different.

### 4.2 Judgments, individual for each user

Judgments are explicit truth assessments.

J_{u,i} = ⟨a_{u,i}, c_{u,i}⟩

Where:
- a_{u,i} ∈ {-1, +1}
- c_{u,i} ∈ (0,1]

Local judgment metric:

cj_i^{(u)} = a_{u,i} ⋅ c_{u,i}

📌 Key idea:
judgments is key moment of truth determination. After creating event its truthfulness is determined as follows:

#### 4.2.1 judgments Table

##### Purpose of judgments table:
judgments table structure in project is implemented to store judgments of system participants about events.
Table is used to store judgments that collective intelligence system participants issue about events. Each judgment represents assessment of specific event by specific participant and includes type of assessment, confidence level, reasoning and cryptographic signature.

##### Table is integrated into collective intelligence system and used for collecting participant opinions about events, which subsequently allows calculating consensus and updating participant reputations based on accuracy of their judgments.

##### Table Structure:

```sql

CREATE TABLE judgments (
    id TEXT PRIMARY KEY,
    participant_id TEXT NOT NULL,
    event_id TEXT NOT NULL,
    assessment TEXT NOT NULL,
    confidence_level REAL NOT NULL,
    reasoning TEXT,
    submitted_at INTEGER NOT NULL,
    signature TEXT NOT NULL,
    UNIQUE(participant_id, event_id),
    FOREIGN KEY(participant_id) REFERENCES participants(id),
    FOREIGN KEY(event_id) REFERENCES events_ci(id)
);

CREATE INDEX idx_participant_id    ON participants(id);
CREATE INDEX idx_event_id ON events_ci(id);

```

##### Where structure is described:
In specification: specs/001-collective-intelligence-layer/data-model.md - contains complete definition of table in "Database Schema Extensions" section, including Judgment entity and its attributes
In implementation code: core/src/storage.rs - contains SQL definition of table in SCHEMA_SQL constant
In models: core/src/collective_intelligence/models.rs - defines Judgment structure in Rust

#### 4.2 participants Table

##### Purpose of participants table:
participants table structure in project is implemented to store information about collective intelligence system participants.
Table is used to store data about collective intelligence system participants, including their cryptographic identifiers, reputation, judgment statistics and activity. Participants represent individual subjects who can issue judgments about events and participate in consensus formation.

##### Table is integrated into collective intelligence system and used for tracking participant reputation, allowing system to assess reliability and accuracy of judgments issued by each participant, use this information when calculating consensus and weighting votes.

##### Table Structure:

```sql

CREATE TABLE participants (
    id TEXT PRIMARY KEY,
    public_key TEXT UNIQUE NOT NULL,
    reputation_score REAL NOT NULL DEFAULT 0.5,
    total_judgments INTEGER NOT NULL DEFAULT 0,
    accurate_judgments INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    last_activity INTEGER
);

```

##### Where structure is described:
In specification: specs/001-collective-intelligence-layer/data-model.md - contains complete definition of table in "Database Schema Extensions" section, including Participant entity and its attributes
In implementation code: core/src/storage.rs - contains SQL definition of table in SCHEMA_SQL constant
In models: core/src/collective_intelligence/models.rs - defines Participant structure in Rust

#### 4.2.3 reputation_history Table

##### Purpose of reputation_history table:
reputation_history table structure in project is implemented to track changes in collective intelligence system participant reputations.
Table is used for auditing and analyzing changes in participant (participants) reputations in collective intelligence system. It allows:
Tracking history of reputation changes for each participant
Understanding reasons for reputation changes
Analyzing participant behavior and judgment effectiveness
Ensuring transparency of reputation system

##### Table is integrated into collective intelligence system and used for ensuring transparency and auditing of participant reputation changes when issuing judgments on events and reaching consensus.

##### Table Structure:

```sql

CREATE TABLE reputation_history (
    id TEXT PRIMARY KEY,
    participant_id TEXT NOT NULL,
    old_reputation REAL NOT NULL,
    new_reputation REAL NOT NULL,
    change_reason TEXT NOT NULL,
    event_id TEXT,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(participant_id) REFERENCES participants(id)
);

CREATE INDEX idx_participant_id    ON participants(id);

```

##### Where structure is described:
In specification: specs/001-collective-intelligence-layer/data-model.md - contains complete definition of table in "Database Schema Extensions" section
In implementation code: core/src/storage.rs - contains SQL definition of table in SCHEMA_SQL constant
In models: core/src/collective_intelligence/models.rs - defines ReputationHistory structure in Rust
In reputation update logic: core/src/collective_intelligence/reputation.rs - update_reputation function creates records in this table

#### 4.2.4 consensus_ci Table

##### Purpose of consensus_ci table:
Table is used to store computed consensus on events based on participant judgments. Consensus represents collective opinion formed based on individual judgments and used for determining general event assessment result.

##### Table is integrated into collective intelligence system and used for storing aggregated event assessment results, allowing system to make collective decisions based on individual participant judgments

##### consensus_ci table structure in project is implemented to store consensus results on events in collective intelligence system. Here is detailed description:

##### Table Structure:

```sql

CREATE TABLE consensus_ci (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    consensus_value TEXT NOT NULL,
    confidence_score REAL NOT NULL,
    participant_count INTEGER NOT NULL,
    calculated_at INTEGER NOT NULL,
    algorithm_version TEXT NOT NULL,
    FOREIGN KEY(event_id) REFERENCES events_ci(id)
);

CREATE INDEX idx_event_id ON events_ci(id);

```

##### Where structure is described:
In specification: specs/001-collective-intelligence-layer/data-model.md - contains complete definition of table in "Database Schema Extensions" section, including Consensus entity and its attributes
In implementation code: core/src/storage.rs - contains SQL definition of table in SCHEMA_SQL constant
In models: core/src/collective_intelligence/models.rs - defines Consensus structure in Rust

#### 4.2.5 events_ci Table

##### For What Implemented
events_ci table is intended for storing events within collective intelligence system (Collective Intelligence Layer). It is used for:
Storing events subject to participant assessment
Linking to participants via created_by field which references participants table
Classifying events using event_type field
Tracking event status (active, resolved, archived)
Storing event result data in resolution_data field
This table is part of collective assessment system where events are created by participants, then other participants can leave their judgments (in judgments table), based on which consensus is calculated (in consensus_ci table).

##### Table is linked to other collective intelligence system components:

Linked to participants (via created_by → participants.id)
Linked to judgments (via events_ci.id ← judgments.event_id)
Linked to consensus results (via events_ci.id ← consensus_ci.event_id)
Thus, events_ci table serves as central element of collective intelligence system around which participant judgments group and collective consensus is calculated.

##### Table Structure:

```sql

CREATE TABLE events_ci (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    event_type TEXT NOT NULL,
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    resolution_data TEXT,
    FOREIGN KEY(created_by) REFERENCES participants(id)
);

CREATE INDEX idx_participant_id    ON participants(id);

```

##### Where structure is described
events_ci table is described in file docs/schema-truth_teraining.sql, which contains SQL database schema for Desktop UI. In specifications (specs/001-collective-intelligence-layer/data-model.md) similar table events is described, which apparently was prototype for events_ci.

#### 4.3 Mathematical Model: Local and Global Truth Functions

##### 4.3.1 User's Local Assessment (csᵢ — collective_score local)
For each event Eᵢ for user u local assessment is determined via their judgments (J_{u,i}) and when necessary via impact:
Minimal model (simple):
judgment absent judgment givencsilocal​(u)={0,au,i​⋅cu,i​,​if judgment absentif judgment given​
where a_{u,i} ∈ {−1, +1} — assessment (false/true), c_{u,i} ∈ (0,1] — confidence.
More complex model can account for impact and participant reputation:
cs_i^{local}(u) = w_{rep}(u) ⋅ cj_i^{(u)} + w_{imp} ⋅ ci_i
where w_{rep}, w_{imp} — weights.
Storage: truth_events.collective_score may contain local cs (for local UX/training).

##### 4.3.2 Aggregation at Global Level — truth_score
When nodes transfer local assessments to global level, set is formed:
CS_i = \{ cs_i^{local}(u_1), \dots, cs_i^{local}(u_k) \}
Global truth score:
truth(E_i) = \frac{\sum_j cs_i^{local}(u_j)}{|CS_i| + \varepsilon}
where ε — small number to protect from division by zero.

Properties:
- No trust in source
- Stability emerges statistically

Interpretation:
• truth_score ≈ +1 — stable confirmation
• truth_score ≈ -1 — stable refutation
• ≈ 0 — conflict / insufficient data

##### 4.3.3 Temporal Weighting (time stability)
To suppress short-term spikes:
introduce weight depending on contribution age:
w(t)=log(1+Δt) 
where Δt — time difference between contribution moment and current time.
Weighted formula:
truth_scorei​=∑j​wj​∑j​wj​⋅csilocal​(uj​)​ 
Effect: long-living, repeatable observations get greater contribution; single spikes suppressed.

##### 4.3.4 Truth Formula (simplest variant)
In practical implementation can use:
Truth(Ei​)=∣I(Ei​)∣+εPi​−Ni​​ 
where P_i and N_i — sums of positive and negative contributions respectively (by judgments), I(E_i) — number of contributions.

#### 4.4 Rules and Invariants
• Truth is not stored — it is calculated on demand.
• Local errors are acceptable; stability arises statistically.
• code does not affect truth; it controls transport logic.
• global_id + user_uuid identifies event in network.
• When changing PK types (TEXT ↔ INTEGER) thoughtful migration and CLI / Desktop / Android coordination is required.

#### 4.5 Migrations and Compatibility
• When changing schema (e.g. adding embedded context fields or changing PK type in truth_events) it is mandatory:
1. Create migration creating new tables/indexes and transferring data.
2. When incompatible PK types (TEXT vs INTEGER) — define migration strategy (convert UUID → INT or unify in TEXT).
3. Update all layers (core, desktop, android) simultaneously or ensure bidirectional compatibility.
• Schema export (room / schema.json) must correspond to core/desktop schemas.

#### 4.6 Implementation and Testing Recommendations
1. Single PK Standard: recommend fixing truth_events.id as INTEGER AUTOINCREMENT everywhere or completely transition to TEXT (UUID) — mixing types leads to FK/join errors.
2. Migration Tests: add automatic migration tests to verify schema compliance at each version.
3. Contract Tests: for API endpoints /events, /contexts, /judgments — contract tests.
4. E2E / cross-device: verify truth_score aggregation behavior in real network (multiple nodes).
5. Logs and Observability: log event code transitions and aggregated truth_score for debugging.
6. WAL files / locks: ensure correct DB closing during tests to avoid SQLITE_BUSY.

## 5. Distributed Event Identification
### 5.1 Global Identifier

Event is considered same if:

id_i(Na)=id_i(Nb)
​
Thus event returned via another node is recognized by id_i — Global textual event identifier is formed as two transmittable values in event which excludes collisions: 
gid_i — global event identifier
	truth_events.global_id
author_i - author's public key
	truth_events.user_uuid

### 5.2 Event Circulation (P2P Model)

Each event circulates as pair:

⟨E_i,c_i⟩

Where 
E_i — i-th event
code = c_i — protocol state, event code (field truth_events.code)
Each event has 8-bit code which is used not for meaning but for protocol logic of distribution.

5.2.1 Purpose of Codes

Field code ∈ N8 (8-bit integer but for sqlite INTEGER NOT NULL DEFAULT 1) — this is discrete control parameter determining event life phase in distributed system.

Code does not carry truth semantics.
It controls movement, return and stop of event.

Thus, each event in network exists as:

⟨ E_i , code_i ⟩

where:

E_i — event content
code_i — circulation state
Event Circulation Codes (protocol code)
Field code — discrete control parameter of event life cycle. Recommended enumeration (as powers of two for safe bit shifting):

| Code | Meaning |
|-----:|--------|
| 1 | ORIGIN |
| 2 | PROPAGATION |
| 4 | EVALUATED |
| 8 | RETURNED |
| 16 | FINALIZED |

Properties:
• Transitions are unidirectional (1 → 2 → 4 → 8 → 16), PROPAGATION → RETURNED return allowed.
• code does not affect truth calculation, controls only allowed operations on event.

**Invariant:**

Truth(E_i) ⟂ c_i

Codes govern *movement*, not meaning.

#### 5.2.2 Set of Codes


Define finite set of codes:

C = { 2^k | k ∈ ℕ, 0 ≤ k ≤ 4 }

📌 Each code corresponds to strictly defined logical function.
Transition between states is implemented by bit shift operation (c_i ← 2 ⋅ c_i), which makes code monotonic and protects from state rollback.

#### 5.2.3 Code Semantics
Code 1 — ORIGIN
Original event

code = 1


Condition:
event created locally - new event (not observed in network before)

Function:

-primary distribution allowed
-event can be accepted by all nodes

Formally:

E_i ∉ Seen_j  ⇒  accept(E_i)

Code 2 — PROPAGATION

Active distribution

code = 2


Condition:

event received from another node
node has not participated in assessment yet

Function:

-local assessment
-adding local contribution
-possible retransmission

Formally:

E_i ∉ Evaluated_j  ⇒  evaluate(E_i)

Code 4 — EVALUATED

Evaluated locally

code = 4


Condition:

node completed assessment calculation
contribution added to aggregate

Function:

-repeat transmission forbidden
-awaiting possible return

Formally:

E_i ∈ Evaluated_j  ⇒  no_relay(E_i)

Code 8 — RETURNED

Returned aggregated event

code = 8


Condition:

event returned to node
id(E_i) already exists locally

Function:

comparison of local and external assessment
aggregate update

Formally:

E_i ∈ Seen_j  ⇒  merge(E_i-local , E_i-remote)

Code 16 — FINALIZED

Stabilized event

code = 16


Condition:

change in aggregated assessment < ε
event completed circulation

Function:

forbid further distribution
event considered stable

Formally:

|Score_t − Score_{t−1}| < ε  ⇒  finalize(E_i)

Finalization condition:

|truth_t(E_i) - truth_{t-1}(E_i)| < \varepsilon

When movement stops, truth stabilizes.

#### 5.2.4 State Transitions (logic diagram)
(1) ORIGIN
      ↓
(2) PROPAGATION
      ↓
(4) EVALUATED
      ↓
(8) RETURNED
      ↓
(16) FINALIZED


Permitted only unidirectional transitions, except return:
PROPAGATION → RETURNED
(when event reappears in another node)

#### 5.2.5 System Invariants

Code does not affect truth calculation

Truth(E_i) ⟂ code_i


Truth is calculated only via aggregation of judgment contributions

Score(E_i) = f(Σ judgments)


Code affects only allowed operations

AllowedActions = g(code)

#### 5.2.6 Key Conceptual Conclusion

Event lives while moving.
Truth emerges when movement stops.

Codes are time and path logic,
not meaning logic.

📌 Thus, event identity is determined not by source but by identifier.

## 7. Collective Event Assessment
Set of event impacts:

I(E_i)={I_(i1),I_(i2),…,I_(in)}

Divide by sign:

P_i=∑I_(ij)^(+), N_i=∑I_(ij)^(-)

### 7.1 Truthfulness as Statistical Function
----------------------
cs_i-local = f-local(I(E_i))
where:

I(E_i) — set of user judgments

function f-local depends only on local data
network not used

At global level, using network infrastructure, statements table is used, statements.truth_score is transferred to global level for calculating group training, calculated based on all events and cs_i field (truth_events.collective_score):

##### Table Structure:

```sql

CREATE TABLE statements (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id        INTEGER NOT NULL,          -- FK → truth_events.id
    truth_score     REAL,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    signature       TEXT, public_key TEXT,
    FOREIGN KEY(event_id) REFERENCES truth_events(id)
);

```

Explanation: statements aggregate local csᵢ for transfer to global level; progress_metrics — analytical journal.

For group progress statements table is used:

##### Table Structures:

```sql

CREATE TABLE group_ratings (
    group_id      TEXT PRIMARY KEY,
    members       TEXT    NOT NULL,
    avg_score     REAL    NOT NULL,
    coherence     REAL    NOT NULL,
    last_updated  INTEGER NOT NULL
);

CREATE TABLE progress_metrics (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp                    INTEGER NOT NULL,
    total_events                 INTEGER NOT NULL,
    total_events_group           INTEGER NOT NULL,
    total_positive_impact        REAL    NOT NULL,
    total_positive_impact_group  REAL    NOT NULL,
    total_negative_impact        REAL    NOT NULL,
    total_negative_impact_group  REAL    NOT NULL,
    trend                        REAL    NOT NULL,
    trend_group                  REAL    NOT NULL
);

```

truth_score_i-global = f-global({ cs_i-local_j })

Where:

{cs_i-local_j} — local assessments of different nodes

-aggregated without trust to source
-stability arises statistically

progress_metrics aggregates:
-individual
-group
-comparative trends

Event truthfulness is not stored but calculated:

Truth(E_i) = (P_i − N_i) / (|I(E_i)| + ε)

Where:

ε — protection from division by zero
result ∈ (−1, +1)

Interpretation:

→ +1 : stably confirmed
→ −1 : stably refuted
≈ 0 : conflict / lack of data

### 7.2 Aggregated System Metrics

Table: progress_metrics

State functions are recorded:

Trend=(∑P−∑N) / total_events

This is observation, not management.

## 8. Temporal Stability (strengthening)

Can introduce time weight:

w(t)=log(1+Δt)

Then:

Truth(E_i)=∑w_j⋅sign(I{ij}) / ∑w_j

This ensures:
-suppression of short-term spikes
-enhancement of stable consequences

## 9. Network / discovery / nodes

These tables are integrated into main database schema and used to support decentralized network, ensuring:
- Discovery and tracking of nodes
- Evaluation of node reliability through trust system
- Monitoring of network performance
- Support of data distribution algorithms
- Diagnosis and analysis of network stability
Explanation: tables for discovery and metrics for monitoring nodes and network stability.

### nodes Table:
Purpose: Storing information about discovered nodes in Truth Training network
Description: Used for tracking peer nodes, their addresses, types, availability and other discovery metadata

##### Table Structure:

```sql

CREATE TABLE nodes (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  address   TEXT NOT NULL UNIQUE,
  type      TEXT NOT NULL,
  reachable INTEGER NOT NULL,
  last_seen INTEGER NOT NULL,
  ttl       INTEGER NOT NULL,
  source    TEXT,
  node_id   TEXT,              -- node public key
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE sqlite_sequence(name,seq);
CREATE INDEX idx_nodes_address ON nodes(address);
CREATE INDEX idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX idx_nodes_type ON nodes(type);
CREATE INDEX idx_nodes_reachable ON nodes(reachable);

```

##### Key Fields:
- address: URL or ip:port of node (e.g. http://192.168.1.100:8080/api/v1)
- type: Node type (LAN, WIFI, GLOBAL, RELAY, CLIENT)
- reachable: Availability flag (0/1)
- last_seen: Time of last successful contact
- ttl: Record lifetime before automatic deletion
- node_id: Node public key (optional)

### node_ratings Table:
Purpose: Storing node reputation and trust
Description: Used for evaluating node reliability based on their activity and assessment accuracy

##### Table Structure:

```sql

CREATE TABLE node_ratings (
  node_id              TEXT PRIMARY KEY,
  events_true          INTEGER NOT NULL DEFAULT 0,
  events_false         INTEGER NOT NULL DEFAULT 0,
  validations          INTEGER NOT NULL DEFAULT 0,
  reused_events        INTEGER NOT NULL DEFAULT 0,
  trust_score          REAL NOT NULL DEFAULT 0.0,
  propagation_priority REAL NOT NULL DEFAULT 0.0,
  last_updated         INTEGER NOT NULL
);

```

##### Key Fields:
- events_true/events_false: Number of true/false events
- validations: Number of confirmations
- trust_score: Overall trust rating (-1.0 .. 1.0)
- propagation_priority: Distribution priority (0.0 .. 1.0)

### node_metrics Table:
Purpose: Monitoring node performance and status
Description: Used for tracking node performance metrics for synchronization optimization

##### Table Structure:

```sql

CREATE TABLE node_metrics (
  pubkey                TEXT PRIMARY KEY,
  last_seen             INTEGER NOT NULL,
  relay_success_rate    REAL NOT NULL DEFAULT 0.0,
  quality_index         REAL NOT NULL DEFAULT 0.0,
  propagation_priority REAL NOT NULL DEFAULT 0.0
);

```

##### Key Fields:
- last_seen: Time of last contact
- relay_success_rate: Percentage of successful transfers
- quality_index: Quality index (0.0 .. 1.0) - continuity of trust indicator
- propagation_priority: Distribution priority (0.0 .. 1.0)

### active_tokens Table:

active_tokens table structure is implemented in Truth Training project for managing authentication sessions based on JWT tokens. Table is used for storing active refresh tokens allowing access token renewal without re-authentication.

#### active_tokens Table Purpose:
Storing refresh tokens: Table contains refresh tokens issued during user authentication
Session management: Allows tracking active user sessions
Token renewal: Used for checking and renewing access tokens via /api/v1/refresh endpoint
Security: Allows revoking access by deleting refresh tokens
Thus, active_tokens table is important part of project authentication and authorization system, providing secure session management using JWT tokens.

#### Token Operation Functions:
- register_refresh_token() - registers new refresh token
- find_session_by_refresh() - finds session by refresh token
- delete_refresh_token() - deletes refresh token (during logout or update)
- cleanup_expired_tokens() - cleans expired tokens

##### Table Structure:

```sql

CREATE TABLE active_tokens (
            public_key    TEXT    NOT NULL,
            refresh_token TEXT    NOT NULL UNIQUE,
            expires_at    INTEGER NOT NULL
        );
CREATE INDEX idx_active_tokens_pub ON active_tokens(public_key);

```

#### Where Described:
In code: Table is created in migrations in file core/src/storage.rs in run_migrations function
In API: Used in authentication endpoints in file src/api.rs for /api/v1/refresh

### peer_history Table:
Purpose: Peer synchronization history
Description: Used for tracking interaction history with each node for diagnostics and reliability analysis

##### Table Structure:

```sql

CREATE TABLE peer_history (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  peer_url         TEXT NOT NULL,
  last_sync        INTEGER,
  success_count    INTEGER DEFAULT 0,
  fail_count       INTEGER DEFAULT 0,
  last_quality_index REAL DEFAULT 0.0,
  last_trust_score REAL DEFAULT 0.0
);

```

##### Key Fields:
- peer_url: Node URL
- last_sync: Time of last synchronization
- success_count/fail_count: Counters of successful/failed attempts
- last_quality_index/last_trust_score: Last metric values during synchronization

### Where Structure Is Described:
All these functions are described in project documentation, including docs/Data_Schema.md, docs/Discovery_Nodes_Schema.md, spec/08-p2p-sync.md and core/src/storage.rs.

## Chapter XI. Documentation Tree (Derived)

```
docs/
├── Data_Schema.md
├── event_rating_protocol.md
├── Discovery_Nodes_Schema.md
├── schema-truth_training.sql
specs/
├── 001-collective-intelligence-layer/
│   └── data-model.md
├── 08-p2p-sync.md
core/
├── src/storage.rs
├── src/collective_intelligence/
│   ├── models.rs
│   ├── reputation.rs
```

## 10. Synchronization Logs and Auxiliary Tables

Additional tables relate to network stability.

Classification
- Table	Model Role
- sync_log, sync_logs	causally-temporal operations log
- node_metrics	node quality metrics
 -peer_history	stability and reliability of channels
- active_tokens	authentication (does not participate in truth)
- app_config	environment parameters

📌 sync_log and sync_logs table structures are implemented in Truth Training project for tracking synchronization operations between network nodes.
- These tables form meta-level of distributed system stability, not affecting truth directly but limiting distribution and improving aggregation quality.
- These tables are important part of synchronization data system in decentralized Truth Training network, ensuring transparency and traceability of all data exchange operations between nodes.

### sync_log Table:
Purpose: Log of low-level synchronization operations for tracking changes at individual record level
Description: Contains information about each synchronization operation (insert, update, delete) specifying operation type, table name, record identifier, signature and public key of synchronization participant

Used for: Auditing and debugging synchronization, checking data integrity during exchange between nodes, tracking authenticity of changes via digital signatures

##### Table Structure:

```sql

CREATE TABLE sync_log (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            op           TEXT NOT NULL,
            table_name   TEXT NOT NULL,
            record_id    TEXT NOT NULL,
            signature    TEXT,
            public_key   TEXT,
            created_at   INTEGER NOT NULL
);

```

### sync_logs Table:
Purpose: Log of high-level synchronization attempts between nodes
Description: Contains information about synchronization sessions, including timestamps, participant URLs, synchronization mode, status and process details

Used for: Network operation monitoring, analysis of synchronization success between nodes, diagnosis of connection and performance problems

#### Table Operation Functions:
- log_sync() - writes low-level operations to sync_log
- log_sync_event() - writes high-level synchronization events to sync_logs
- get_recent_sync_logs() - gets recent entries from log
Mentioned in specification: Document spec/08-p2p-sync.md as part of synchronization audit system

##### Table Structure:

```sql

CREATE TABLE sync_logs (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp  INTEGER NOT NULL,
            peer_url   TEXT NOT NULL,
            mode       TEXT NOT NULL,
            status     TEXT NOT NULL,
            details    TEXT NOT NULL
);

```

#### Where Described:
In code: Tables are created in migrations in file core/src/storage.rs in run_migrations function

### Final Principle

> **Truth is not what was said first.
> Truth is what survives circulation.**
