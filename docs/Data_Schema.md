# Truth Training Database Schema

**Document Version:** v1.1.0  
**Status:** Specification  
**Updated:** 2025-12-28  
**Status:** Approved

This document describes the database schema for the Truth Training platform, detailing the structure of tables and their relationships in two main databases:

1. **truth_training.sqlite** - Core application database for storing events, impacts, judgments, and participant data
2. **discovery_nodes.sqlite** - Network discovery database for tracking peer nodes and network topology

## 1. Core Application Database (truth_training.sqlite)

### 1.1 Schema Management Tables

#### schema_version Table
Tracks database schema versions for version control and migration tracking.

| Column | Type | Description |
|--------|------|-------------|
| version | TEXT | Schema version (primary key) |
| applied_at | INTEGER | Time when version was applied |
| description | TEXT | Description of the version |

### 1.2 Knowledge Base Tables

#### category Table
Stores event category classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Category name (e.g., "Social", "Financial") |
| description | TEXT | Category description |

#### cause Table
Stores cause classifications with semantic valence.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Cause name (e.g., "Fear", "Benefit", "Mercy") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Cause description |

#### develop Table
Stores development/manifestation classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Manifestation name (e.g., "Concealment", "Manipulation") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Manifestation description |

#### effect Table
Stores effect/consequence classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Consequence name (e.g., "Distrust", "Disappointment") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Consequence description |

#### forma Table
Stores form of logic classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Form name (e.g., "Deception", "Truth", "Self-deception") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Form description |

#### time_axes Table
Defines independent time scales ('past','present','future') for analysis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| description | TEXT | Description of the time axis |
| time_type | TEXT | Type of time ('past' / 'present' / 'future') |
| created_at | INTEGER | Timestamp of creation |

#### context Table
Stores interpretation context templates with embedded knowledge base references.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Context name |
| category_id | INTEGER | Foreign key to category.id |
| forma_id | INTEGER | Foreign key to forma.id |
| cause_id | INTEGER | Foreign key to cause.id |
| develop_id | INTEGER | Foreign key to develop.id |
| effect_id | INTEGER | Foreign key to effect.id |
| description | TEXT | Context description |

### 1.3 Core Event Tables

#### truth_event Table
Main table storing events in the Truth Training system with embedded context fields.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| description | TEXT | Event description |
| global_id | TEXT | Global event identifier for network identification (unique) |
| participant_id | TEXT | Foreign key to participants.public_key |
| signature | TEXT | Cryptographic signature |
| category_id | INTEGER | Foreign key to category.id |
| forma_id | INTEGER | Foreign key to forma.id |
| cause_id | INTEGER | Foreign key to cause.id |
| develop_id | INTEGER | Foreign key to develop.id |
| effect_id | INTEGER | Foreign key to effect.id |
| vector | INTEGER | Event direction (0/1) 0-incoming; 1-outgoing |
| detected | INTEGER | Detection flag (NULL/0/1) |
| corrected | INTEGER | Correction flag (default 0) |
| timeline_id | INTEGER | Foreign key to event_timeline.id |
| code | INTEGER | Circulation code for distribution protocol (default 1) |
| collective_score | REAL | Local training/assessment metric |
| impact_score | REAL | Local impact metric |
| judgment_score | REAL | Local judgment metric |

#### event_timeline Table
Records the time range of events on each time axis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| time_axis_id | INTEGER | Foreign key to time_axes.id |
| t_start | INTEGER | Event start time on this axis |
| t_end | INTEGER | Event end time on this axis (if set) |

#### event_links Table
Describes logical and causal relationships between events.

| Column | Type | Description |
|--------|------|-------------|
| source_impact_id | INTEGER | Foreign key to truth_event.id (source event reference) |
| target_impact_id | INTEGER | Foreign key to truth_event.id (target event reference) |
| relation_type | TEXT | ENUM (basic / equal / foreign) |
| created_at | INTEGER | Timestamp of creation |

### 1.4 Collective Intelligence Tables

#### participants Table
Stores information about collective intelligence system participants.

| Column | Type | Description |
|--------|------|-------------|
| public_key | TEXT | Primary key (participant identifier - public key, unique) |
| signature | TEXT | Cryptographic signature |
| reputation_score | REAL | Participant's reputation score (range 0.0 .. 1.0, default 0.5) |
| reputation_history | INTEGER | Foreign key to reputation_history.id |
| total_judgment | INTEGER | Total number of judgment made (default 0) |
| accurate_judgment | INTEGER | Number of accurate judgment (default 0) |
| total_impact | INTEGER | Total number of impact made (default 0) |
| accurate_impact | INTEGER | Number of accurate impact (default 0) |
| created_at | INTEGER | Registration timestamp |
| last_activity | INTEGER | Timestamp of last activity |

#### event_ci Table
Storing events within collective intelligence system (Collective Intelligence Layer) for participant assessment, classification using event_type field, tracking event status (active, resolved, archived), and storing event result data in resolution_data field.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| created_by | INTEGER | Foreign key to truth_event.id |
| event_type | TEXT | Type of event ('impact', 'judgment', 'both') (default 'judgment') |
| status | TEXT | Event status ('active', 'resolved', 'archived') (default 'active') |
| old_status | TEXT | Event old status (default 'active') |
| resolution_data | TEXT | Data about event resolution ('unstable', 'suppose', 'consent') (default 'unstable') |
| created_at | INTEGER | Timestamp of creation |

#### judgment Table
Stores subjective assessments (judgments) of events issued by participants, representing assessment of specific event by specific participant.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| participant_id | INTEGER | Foreign key to participants.id |
| event_id | INTEGER | Foreign key to event_ci.id |
| assessment | REAL | Type of assessment |
| confidence_level | REAL | Confidence level of the assessment |
| reasoning | TEXT | Reasoning behind the judgment |
| consensus_ci | INTEGER | Foreign key to consensus_ci.id |
| judgment_weights | INTEGER | Foreign key to judgment_weights.id |
| timeline_id | INTEGER | Foreign key to judgment_timeline.id |

#### judgment_links Table
Describes logical and causal relationships between judgments.

| Column | Type | Description |
|--------|------|-------------|
| source_judgment_id | INTEGER | Foreign key to judgment.id (source judgment reference) |
| target_judgment_id | INTEGER | Foreign key to judgment.id (target judgment reference) |
| relation_type | TEXT | ENUM (supports / contradicts / refines) |
| created_at | INTEGER | Timestamp of creation |

#### judgment_weights Table
Defines weight of participants judgment in specific context. Weight reflects system's trust in participant.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| participant_id | INTEGER | Foreign key to participants.id |
| event_id | INTEGER | Foreign key to event_ci.id |
| weight | REAL | Participant's trust weight in judgment calculations |
| calculated_at | INTEGER | Timestamp of creation |

#### consensus_ci Table
Storing computed consensus on event based on participant judgment, representing collective opinion formed based on individual judgment and used for determining general event assessment result.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to event_ci.id |
| consensus_value | INTEGER | The consensus value reached |
| confidence_score | REAL | Confidence in the consensus |
| participant_count | INTEGER | Number of participants involved |
| calculated_at | INTEGER | Timestamp of calculation |
| algorithm_version | INTEGER | Version of algorithm used |

#### reputation_history Table
Tracking changes in collective intelligence system participant reputations for auditing and analyzing changes in participant reputations, understanding reasons for reputation changes, analyzing participant behavior and judgment impactiveness, and ensuring transparency of reputation system.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| old_reputation | REAL | Previous reputation score |
| new_reputation | REAL | New reputation score |
| change_reason | TEXT | Reason for reputation change |
| updated_at | INTEGER | Timestamp of update |

#### impact Table
Stores subjective assessments (impacts) of events issued by validators, representing observation and prediction of consequences from specific participant, not opinion on truthfulness.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to truth_event.id |
| type_id | INTEGER | Foreign key to effect.id (Reference knowledge-base) |
| trend | INTEGER | Impact trend 0/1/2/3 ("logical_negative"/"logical_positive"/"illogical_negative"/"illogical_positive") |
| value | INTEGER | Impact value (NULL/0/1 for measurable/negative/positive) |
| notes | TEXT | Additional notes about the impact |
| impact_metrics | INTEGER | Foreign key to impact_metrics.id |
| impact_predictions | INTEGER | Foreign key to impact_predictions.id |
| timeline_id | INTEGER | Foreign key to impact_timeline.id |

#### impact_links Table
Allows linking consequences to each other, forming chains of cause-and-impact relationships.

| Column | Type | Description |
|--------|------|-------------|
| source_impact_id | INTEGER | Foreign key to impact.id (source impact reference) |
| target_impact_id | INTEGER | Foreign key to impact.id (target impact reference) |
| relation_type | TEXT | ENUM (supports / contradicts / refines) |
| created_at | INTEGER | Timestamp of creation |

#### impact_metrics Table
Stores aggregated metrics of event consequences.

| Column | Type | Description |
|--------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to event_ci.id |
| total_magnitude | INTEGER | Overall impact significance |
| positive_ratio | INTEGER | Positive rating |
| negative_ratio | INTEGER | Negative rating |
| uncertainty | INTEGER | Undefined rating |
| calculated_at | INTEGER | Timestamp of calculated |

#### impact_predictions Table
Stores predicted consequences of events.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to event_ci.id |
| predicted_impact_type | INTEGER | Foreign key to effect.id |
| expected_strength | REAL | Expected expression, signal strength |
| probability | REAL | Participants confidence that the predicted effect occurred |
| horizon | REAL | Time interval, predicted time lag |
| created_at | INTEGER | Timestamp of calculated |

#### impact_timeline Table
Records the time range of impact on each time axis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| time_axis_id | INTEGER | Foreign key to time_axes.id (time axis reference) |
| t_start | INTEGER | Impact start time on this axis |
| t_end | INTEGER | Impact end time on this axis (if set) |

#### judgment_timeline Table
Records the time range of judgment on each time axis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| time_axis_id | INTEGER | Foreign key to time_axes.id (time axis reference) |
| t_start | INTEGER | Judgment start time on this axis |
| t_end | INTEGER | Judgment end time on this axis (if set) |

#### truth_state Table
Stores aggregated truth state of an event at a given point in time.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to event_ci.id |
| time_axis_id | INTEGER | Foreign key to time_axes.id |
| truth_state | TEXT | ENUM (active / resolved / archived) |
| truth_score | REAL | Aggregated truth score |
| dispersion | REAL | Dispersion in the truth score |
| confidence | REAL | Confidence in the truth score |
| calculated_at | INTEGER | Timestamp of calculation |

#### event_state_history Table
Records snapshots of an event state over time to track how event assessments evolve.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to event_ci.id |
| judgment_count | INTEGER | Number of judgments recorded so far |
| truth_score | REAL | Truth at this time |
| impact_count | INTEGER | Number of impacts recorded so far |
| impact_score | REAL | Impact at this time |
| recorded_at | INTEGER | Timestamp of recorded |

#### event_stability Table
Records when an event has become stable in truth and/or impact. This helps flag factors historical resolved misinformation that no longer require active monitoring.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to event_ci.id |
| truth_stable | INTEGER | BOOLEAN 0/1 - true if truth is stabilized |
| impact_stable | INTEGER | BOOLEAN 0/1 - true if impact is stabilized |
| stabilized_at | INTEGER | When stabilization was detected |

### 1.5 Collective Intelligence System Tables

#### event_projection Table
Stores the projection of an event in truth–impact space for classification.

| Column | Type | Description |
|--------|------|-------------|
| event_id | INTEGER | Foreign key to event_ci.id |
| truth_score | REAL | Aggregated truth score |
| impact_score | REAL | Aggregated impact score |
| quadrant | TEXT | Quadrant classification (Q1/Q2/Q3/Q4) |
| calculated_at | INTEGER | Timestamp of calculation |

#### statements Table
Aggregates local training metrics (csᵢ) for global processing in group training.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to truth_event.id |
| truth_score | REAL | Aggregated truth score (from local nodes) |
| created_at | INTEGER | Timestamp of creation |
| updated_at | INTEGER | Timestamp of last update |

#### group_ratings Table
Stores group assessment ratings for collective progress metrics.

| Column | Type | Description |
|--------|------|-------------|
| group_id | INTEGER | Primary key (auto-increment) |
| members | INTEGER | List of group member IDs |
| avg_score | REAL | Average score of the group |
| coherence | REAL | Coherence of group assessments |
| last_updated | INTEGER | Timestamp of last update |

#### progress_metrics Table
Aggregates system-wide progress metrics (event counts and reaction totals).

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| total_events | INTEGER | Total number of events processed |
| total_events_group | INTEGER | Total number of group events |
| total_positive_impacts | REAL | Total positive impacts observed |
| total_positive_impacts_group | REAL | Positive impacts in group events |
| total_negative_impacts | REAL | Total negative impacts observed |
| total_negative_impact_group | REAL | Negative impacts in group events |
| trend | REAL | Overall trend metric |
| trend_group | REAL | Trend metric for group events |
| last_updated | INTEGER | Timestamp of last update metric |

#### judgment_heuristics Table
Linking judgment to applied heuristics to track which heuristics influenced specific judgments.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| judgment_id | INTEGER | Foreign key to judgment.id |
| heuristic_id | INTEGER | Foreign key to expert_heuristics.id |
| influence | REAL | Influence or impact of the heuristic on the judgment |
| created_at | INTEGER | Timestamp of creation |

#### expert_heuristics Table
Storing descriptions of heuristics and expert rules for consistent application across the system.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Name of the heuristic |
| description | TEXT | Detailed description of the heuristic |
| domain | TEXT | Domain or context where heuristic applies |
| weight | REAL | Current weight/importance of the heuristic |
| confidence | REAL | Confidence level of the heuristic |
| proven_accuracy | REAL | Proven accuracy rate of the heuristic |
| created_at | INTEGER | Timestamp of creation |
| updated_at | INTEGER | Timestamp of last update |

---

## 2. Network Discovery Database (discovery_nodes.sqlite)

### 2.1 Schema Management Tables

#### schema_version Table
Tracks database schema versions for version control and migration tracking.

| Column | Type | Description |
|--------|------|-------------|
| version | TEXT | Schema version (primary key) |
| applied_at | INTEGER | Time when version was applied |
| description | TEXT | Description of the version |

### 2.2 Network and Synchronization Tables

#### discovery_nodes Table
Stores information about discovered nodes in Truth Training network.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| address | TEXT | Node address (URL or ip:port) |
| type | TEXT | Node type (LAN, WIFI, GLOBAL, RELAY, CLIENT) |
| reachable | INTEGER | Availability flag (0 = down, 1 = up) |
| last_seen | INTEGER | Time of last successful contact |
| ttl | INTEGER | Record lifetime before automatic deletion |
| source | TEXT | Source of node discovery |
| node_id | TEXT | Foreign key to participants.public_key |
| created_at | INTEGER | Timestamp of record creation |
| updated_at | INTEGER | Timestamp of last update |

#### node_ratings Table
Stores node reputation and trust for evaluating node reliability based on their activity and assessment accuracy.

| Column | Type | Description |
|--------|------|-------------|
| node_id | TEXT | Foreign key to discovery_nodes.node_id (public key of the node) |
| events_true | INTEGER | Number of true events (default 0) |
| events_false | INTEGER | Number of false events (default 0) |
| validations | INTEGER | Number of confirmations (default 0) |
| reused_events | INTEGER | Number of reused events (default 0) |
| trust_score | REAL | Overall trust rating (-1.0 .. 1.0, default 0.0) |
| propagation_priority | REAL | Distribution priority (0.0 .. 1.0, default 0.0) |
| last_updated | INTEGER | Timestamp of last update |

#### node_performance Table
Monitors node performance and status for tracking node performance metrics for synchronization optimization.

| Column | Type | Description |
|--------|------|-------------|
| pubkey | TEXT | Foreign key to discovery_nodes.node_id (public key of the node) |
| last_seen | INTEGER | Time of last contact |
| relay_success_rate | REAL | Percentage of successful transfers (default 0.0) |
| quality_index | REAL | Quality index (0.0 .. 1.0) - continuity of trust indicator (default 0.0) |
| propagation_priority | REAL | Distribution priority (0.0 .. 1.0) (default 0.0) |

#### node_trust_limits Table
Limiting maximum influence of nodes.

| Column | Type | Description |
|--------|------|-------------|
| node_id | TEXT | Foreign key to discovery_nodes.node_id |
| max_weight | REAL | Maximum allowable trust weight for this node |
| decay_factor | REAL | Per-period decay factor for that node's weight |
| small_constants | REAL | Small random constant in system time |
| last_adjusted_at | INTEGER | Timestamp when these limits were last updated |

#### node_behavior_signatures Table
Storing behavioral characteristics of nodes.

| Column | Type | Description |
|--------|------|-------------|
| node_id | TEXT | Foreign key to discovery_nodes.node_id |
| signature | TEXT | Cryptographic signature |
| stability_score | REAL | Stability score |
| anomaly_score | REAL | Anomaly score |
| updated_at | INTEGER | Timestamp of update |

#### manipulation_flags Table
Recording suspicious patterns.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| node_id | TEXT | Foreign key to discovery_nodes.node_id |
| flag_type | TEXT | Flag type |
| severity | INTEGER | Severity level |
| detected_at | INTEGER | Timestamp of detection |

#### sync_operations Table
Tracking low-level synchronization operations for tracking changes at individual record level, auditing and debugging synchronization, checking data integrity during exchange between nodes, tracking authenticity of changes via digital signatures.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| op | TEXT | Operation type (insert, update, delete) |
| table_name | TEXT | Name of the table affected |
| record_id | TEXT | Identifier of the record affected |
| signature | TEXT | Signature of the synchronization participant |
| public_key | TEXT | Foreign key to discovery_nodes.node_id |
| created_at | INTEGER | Timestamp of the operation |

#### sync_attempts Table
Records high-level synchronization events between nodes for monitoring network-wide operations and catching failures.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| timestamp | INTEGER | When the sync occurred |
| peer_url | TEXT | The peer node's URL |
| mode | TEXT | Sync mode or protocol (e.g., "full", "delta") |
| status | TEXT | Result status (e.g. "success" or error code) |
| details | TEXT | Additional info or error message |

### 2.3 Authentication and Session Management Tables

#### active_tokens Table
Managing authentication sessions based on JWT tokens for storing active refresh tokens allowing access token renewal without re-authentication.

| Column | Type | Description |
|--------|------|-------------|
| public_key | TEXT | Foreign key to discovery_nodes.node_id |
| refresh_token | TEXT | Refresh token value |
| expires_at | INTEGER | Expiration timestamp |

#### peer_synchronization Table
Storing peer synchronization history for tracking interaction history with each node for diagnostics and reliability analysis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| peer_url | TEXT | Foreign key to discovery_nodes.address |
| mode | TEXT | Synchronization mode |
| status | TEXT | Status of the synchronization |
| details | TEXT | Details of the synchronization process |
| last_sync | INTEGER | Time of last synchronization |
| success_count | INTEGER | Counter of successful attempts (default 0) |
| fail_count | INTEGER | Counter of failed attempts (default 0) |
| last_quality_index | REAL | Last quality index during synchronization (default 0.0) |
| last_trust_score | REAL | Last trust score during synchronization (default 0.0) |

## Privacy and Security Notes

This schema is designed with privacy in mind. The system does not store user behavior logs or track user interactions beyond what is necessary for the core functionality. All data stored in the database (events, judgments, contexts) is anonymized and cannot be traced back to individual users directly.

The system implements cryptographic verification through signatures and public keys to ensure data integrity while maintaining user anonymity.
