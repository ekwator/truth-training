# Project Documentation Update - Update Constitution
# Formalized - Model and Database Schema
## Truth Training

**Document Version:** 2.4.0
**Status:** Draft / Specification
**Purpose:** formal description of data structure used
in Truth Training application and its compliance with
mathematical model of event, consequence and truth assessment.
This document describes the formal mathematical model of
Truth Training application with emphasis on relational database structure.

The model is designed for:
- formalization of application entities;
- description of relationships between events, consequences and judgments;
- ensuring reproducibility of truth calculation;
- alignment of Core / Desktop / Mobile implementations.

The model is designed for:
- formalization of application entities;
- description of relationships between events, consequences and judgments;
- ensuring reproducibility of truth calculation;
- alignment of Core / Desktop / Mobile implementations.
Truth Training system is based on the following principles:

1. Truth is *computed*, not stored.
2. Errors are allowed locally; stability arises globally.
3. No trusted authority; robustness is statistical.
4. Two orthogonal axes:
   - **Consequences axis** → *Impact*
   - **Truth axis** → *Judgments*

• System is distributed: multiple independent nodes N = {N₁, N₂, …}. Each node stores a local copy of the database, separately evaluates events, participates in P2P exchange.
• Event does not have a single "truth" — truth emerges statistically as a stable form of collective judgments.
• System has two independent axes of event evaluation:
◦ Consequence axis → impact
◦ Truth axis → judgments
• Local metrics (csᵢ, ciᵢ, cjᵢ) are used for training and aggregation — they are not equal to the final truth.

Model reflects "one-to-many" principle:

- one event → multiple interpretations;
- one source → multiple consequences;
- one observation → multiple judgments.

The system operates on a distributed set of nodes:

N={N_1, N_2, \dots, N_k\}

Each node maintains a local database, evaluates events independently, and participates in P2P circulation.

By analogy:
- neural network = vector + relational structure;
- system nodes = assessors;
- connections = judgments and consequences.
- application model includes the following main entity classes:

- Event
- Impact
- Judgment
- Participant / User
- Consensus / Aggregation

Each entity is mapped to one or more relational database tables.
Model reflects "one-to-many" principle:

- one event → multiple interpretations;
- one source → multiple consequences;
- one observation → multiple judgments.

The system operates on a distributed set of nodes:

N={N_1, N_2, \dots, N_k\}

Each node maintains a local database, evaluates events independently, and participates in P2P circulation.

By analogy:
- neural network = vector + relational structure;
- system nodes = assessors;
- connections = judgments and consequences.
- application model includes the following main entity classes:

- Event
- Impact
- Judgment
- Participant / User
- Consensus / Aggregation

Each entity is mapped to one or more relational database tables.
Document is coordinated and should be used jointly with:

- mat_model.md — more complete and accurate description of mathematical model with database structure implementation;
- SECURITY.md — security and verification requirements;
- CONTRIBUTING.md — quality and testing requirements;
- 14-quality-gates.md — minimum requirements for PR acceptance.

## 2. Basic Model Entities and Service Tables

This chapter describes fundamental database tables that provide object identification, their lifecycle and integrity of Truth Training model.
Database structure complies with following principles:

- Relational model with explicit primary and foreign keys
- Absence of stored calculated truth values
- All aggregates are calculated at Core logic level
- Data historization: records are not overwritten, but supplemented
- Support for multiple sources and evaluation contexts

### Table: participants

Purpose:
Stores information about collective intelligence system participants.

Fields:
- id (TEXT, PK) — unique participant identifier (public key)
- public_key (TEXT, UNIQUE, NOT NULL) — participant's public key
- reputation_score (REAL, NOT NULL, DEFAULT 0.5) — participant's reputation score
- total_judgments (INTEGER, NOT NULL, DEFAULT 0) — total number of judgments made
- accurate_judgments (INTEGER, NOT NULL, DEFAULT 0) — number of accurate judgments
- created_at (INTEGER, NOT NULL) — registration time
- last_activity (INTEGER) — timestamp of last activity

Notes:
- Participant is not tied to identity
- Authentication is based on cryptographic key
- Reputation is calculated based on accuracy of judgments

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

### Table: context

Purpose:
Storing interpretation context templates representing predefined combinations of categories, forms, causes, manifestations and effects.

Fields:
- id (INTEGER, PK) — unique context identifier
- name (TEXT, NOT NULL) — context name
- category_id (INTEGER) — FK to category.id
- forma_id (INTEGER) — FK to forma.id
- cause_id (INTEGER) — FK to cause.id
- develop_id (INTEGER) — FK to develop.id
- effect_id (INTEGER) — FK to effect.id
- description (TEXT) — context description

## 3. Events and Consequence Axis (Impact Axis)

Event is central entity of Truth Training model.
It represents a fixed fact or statement around which truth assessments and observable consequences are formed.

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

### Table: truth_events

Purpose:
Storing main data about events in Truth Training system, including their description, context, timestamps, vector (incoming/outgoing), detection and correction status, and various assessments.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — local auto-increment identifier
- description (TEXT, NOT NULL) — event description
- global_id (TEXT, NOT NULL, UNIQUE) — global event identifier for network identification
- user_uuid (TEXT, NOT NULL) — author's public key
- category_id (INTEGER, NOT NULL) — FK to category.id
- forma_id (INTEGER, NOT NULL) — FK to forma.id
- cause_id (INTEGER, NOT NULL) — FK to cause.id
- develop_id (INTEGER, NOT NULL) — FK to develop.id
- effect_id (INTEGER, NOT NULL) — FK to effect.id
- vector (INTEGER, NOT NULL) — event direction (0/1)
- detected (INTEGER) — detection flag (0/1/NULL)
- corrected (INTEGER, NOT NULL, DEFAULT 0) — correction flag
- timestamp_start (INTEGER, NOT NULL) — start time of event
- timestamp_end (INTEGER) — end time of event
- code (INTEGER, NOT NULL, DEFAULT 1) — circulation code for distribution protocol
- collective_score (REAL) — local training/assessment metric
- impact_score (REAL) — local impact metric
- judgments_score (REAL) — local judgments metric
- signature (TEXT) — cryptographic signature
- public_key (TEXT) — public key for verification

Notes:
- Event identity is defined by (global_id, user_uuid), never by local autoincrement id
- Event content is stored in embedded context fields (category, forma, cause, etc.)
- Circulation code controls distribution protocol, not truth calculation
- collective_score, impact_score, and judgments_score are local metrics, not final truth values

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

### Table: impact_type

Purpose:
Classifying types of event impacts (reputational, financial, moral, etc.) serving as reference for impact types that can be applied to events.

Fields:
- id (INTEGER, PK) — unique impact type identifier
- name (TEXT, NOT NULL) — impact type name
- description (TEXT) — description of the impact type

### Table: impact

Purpose:
Storing subjective assessments (impacts) of events issued by validators, representing observation and prediction of consequences from specific participant, not opinion on truthfulness.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique impact identifier
- event_id (INTEGER, NOT NULL) — FK to truth_events.id
- type_id (INTEGER, NOT NULL) — FK to impact_type.id
- value (INTEGER, NOT NULL) — impact value (0/1 for negative/positive)
- notes (TEXT) — additional notes about the impact
- created_at (INTEGER, NOT NULL) — timestamp of impact recording
- signature (TEXT) — cryptographic signature for verification
- public_key (TEXT) — public key for verification

Notes:
- Consequence is always linked to specific event
- One event can have multiple consequences
- Consequences are not aggregated at DB level
- value is boolean (0/1) indicating negative/positive impact

### Table: impact_links

Purpose:
Allows linking consequences to each other, forming chains of cause-and-effect relationships.

Fields:
- id (UUID, PK)
- source_impact_id (UUID, FK → impacts.id)
- target_impact_id (UUID, FK → impacts.id)
- relation_weight (FLOAT)
- created_at (TIMESTAMP)

Notes:
- Implements consequence graph
- Used for analysis of secondary effects
Consequence axis (Impact Axis) reflects objective or observable effects of event over time.

Unlike truth axis:
- Impact can exist without consensus
- Impact is measurable and comparable
- Impact forms system's learning feedback

## 4. Truth Axis (Judgments Axis)

Truth axis describes process of collective and individual event assessment.
Unlike consequence axis, truth is not an objective value and is formed through system participants' judgments.

### Table: judgments

Purpose:
Storing judgments that collective intelligence system participants issue about events, representing assessment of specific event by specific participant.

Fields:
- id (TEXT, PK) — unique judgment identifier
- participant_id (TEXT, NOT NULL) — FK to participants.id
- event_id (TEXT, NOT NULL) — FK to events_ci.id
- assessment (TEXT, NOT NULL) — type of assessment
- confidence_level (REAL, NOT NULL) — confidence level of the assessment
- reasoning (TEXT) — reasoning behind the judgment
- submitted_at (INTEGER, NOT NULL) — timestamp of submission
- signature (TEXT, NOT NULL) — cryptographic signature

Constraints:
- UNIQUE(participant_id, event_id) — each participant can have only one judgment per event

Notes:
- Judgment is not changed, only new record is possible
- Absence of judgment ≠ negative judgment

### Table: judgment_weights

Purpose:
Defines weight of participant's judgment in specific context.
Weight reflects system's trust in participant.

Fields:
- id (UUID, PK)
- participant_id (UUID, FK → participants.id)
- context_id (UUID, FK → contexts.id)
- weight (FLOAT)
- calculated_at (TIMESTAMP)

Notes:
- Weight is not set manually
- Weight is derivative of action history

### Table: reputation_history

Purpose:
Tracking changes in collective intelligence system participant reputations for auditing and analyzing changes in participant reputations, understanding reasons for reputation changes, analyzing participant behavior and judgment effectiveness, and ensuring transparency of reputation system.

Fields:
- id (TEXT, PK) — unique history record identifier
- participant_id (TEXT, NOT NULL) — FK to participants.id
- old_reputation (REAL, NOT NULL) — previous reputation score
- new_reputation (REAL, NOT NULL) — new reputation score
- change_reason (TEXT, NOT NULL) — reason for reputation change
- event_id (TEXT) — associated event ID
- updated_at (INTEGER, NOT NULL) — timestamp of update

Notes:
- Used for auditing and transparency of reputation changes
- Tracks historical changes for analysis

### Table: consensus_ci

Purpose:
Storing computed consensus on events based on participant judgments, representing collective opinion formed based on individual judgments and used for determining general event assessment result.

Fields:
- id (TEXT, PK) — unique consensus identifier
- event_id (TEXT, NOT NULL) — FK to events_ci.id
- consensus_value (TEXT, NOT NULL) — the consensus value reached
- confidence_score (REAL, NOT NULL) — confidence in the consensus
- participant_count (INTEGER, NOT NULL) — number of participants involved
- calculated_at (INTEGER, NOT NULL) — timestamp of calculation
- algorithm_version (TEXT, NOT NULL) — version of algorithm used

Notes:
- Used for storing aggregated event assessment results
- Enables system to make collective decisions based on individual participant judgments
### Table: events_ci

Purpose:
Storing events within collective intelligence system (Collective Intelligence Layer) for participant assessment, classification using event_type field, tracking event status (active, resolved, archived), and storing event result data in resolution_data field.

Fields:
- id (TEXT, PK) — unique event identifier
- title (TEXT, NOT NULL) — event title
- description (TEXT) — event description
- event_type (TEXT, NOT NULL) — type of event
- created_by (TEXT, NOT NULL) — FK to participants.id
- created_at (INTEGER, NOT NULL) — timestamp of creation
- status (TEXT, NOT NULL, DEFAULT 'active') — event status
- resolution_data (TEXT) — data about event resolution

Notes:
- Events are created by participants
- Other participants can leave judgments in the judgments table
- Consensus is calculated in the consensus_ci table
Truth axis (Judgments Axis) is subjective and dynamic.

Key properties:
- Truth is formed collectively
- Truth can change over time
- High confidence ≠ high truth
- Consensus does not guarantee correspondence to reality

Event is intersection point of two independent axes:
- Impact Axis → consequences and effects
- Judgments Axis → interpretations and assessments

Their intersection occurs:
- not in time
- not in logic
- but in system learning

## 5. Contexts, Observers and System Learning

Context defines framework for interpretation of events and judgments.
Without context, neither consequences nor truth can be correctly matched or aggregated.

Context acts as coordinate system in which observer draws conclusion.

### Table: contexts

Purpose:
Describes logical, temporal or thematic areas within which events are assessed and judgments are formed.

Fields:
- id (UUID, PK)
- name (TEXT)
- description (TEXT)
- context_type (ENUM) — global / local / temporal / thematic
- created_at (TIMESTAMP)

Notes:
- Same event can exist in multiple contexts
- Contexts can intersect

### Table: context_events

Purpose:
Links events to their interpretation contexts.

Fields:
- context_id (UUID, FK → contexts.id)
- event_id (UUID, FK → events.id)
- linked_at (TIMESTAMP)

Constraints:
- (context_id, event_id) — composite PK

### Table: observers

Purpose:
Records system participants as observers, not as truth sources.

Fields:
- id (UUID, PK)
- participant_id (UUID, FK → participants.id)
- observer_role (ENUM) — witness / evaluator / analyzer
- created_at (TIMESTAMP)

Notes:
- Observer can have multiple roles
- Role affects admissible actions

### Table: observer_contexts

Purpose:
Defines in which contexts observer is active.

Fields:
- observer_id (UUID, FK → observers.id)
- context_id (UUID, FK → contexts.id)
- trust_level (FLOAT)
- assigned_at (TIMESTAMP)

Constraints:
- (observer_id, context_id) — composite PK

### Table: learning_snapshots

Purpose:
Stores system states for learning and analysis.

Fields:
- id (UUID, PK)
- context_id (UUID, FK → contexts.id)
- model_version (TEXT)
- snapshot_data (JSONB)
- created_at (TIMESTAMP)

Notes:
- Used for offline learning
- Does not directly affect online decisions
System learning occurs not on events, but on differences in event interpretations in different contexts.

Context → distortion
Observer → filter
Judgment → signal
Consequence → feedback
Truth Training does not learn "truth".
System learns to recognize stable structures between perception, consequences and collective judgment.

This makes system:
- resistant to manipulation
- insensitive to single sources
- capable of self-correction

## 6. Temporal Dynamics and Truth Evolution

Truth in Truth Training system is not static value.
It exists as function of time, context and accumulated experience.

Each event and each judgment has temporal extent, and their significance changes as new data arrives.

### Table: time_axes

Purpose:
Describes independent time scales used for analysis of events and judgments.

Fields:
- id (UUID, PK)
- name (TEXT)
- description (TEXT)
- time_type (ENUM) — physical / logical / causal
- created_at (TIMESTAMP)

Notes:
- Physical time ≠ logical time
- Causal time can be non-linear

### Table: event_timeline

Purpose:
Records position of event on various time axes.

Fields:
- event_id (UUID, FK → events.id)
- time_axis_id (UUID, FK → time_axes.id)
- t_start (TIMESTAMP)
- t_end (TIMESTAMP, nullable)

Constraints:
- (event_id, time_axis_id) — composite PK

### Table: judgment_timeline

Purpose:
Tracks changes in judgments over time.

Fields:
- judgment_id (UUID, FK → judgments.id)
- time_axis_id (UUID, FK → time_axes.id)
- value_at_time (FLOAT)
- recorded_at (TIMESTAMP)

Notes:
- One judgment can have multiple states
- Changing value does not delete history

### Table: truth_state

Purpose:
Stores aggregated truth state of event at given point in time.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- context_id (UUID, FK → contexts.id)
- time_axis_id (UUID, FK → time_axes.id)
- truth_score (FLOAT)
- confidence (FLOAT)
- calculated_at (TIMESTAMP)

Notes:
- truth_score ≠ verdict
- confidence reflects assessment stability

### Table: decay_functions

Purpose:
Defines decay functions for data significance over time.

Fields:
- id (UUID, PK)
- target_type (ENUM) — event / judgment / impact
- function_type (ENUM) — linear / exponential / custom
- parameters (JSONB)
- created_at (TIMESTAMP)

Notes:
- Different data types have different obsolescence rates
Truth is represented not as point, but as trajectory in space:

(context × time × collective perception)

Stable truth — is trajectory with small curvature.
Manipulation — sharp local bend.
- Event → input signal
- Judgment → activation
- Time → integrator
- Decay → forgetting factor
- Truth State → neuron output

But activation function is determined by people, not model parameters.

System:
- does not require data rollback
- allows contradictory states
- can re-assess past
without rewriting it

## 7. Truth Axis (Judgments Axis)

Truth axis describes process of formation, change and collective stabilization of event truth.

Truth in Truth Training system is not calculated directly — it arises as result of interaction of multiple independent judgments made by people.

### Table: judgments

Purpose:
Records individual judgment of subject regarding event.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- subject_id (UUID, FK → subjects.id)
- judgment_value (FLOAT) — range [-1.0 … +1.0]
- weight (FLOAT) — subject weight
- confidence (FLOAT) — subjective confidence
- created_at (TIMESTAMP)

Constraints:
- One subject can have only one active judgment
  for one event within one context

### Table: judgment_versions

Purpose:
Stores history of judgment changes without data loss.

Fields:
- id (UUID, PK)
- judgment_id (UUID, FK → judgments.id)
- judgment_value (FLOAT)
- confidence (FLOAT)
- reason (TEXT)
- created_at (TIMESTAMP)

Notes:
- Any change creates new version
- Truth is not overwritten, it evolves

### Table: judgment_relations

Purpose:
Describes logical and causal relationships between judgments.

Fields:
- source_judgment_id (UUID, FK → judgments.id)
- target_judgment_id (UUID, FK → judgments.id)
- relation_type (ENUM) — supports / contradicts / refines
- strength (FLOAT)

Constraints:
- (source_judgment_id, target_judgment_id) — composite PK

### Table: collective_truth

Purpose:
Stores aggregated collective truth assessment of event.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- context_id (UUID, FK → contexts.id)
- truth_score (FLOAT)
- dispersion (FLOAT)
- stability (FLOAT)
- calculated_at (TIMESTAMP)

Interpretation:
- truth_score — collective direction
- dispersion — degree of disagreement
- stability — temporal stability

### Table: judgment_weights

Purpose:
Defines dynamic weight of subject in system.

Fields:
- subject_id (UUID, FK → subjects.id)
- context_id (UUID, FK → contexts.id)
- weight (FLOAT)
- updated_at (TIMESTAMP)

Notes:
- Weight is not reputation
- Weight reflects consistency with system

Event truth in context C:

T(E, C) = Σ (J_i × W_i × C_i) / Σ (|W_i|)

Where:
- J_i — judgment value
- W_i — subject weight
- C_i — subject confidence
- Convergence → consensus formation
- Divergence → sign of complex or manipulable event
- Absence of convergence ≠ falsehood

Judgments Axis ⟂ Impact Axis

Truth and consequences are independent:
- True event can have catastrophic consequences
- False event can have no consequences

Both axes intersect in limit of infinite observation
- Judgment → neuron activation
- Weight → synaptic coefficient
- Collective Truth → layer output
- Human → activation function

System:
- does not impose truth
- allows parallel contradictions
- measures not "who is right", but "how stable is opinion"

## 8. Impact Axis

Impact axis describes measurable, observable and predictable effects of events over time.

Unlike truth axis, impact axis does not operate with opinions or interpretations — it records changes in system and environment states.

### Table: impacts

Purpose:
Records specific consequence of event.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- impact_type (ENUM) — social / economic / technical / cognitive / ecological
- magnitude (FLOAT) — impact force
- polarity (ENUM) — positive / negative / neutral
- description (TEXT)
- observed_at (TIMESTAMP)

Notes:
- One event can have multiple consequences
- Consequences can be in different directions

### Table: impact_chains

Purpose:
Describes cause-and-effect chains of consequences.

Fields:
- source_impact_id (UUID, FK → impacts.id)
- target_impact_id (UUID, FK → impacts.id)
- delay (INTERVAL)
- probability (FLOAT)

Constraints:
- (source_impact_id, target_impact_id) — composite PK

### Table: impact_metrics

Purpose:
Stores aggregated metrics of event consequences.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- total_magnitude (FLOAT)
- positive_ratio (FLOAT)
- negative_ratio (FLOAT)
- uncertainty (FLOAT)
- calculated_at (TIMESTAMP)

### Table: impact_observers

Purpose:
Records subjects or systems that recorded consequences.

Fields:
- impact_id (UUID, FK → impacts.id)
- observer_id (UUID, FK → subjects.id)
- observation_confidence (FLOAT)
- recorded_at (TIMESTAMP)

Constraints:
- (impact_id, observer_id) — composite PK

### Table: impact_predictions

Purpose:
Stores predicted consequences of event.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- predicted_impact_type (ENUM)
- expected_magnitude (FLOAT)
- probability (FLOAT)
- horizon (INTERVAL)
- created_at (TIMESTAMP)

Notes:
- Forecast ≠ fact
- Forecasts do not directly affect truth

Total impact of event E:

I(E) = Σ (M_i × P_i)

Where:
- M_i — consequence magnitude
- P_i — probability or confirmation
- Consequences can be delayed
- Small event → large consequences
- Large event → zero consequences

Impact(E) ⟂ Truth(E)

Consequences exist independently of
whether event is recognized as true.
- Impact → output signal
- Impact Chain → activation propagation
- Delay → time constant
- Probability → signal attenuation

System:
- measures consequences without moral assessment
- allows uncertainty
- supports prediction without truth assertion

## 9. Axis Intersection: Truth × Impact

Each event in Truth Training system exists simultaneously in two independent spaces:

- Truth space (Judgments)
- Impact space (Impact)

These axes are orthogonal and cannot be derived from each other.
Event E is represented as point:

E = ( T(E), I(E) )

Where:
- T(E) ∈ [0,1] — aggregated truth
- I(E) ∈ ℝ — cumulative impact

On Truth × Impact plane, 4 basic classes are identified:

1. High truth / High impact
2. High truth / Low impact
3. Low truth / High impact
4. Low truth / Low impact
| Truth | Impact | Interpretation                 |
| ----- | ------ | ----------------------------- |
| High  | High   | Critical real event           |
| High  | Low    | Fact without significant consequences |
| Low   | High   | Dangerous disinformation      |
| Low   | Low    | Noise / information garbage   |

### Table: event_projection

Purpose:
Stores event projection in Truth × Impact space.

Fields:
- event_id (UUID, PK, FK → events.id)
- truth_score (FLOAT)
- impact_score (FLOAT)
- quadrant (ENUM) — Q1 / Q2 / Q3 / Q4
- calculated_at (TIMESTAMP)

quadrant(E) =
  Q1 if T(E) ≥ θ_T and I(E) ≥ θ_I
 Q2 if T(E) ≥ θ_T and I(E) < θ_I
 Q3 if T(E) < θ_T and I(E) ≥ θ_I
 Q4 otherwise

### Table: statements

Purpose:
Aggregating local csᵢ for transfer to global level for calculating group training, calculated based on all events and cs_i field (truth_events.collective_score).

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique statement identifier
- event_id (INTEGER, NOT NULL) — FK to truth_events.id
- truth_score (REAL) — aggregated truth score
- created_at (INTEGER, NOT NULL) — timestamp of creation
- updated_at (INTEGER, NOT NULL) — timestamp of last update
- signature (TEXT) — cryptographic signature
- public_key (TEXT) — public key for verification

### Table: group_ratings

Purpose:
Storing group ratings for collective assessment of truth training progress.

Fields:
- group_id (TEXT, PK) — unique group identifier
- members (TEXT, NOT NULL) — list of group members
- avg_score (REAL, NOT NULL) — average score of the group
- coherence (REAL, NOT NULL) — coherence of the group's assessments
- last_updated (INTEGER, NOT NULL) — timestamp of last update

### Table: progress_metrics

Purpose:
Aggregating individual, group, and comparative trends for system metrics.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique metric identifier
- timestamp (INTEGER, NOT NULL) — timestamp of the metric
- total_events (INTEGER, NOT NULL) — total number of events
- total_events_group (INTEGER, NOT NULL) — total number of group events
- total_positive_impact (REAL, NOT NULL) — total positive impact
- total_positive_impact_group (REAL, NOT NULL) — total positive impact for group
- total_negative_impact (REAL, NOT NULL) — total negative impact
- total_negative_impact_group (REAL, NOT NULL) — total negative impact for group
- trend (REAL, NOT NULL) — trend calculation
- trend_group (REAL, NOT NULL) — group trend calculation

### Table: nodes

Purpose:
Storing information about discovered nodes in Truth Training network for tracking peer nodes, their addresses, types, availability and other discovery metadata.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique node identifier
- address (TEXT, NOT NULL, UNIQUE) — URL or ip:port of node (e.g. http://192.168.1.100:8080/api/v1)
- type (TEXT, NOT NULL) — node type (LAN, WIFI, GLOBAL, RELAY, CLIENT)
- reachable (INTEGER, NOT NULL) — availability flag (0/1)
- last_seen (INTEGER, NOT NULL) — time of last successful contact
- ttl (INTEGER, NOT NULL) — record lifetime before automatic deletion
- source (TEXT) — source of node discovery
- node_id (TEXT) — node public key (optional)
- created_at (INTEGER, NOT NULL) — timestamp of creation
- updated_at (INTEGER, NOT NULL) — timestamp of last update

### Table: node_ratings

Purpose:
Storing node reputation and trust for evaluating node reliability based on their activity and assessment accuracy.

Fields:
- node_id (TEXT, PK) — unique node identifier (public key)
- events_true (INTEGER, NOT NULL, DEFAULT 0) — number of true events
- events_false (INTEGER, NOT NULL, DEFAULT 0) — number of false events
- validations (INTEGER, NOT NULL, DEFAULT 0) — number of confirmations
- reused_events (INTEGER, NOT NULL, DEFAULT 0) — number of reused events
- trust_score (REAL, NOT NULL, DEFAULT 0.0) — overall trust rating (-1.0 .. 1.0)
- propagation_priority (REAL, NOT NULL, DEFAULT 0.0) — distribution priority (0.0 .. 1.0)
- last_updated (INTEGER, NOT NULL) — timestamp of last update

### Table: node_metrics

Purpose:
Monitoring node performance and status for tracking node performance metrics for synchronization optimization.

Fields:
- pubkey (TEXT, PK) — public key of the node
- last_seen (INTEGER, NOT NULL) — time of last contact
- relay_success_rate (REAL, NOT NULL, DEFAULT 0.0) — percentage of successful transfers
- quality_index (REAL, NOT NULL, DEFAULT 0.0) — quality index (0.0 .. 1.0) - continuity of trust indicator
- propagation_priority (REAL, NOT NULL, DEFAULT 0.0) — distribution priority (0.0 .. 1.0)

### Table: active_tokens

Purpose:
Managing authentication sessions based on JWT tokens for storing active refresh tokens allowing access token renewal without re-authentication.

Fields:
- public_key (TEXT, NOT NULL) — public key of the user
- refresh_token (TEXT, NOT NULL, UNIQUE) — refresh token value
- expires_at (INTEGER, NOT NULL) — expiration timestamp

### Table: peer_history

Purpose:
Storing peer synchronization history for tracking interaction history with each node for diagnostics and reliability analysis.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique history record identifier
- peer_url (TEXT, NOT NULL) — node URL
- last_sync (INTEGER) — time of last synchronization
- success_count (INTEGER, DEFAULT 0) — counter of successful attempts
- fail_count (INTEGER, DEFAULT 0) — counter of failed attempts
- last_quality_index (REAL, DEFAULT 0.0) — last quality index during synchronization
- last_trust_score (REAL, DEFAULT 0.0) — last trust score during synchronization

### Table: sync_log

Purpose:
Tracking low-level synchronization operations for tracking changes at individual record level, auditing and debugging synchronization, checking data integrity during exchange between nodes, tracking authenticity of changes via digital signatures.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique log record identifier
- op (TEXT, NOT NULL) — operation type (insert, update, delete)
- table_name (TEXT, NOT NULL) — name of the table affected
- record_id (TEXT, NOT NULL) — identifier of the record affected
- signature (TEXT) — signature of the synchronization participant
- public_key (TEXT) — public key of the synchronization participant
- created_at (INTEGER, NOT NULL) — timestamp of the operation

### Table: sync_logs

Purpose:
Tracking high-level synchronization attempts between nodes for network operation monitoring, analysis of synchronization success between nodes, diagnosis of connection and performance problems.

Fields:
- id (INTEGER, PK, AUTOINCREMENT) — unique log record identifier
- timestamp (INTEGER, NOT NULL) — timestamp of the synchronization event
- peer_url (TEXT, NOT NULL) — URL of the peer node
- mode (TEXT, NOT NULL) — synchronization mode
- status (TEXT, NOT NULL) — status of the synchronization
- details (TEXT, NOT NULL) — details of the synchronization process

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
---

### 9.5 Event movement dynamics

Event can move across plane over time:

E(t₀) ≠ E(t₁)

Causes:
- new judgments
- new impacts
- probability re-assessment

Truth(E, t) and Impact(E, t) are updated independently.

Truth can stabilize earlier or later than consequences.

1. True event without consequences
   → historical fact

2. False event with consequences
   → social catastrophe

3. Undefined event
   → active system learning zone

System amplifies attention to events:

attention(E) ∝ |∂I/∂t| × uncertainty(T)

---

### 9.9 Neural network analogy

- Truth axis → confidence weight
- Impact axis → activation output
- Event → neuron
- Event graph → neural network

System:
- does not delete events
- does not suppress axes
- allows coexistence of truth and harm

## 10. Temporal Dimension and Event Evolution

Events in Truth Training are not static objects.
Each event exists in time and changes its position in Truth × Impact space as new data arrives.

Event is defined as temporal process:

E(t) = { T(E, t), I(E, t) }

Where:
- t — discrete or continuous time
- T(E, t) — event truth in time
- I(E, t) — event impact in time
System must preserve entire history of changes, not overwriting previous event states.

### Table: event_state_history

Purpose:
Stores time slices of event state.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- truth_score (FLOAT)
- impact_score (FLOAT)
- judgments_count (INTEGER)
- impacts_count (INTEGER)
- recorded_at (TIMESTAMP)

Impact(E, t):
- can be monotonic (accumulative)
- can be impulsive
- can decay

Truth(E, t):
- not required to be monotonic
- allows revision
Between event appearance and its assessment there is temporal lag:

Δt_truth ≠ Δt_impact
Reasons:
- observation delay
- social propagation
- cognitive inertia

Event is considered stabilized if:

|∂T/∂t| < ε_T
|∂I/∂t| < ε_I

Where:
ε_T, ε_I — stabilization thresholds

---

### 10.7 Table `event_stability`

### Table: event_stability

Purpose:
Recording moment of event stabilization.

Fields:
- event_id (UUID, PK, FK → events.id)
- truth_stable (BOOLEAN)
- impact_stable (BOOLEAN)
- stabilized_at (TIMESTAMP)
Over time, trust in events can decrease without new confirmations.

Decay(T, t) ∝ e^(−λt)

Event can be reactivated:

- new judgments
- new impacts
- contextual changes
Time is not attribute of event, but separate dimension of its existence.

## 11. Context as Interpretation Space

In Truth Training, context is not auxiliary attribute of event.
Context — is space in which event acquires meaning, and its consequences and truth become interpretable.
Context C is defined as structured set of features:

C = { c₁, c₂, …, cₙ }

Context affects:
- event interpretation
- admissible judgments
- admissible impacts
Each event E does not exist in vacuum, but in context coordinates:

E ⊂ C

Context change:
- does not change event itself
- changes its meaning and assessments

Contexts can be:
- social
- temporal
- cultural
- legal
- technological
- domain
Judgment(E) is admissible only if it is consistent with context C.

Context:
- limits admissible interpretations
- prevents false extrapolations

### Table: contexts

Purpose:
Storing interpretation context templates.

Fields:
- id (UUID, PK)
- name (TEXT, UNIQUE)
- description (TEXT)
- domain (TEXT)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
One event can exist in multiple contexts simultaneously.

### Table: event_contexts

Purpose:
Event-context link (M:N).

Fields:
- event_id (UUID, FK → events.id)
- context_id (UUID, FK → contexts.id)
- PRIMARY KEY (event_id, context_id)

Contexts can:
- be supplemented
- be refined
- become obsolete

Contradictory judgments can be admissible if they belong to different contexts.
Context — is space of meanings, not event metadata.

## 12. Expert Functions and Assessment Heuristics

Truth Training does not use centralized expert system.
Expert role is performed by network participants, and their assessments form distributed interpretation field.

Expert function F is mapping:

F: (E, C) → J

where:
- E — event
- C — context
- J — judgment

Expert function can be:
- human
- algorithmic
- hybrid

Heuristic — is approximate rule used when complete information is absent.
In system:
- heuristics are not considered truth
- they are considered contribution to collective assessment

Heuristic types:
- logical
- statistical
- empirical
- contextual
- domain
- Table: expert_heuristics

Purpose:
Storing descriptions of heuristics and expert rules.

Fields:
- id (UUID, PK)
- name (TEXT)
- description (TEXT)
- domain (TEXT)
- weight (FLOAT)
- created_at (TIMESTAMP)
Heuristic is not applied directly to event, but through judgment.

### Table: judgment_heuristics

Purpose:
Linking judgments to applied heuristics.

Fields:
- judgment_id (UUID, FK → judgments.id)
- heuristic_id (UUID, FK → expert_heuristics.id)
- influence (FLOAT)
- PRIMARY KEY (judgment_id, heuristic_id)
Final event assessment is aggregated function of all applied heuristics and judgments.

Expert function:
- is not source of truth
- does not have priority over collective assessment
Conflict of heuristics is admissible and recorded in system as state of uncertainty.
Expertise — is contribution, not authority.

## 13. Assessment Aggregation and Collective Truth

In Truth Training, truth is not determined by single act.
It arises as result of aggregation of independent assessments in distributed system of observers.
Collective truth — is not value, but distribution of assessments in judgment space.

Truth is represented:
- as vector
- as density
- as dynamic state

Let:

J = {j₁, j₂, ..., jₙ} — set of judgments of event E

Then aggregated assessment T(E):

T(E) = A(J)
where A — aggregating function.

Possible aggregators:
- weighted average
- median
- quantile distribution
- bayesian aggregation
- neuron-like function

Each judgment has weight w,
depending on:
- node trust
- consistency with other assessments
- time
- applied heuristics

### Table: judgment_weights

Purpose:
Storing computed judgment weights.

Fields:
- judgment_id (UUID, PK, FK → judgments.id)
- weight (FLOAT)
- confidence (FLOAT)
- computed_at (TIMESTAMP)

Assessments become obsolete.
Truth has temporal dynamics.

w(t) = w₀ * e^(-λt)

Decay function is introduced:
w(t) = w₀ * e^(-λt)

Possible states:
- consensus
- polarization
- uncertainty
- conflict

### Table: truth_states

Purpose:
Recording current truth state by event.

Fields:
- event_id (UUID, PK, FK → events.id)
- state (ENUM)
- score (FLOAT)
- dispersion (FLOAT)
- updated_at (TIMESTAMP)
Polarization is recorded when assessments form multiple clusters.
Truth — is not result.
Truth — is process of alignment.

## 14. Constraints, Security and Anti-Manipulation Mechanisms

Truth Training is designed as system resistant to manipulation, centralization and substitution of collective truth.

System lacks:
- truth administrator
- global moderator
- centralized source of truth
Any attempt at centralization is considered architectural defect.
No node can have disproportionate influence on aggregated truth.

Influence is limited by:
- trust weight
- historical consistency
- temporal decay

### Table: node_trust_limits

Purpose:
Limiting maximum influence of nodes.

Fields:
- node_id (UUID, PK)
- max_weight (FLOAT)
- decay_factor (FLOAT)
- last_adjusted_at (TIMESTAMP)
System is resistant to creation of mass fake nodes.

Used:
- behavioral signatures
- temporal correlations
- network patterns
- cross-checking impact ↔ judgments

---

### 14.5 Table `node_behavior_signatures`

### Table: node_behavior_signatures

Purpose:
Storing behavioral characteristics of nodes.

Fields:
- node_id (UUID, PK)
- signature_hash (HASH)
- stability_score (FLOAT)
- anomaly_score (FLOAT)
- updated_at (TIMESTAMP)
Manipulation is determined not by content, but by behavior structure.

Examples:
- synchronous assessments
- repeating patterns
- sharp weight jumps
- unnatural consistency

### Table: manipulation_flags

Purpose:
Recording suspicious patterns.

Fields:
- id (UUID, PK)
- node_id (UUID, FK → nodes.id)
- event_id (UUID, FK → events.id)
- flag_type (ENUM)
- severity (INT)
- detected_at (TIMESTAMP)

Important:
System does NOT block users.

Possible reactions:
- weight reduction
- temporary influence decay
- enhanced verification
- increased consensus requirements

Events and judgments are not deleted.

Deletion is replaced by:
- annotations
- context
- subsequent assessments

Attack — is data.
Data — is signal.
System uses attacks as training material.

All security mechanisms are part of model, not external layer.

Security:
- formalized
- measurable
- reproducible

## 15. Cognitive and Neural Network Analogies of Mathematical Model

Mathematical model of Truth Training is intentionally designed to be isomorphic to natural human cognitive processes and principles of neural network operation.

Event (event) is equivalent to external stimulus exciting cognitive system.

In neural analogy:
- input signal
- sensory impulse
- feature vector

Impact reflects
space of possible event consequences.

Analogy:
- signal propagation
- influence on neighboring neurons
- formation of associative connections
- Impact is not binary — it is continuous.

Judgments represent
truth assessment of event
through collective confirmation.

Analogy:
- activation function
- neural network response
- result of signal interpretation

Impact and Judgments —
two independent axes,
intersecting only in limit.

This means:
- truth is not equal to consequence
- consequences do not prove truth
- their consistency is manifested over time

Each node — is autonomous neuron controlled by human.

Properties:
- local memory
- individual assessment function
- limited bandwidth
- learning through experience

Unlike AI, activation function is set by human.

Human:
- interprets signal
- applies context
- makes decision
- makes judgment

Set of nodes forms distributed neural network.

Characteristics:
- no center
- no global weights
- learning through correlations
- noise resistance

Relational database — is material form of network.

Table connections correspond to:
- synapses
- weights
- temporal delays

System does not use backpropagation.

Learning occurs through:
- accumulation of judgments
- weakening of contradictory nodes
- strengthening of consistent patterns

Error is not deleted.

It:
- is recorded
- is analyzed
- affects future weights

Truth — is not value, but process.

It:
- is refined
- is stabilized
- never freezes

Truth Training — is hybrid:
neural network + relational DB + human.

Each component is necessary.
Removing any destroys system.

## 16. Connection of Mathematical Model with Quality Gates

Mathematical model of Truth Training is not abstract theory — it directly determines quality criteria for code, data and system behavior.
Quality Gates serve as formalized mechanism for checking that implementation does not violate basic model principles.
Each Quality Gate — is invariant of mathematical model that cannot be violated without loss of system correctness.

Examples:
- database schema violation → destruction of cognitive connections
- enum inconsistency → distortion of assessment axes
- non-deterministic logic → loss of reproducibility

Quality Gates guarantee that database structure corresponds to formal model schema.

Checked:
- presence of all tables
- correctness of foreign keys
- consistency of types
- immutability of field semantics

This is critical because: relational database — physical body of neural network

Impact axis requires:
- impossibility of deleting consequences without reason
- strict binding to events
- preservation of temporal ordering
- support for multiple consequences

Quality Gate:
- event → impact integrity tests
- silent-delete prohibition
- TTL and lifecycle check

Judgments — truth axis — has strictest requirements.

Quality Gates check:
- cryptographic signature
- immutability of judgment after fixation
- correctness of aggregation
- absence of history overwrite

Violation of these rules is equivalent to:
- damage to neural network activation function

Since human is part of computing circuit, his actions must be verifiable.

Quality Gates ensure:
- impossibility of automatic judgment without human involvement
- transparency of assessment source
- reproducibility of assessment logic

System must remain distributed.

Quality Gates prohibit:
- hidden central states
- global mutable-structures
- implicit authorities

Each node:
- autonomous
- verifiable
- isolated

Without Quality Gates model exists only on paper.

With Quality Gates:
- model becomes executable
- architecture — verifiable
- development — safe

## 17. Formal Conclusion of Mathematical Model

Truth Training — is formally defined system consisting of:
- relational database
- distributed computing nodes
- human assessment functions
- strict quality invariants

Model states following:
- Truth is not value — it is process
- Consequences and truth — are orthogonal
- Human — is part of computing graph
- Decentralization — is not option, but requirement
- Error — is source of information, not failure

Model intentionally:
- does not seek instant truth
- is not optimized for speed
- does not centralize decision making

It is optimized for:
- resilience
- long-term correlation
- collective verifiability

Truth Training is not:
- voting
- rating
- expert system
- traditional neural network

This: cognitive infrastructure

Any implementation of Truth Training is considered correct if and only if:
- database structure corresponds to model
- Quality Gates are observed
- Impact and Judgments axes are independent
- human participation is not simulated

This document:
- is normative
- is used for architectural decisions
- serves as reference for PR and review
- cannot be changed without new specification

Truth Training — is not application. This:
- way of collective thinking
- formalized ethical mechanism
- distributed cognitive system

> **Truth is not what was said first.
> Truth is what survives circulation.**