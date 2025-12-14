# Project Documentation Update - Update Constitution
# Formalized - Model and Database Schema
## Truth Training

**Document Version:** 1.0
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
Truth Training system is based on the following principles:

1. Truth is not stored as a fixed value —
   it is calculated as an aggregated function of multiple judgments.

2. Each event is considered in two independent dimensions:
   - impact axis
   - truth axis

3. System allows parallel, conflicting assessments,
   which may stabilize or disappear over time.

4. Each assessment is local and contextual,
   while global values are statistical in nature.
Model reflects "one-to-many" principle:

- one event → multiple interpretations;
- one source → multiple consequences;
- one observation → multiple judgments.

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
Stores information about system participants who form judgments, initiate events and confirm consequences.

Fields:
- id (UUID, PK) — unique participant identifier
- public_key (TEXT, UNIQUE) — participant's public key
- created_at (TIMESTAMP) — registration time
- status (ENUM) — participant status (active, suspended, revoked)

Notes:
- Participant is not tied to identity
- Authentication is based on cryptographic key

### Table: contexts

Purpose:
Describes context within which event assessment takes place. Context affects admissible consequences and interpretations.

Fields:
- id (UUID, PK)
- name (TEXT)

## 3. Events and Consequence Axis (Impact Axis)

Event is central entity of Truth Training model.
It represents a fixed fact or statement around which truth assessments and observable consequences are formed.

### Table: events

Purpose:
Stores atomic system events. Event is immutable object and serves as intersection point of truth and consequence axes.

Fields:
- id (UUID, PK) — unique event identifier
- author_id (UUID, FK → participants.id) — event initiator
- context_id (UUID, FK → contexts.id) — event context
- payload_hash (TEXT) — hash of event content
- created_at (TIMESTAMP) — event recording time
- visibility (ENUM) — visibility scope (public, restricted, private)

Notes:
- Event content is stored outside DB or in encrypted form
- Hash is used for integrity check
- Event is not changed after creation

### Table: impacts

Purpose:
Records consequences caused by event.
Consequences reflect "Impact" axis and can be both immediate and delayed.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- observer_id (UUID, FK → participants.id)
- impact_type (ENUM) — consequence type (system, social, economic, logical)
- magnitude (FLOAT) — impact magnitude
- direction (ENUM) — positive / negative / neutral
- recorded_at (TIMESTAMP) — consequence recording time

Notes:
- Consequence is always linked to specific event
- One event can have multiple consequences
- Consequences are not aggregated at DB level

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
Stores individual participant judgments about event truth.
Each judgment is an act of interpretation, not a fact.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- participant_id (UUID, FK → participants.id)
- judgment_value (ENUM) — true / false / uncertain / abstain
- confidence (FLOAT) — confidence level (0.0 – 1.0)
- created_at (TIMESTAMP)

Constraints:
- (event_id, participant_id) — unique pair

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

### Table: judgment_aggregation

Purpose:
Stores aggregated truth values of event based on multiple judgments.

Fields:
- event_id (UUID, PK, FK → events.id)
- collective_score (FLOAT)
- entropy (FLOAT)
- total_judgments (INTEGER)
- last_updated (TIMESTAMP)

Notes:
- collective_score ∈ [-1.0; 1.0]
- entropy reflects degree of opinion divergence

### Table: judgment_history

Purpose:
Records evolution of event truth over time.

Fields:
- id (UUID, PK)
- event_id (UUID, FK → events.id)
- collective_score (FLOAT)
- entropy (FLOAT)
- snapshot_at (TIMESTAMP)

Notes:
- Used for learning and dynamics analysis
- Not involved in online calculations
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