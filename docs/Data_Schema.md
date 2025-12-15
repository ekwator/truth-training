# Truth Training Database Schema

This document describes the database schema for the Truth Training platform, detailing the structure of tables and their relationships in two main databases:

1. **truth_training.sqlite** - Core application database for storing events, impacts, judgments, and user data
2. **discovery_nodes.sqlite** - Network discovery database for tracking peer nodes and network topology

## 1. Core Application Database (truth_training.sqlite)

### 1.1 Knowledge Base Tables

#### category Table
Stores event category classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Category name (e.g., "Social", "Financial") |
| description | TEXT | Category description |

#### cause Table
Stores cause classifications with semantic valence.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Cause name (e.g., "Fear", "Benefit", "Mercy") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Cause description |

#### develop Table
Stores development/manifestation classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Manifestation name (e.g., "Concealment", "Manipulation") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Manifestation description |

#### effect Table
Stores effect/consequence classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Consequence name (e.g., "Distrust", "Disappointment") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Consequence description |

#### forma Table
Stores form of logic classifications.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Form name (e.g., "Deception", "Truth", "Self-deception") |
| quality | INTEGER | Semantic valence (0/1 for negative/positive) |
| description | TEXT | Form description |

#### context Table
Stores interpretation context templates with embedded knowledge base references.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Context name |
| category_id | INTEGER | Foreign key to category.id |
| forma_id | INTEGER | Foreign key to forma.id |
| cause_id | INTEGER | Foreign key to cause.id |
| develop_id | INTEGER | Foreign key to develop.id |
| effect_id | INTEGER | Foreign key to effect.id |
| description | TEXT | Context description |

### 1.2 Core Event Tables

#### truth_events Table
Main table storing events in the Truth Training system with embedded context fields.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| description | TEXT | Event description |
| global_id | TEXT | Global event identifier for network identification (unique) |
| user_uuid | TEXT | Author's public key |
| category_id | INTEGER | Foreign key to category.id (nullable) |
| forma_id | INTEGER | Foreign key to forma.id (nullable) |
| cause_id | INTEGER | Foreign key to cause.id (nullable) |
| develop_id | INTEGER | Foreign key to develop.id (nullable) |
| effect_id | INTEGER | Foreign key to effect.id (nullable) |
| vector | INTEGER | Event direction (0/1 for incoming/outgoing) |
| detected | INTEGER | Detection flag (NULL/0/1) |
| corrected | INTEGER | Correction flag (0/1, default 0) |
| timestamp_start | INTEGER | Event start time (Unix timestamp) |
| timestamp_end | INTEGER | Event end time (Unix timestamp, nullable) |
| code | INTEGER | Circulation code for distribution protocol (default 1) |
| collective_score | REAL | Local training/assessment metric |
| impact_score | REAL | Local impact metric |
| judgments_score | REAL | Local judgments metric |
| signature | TEXT | Cryptographic signature |
| public_key | TEXT | Public key for verification |

#### impact_type Table
Classifies types of event impacts (reputational, financial, moral, etc.).

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | TEXT | Impact type name |
| description | TEXT | Impact type description |

#### impact Table
Stores subjective assessments (impacts) of events issued by validators, representing observation and prediction of consequences from specific participant, not opinion on truthfulness.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to truth_events.id |
| type_id | INTEGER | Foreign key to impact_type.id |
| value | INTEGER | Impact value (0/1 for negative/positive) |
| notes | TEXT | Additional notes about the impact |
| created_at | INTEGER | Timestamp of impact recording |
| signature | TEXT | Cryptographic signature for verification |
| public_key | TEXT | Public key for verification |

### 1.3 Collective Intelligence Tables

#### participants Table
Stores information about collective intelligence system participants.

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT | Primary key (participant identifier - public key) |
| public_key | TEXT | Unique public key (unique constraint) |
| reputation_score | REAL | Participant's reputation score (range 0.0 .. 1.0, default 0.5) |
| total_judgments | INTEGER | Total number of judgments made (default 0) |
| accurate_judgments | INTEGER | Number of accurate judgments (default 0) |
| created_at | INTEGER | Registration timestamp |
| last_activity | INTEGER | Timestamp of last activity |

#### events_ci Table
Storing events within collective intelligence system (Collective Intelligence Layer) for participant assessment, classification using event_type field, tracking event status (active, resolved, archived), and storing event result data in resolution_data field.

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT | Primary key (unique event identifier) |
| title | TEXT | Event title |
| description | TEXT | Event description |
| event_type | TEXT | Type of event |
| created_by | TEXT | Foreign key to participants.id (creator of the event) |
| created_at | INTEGER | Creation timestamp |
| status | TEXT | Event status (default 'active') |
| resolution_data | TEXT | Data about event resolution |

#### judgments Table
Same structure as in truth_training.sqlite for consistency across both databases.

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT | Primary key (unique judgment identifier) |
| participant_id | TEXT | Foreign key to participants.id (participant who made the judgment) |
| event_id | TEXT | Foreign key to events_ci.id (event being judged) |
| assessment | TEXT | Type of assessment (e.g., true, false, uncertain) |
| confidence_level | REAL | Confidence level of the assessment (0.0 .. 1.0) |
| reasoning | TEXT | Reasoning behind the judgment |
| submitted_at | INTEGER | Timestamp of submission |
| signature | TEXT | Cryptographic signature |
| UNIQUE constraint | (participant_id, event_id) | Each participant can have only one judgment per event |

#### consensus_ci Table
Same structure as in truth_training.sqlite for consistency across both databases.

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT | Primary key (unique consensus identifier) |
| event_id | TEXT | Foreign key to events_ci.id (event being evaluated) |
| consensus_value | TEXT | The consensus value reached |
| confidence_score | REAL | Confidence level in the consensus (0.0 .. 1.0) |
| participant_count | INTEGER | Number of participants involved in consensus |
| calculated_at | INTEGER | Timestamp of calculation |
| algorithm_version | TEXT | Version of algorithm used |

#### reputation_history Table
Same structure as in truth_training.sqlite for consistency across both databases.

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT | Primary key (unique history record identifier) |
| participant_id | TEXT | Foreign key to participants.id (participant whose reputation changed) |
| old_reputation | REAL | Previous reputation score |
| new_reputation | REAL | New reputation score |
| change_reason | TEXT | Reason for reputation change |
| event_id | TEXT | Associated event ID (optional) |
| updated_at | INTEGER | Timestamp of update |

### 1.4 Network and Synchronization Tables

#### nodes Table
Stores information about discovered nodes in Truth Training network.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| address | TEXT | Node address (URL or ip:port) |
| type | TEXT | Node type (LAN, WIFI, GLOBAL, RELAY, CLIENT) |
| reachable | INTEGER | Availability flag (0/1) |
| last_seen | INTEGER | Time of last successful contact |
| ttl | INTEGER | Record lifetime before automatic deletion |
| source | TEXT | Source of node discovery |
| node_id | TEXT | Node public key (optional) |
| created_at | INTEGER | Timestamp of creation |
| updated_at | INTEGER | Timestamp of last update |

#### node_ratings Table
Stores node reputation and trust for evaluating node reliability based on their activity and assessment accuracy.

| Column | Type | Description |
|--------|------|-------------|
| node_id | TEXT | Primary key (node identifier - public key) |
| events_true | INTEGER | Number of true events (default 0) |
| events_false | INTEGER | Number of false events (default 0) |
| validations | INTEGER | Number of confirmations (default 0) |
| reused_events | INTEGER | Number of reused events (default 0) |
| trust_score | REAL | Overall trust rating (-1.0 .. 1.0, default 0.0) |
| propagation_priority | REAL | Distribution priority (0.0 .. 1.0, default 0.0) |
| last_updated | INTEGER | Timestamp of last update |

#### node_metrics Table
Monitors node performance and status for tracking node performance metrics for synchronization optimization.

| Column | Type | Description |
|--------|------|-------------|
| pubkey | TEXT | Primary key (node's public key) |
| last_seen | INTEGER | Time of last contact |
| relay_success_rate | REAL | Percentage of successful transfers (default 0.0) |
| quality_index | REAL | Quality index (0.0 .. 1.0) - continuity of trust indicator (default 0.0) |
| propagation_priority | REAL | Distribution priority (0.0 .. 1.0) (default 0.0) |

#### sync_log Table
Same structure as in truth_training.sqlite for consistency across both databases.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| op | TEXT | Operation type (insert/update/delete) |
| table_name | TEXT | Name of the table affected |
| record_id | TEXT | Identifier of the record affected |
| signature | TEXT | Cryptographic signature of synchronization participant |
| public_key | TEXT | Public key of synchronization participant |
| created_at | INTEGER | Timestamp of the operation |

#### sync_logs Table
Same structure as in truth_training.sqlite for consistency across both databases.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| timestamp | INTEGER | Timestamp of synchronization event |
| peer_url | TEXT | URL of the peer node |
| mode | TEXT | Synchronization mode |
| status | TEXT | Status of synchronization |
| details | TEXT | Details of synchronization process |

### 1.5 Authentication and Session Management Tables

#### active_tokens Table
Managing authentication sessions based on JWT tokens for storing active refresh tokens allowing access token renewal without re-authentication.

| Column | Type | Description |
|--------|------|-------------|
| public_key | TEXT | User's public key |
| refresh_token | TEXT | Refresh token value (unique) |
| expires_at | INTEGER | Expiration timestamp for the token |

#### app_config Table
Service table for application configuration settings.

| Column | Type | Description |
|--------|------|-------------|
| key | TEXT | Configuration key (primary key) |
| value | TEXT | Configuration value |

### 1.6 Analytics and Progress Tracking Tables

#### statements Table
Aggregating local csᵢ for transfer to global level for calculating group training, calculated based on all events and cs_i field (truth_events.collective_score).

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| event_id | INTEGER | Foreign key to truth_events.id |
| text | TEXT | Statement text |
| context | TEXT | Statement context |
| truth_score | REAL | Aggregated truth score |
| created_at | INTEGER | Creation timestamp |
| updated_at | INTEGER | Update timestamp |
| signature | TEXT | Cryptographic signature |
| public_key | TEXT | Public key for verification |

#### group_ratings Table
Storing group ratings for collective assessment of truth training progress.

| Column | Type | Description |
|--------|------|-------------|
| group_id | TEXT | Primary key (unique group identifier) |
| members | TEXT | List of group members |
| avg_score | REAL | Average score of the group |
| coherence | REAL | Coherence of the group's assessments |
| last_updated | INTEGER | Timestamp of last update |

#### progress_metrics Table
Aggregating individual, group, and comparative trends for system metrics.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| timestamp | INTEGER | Timestamp of the metric |
| total_events | INTEGER | Total number of events |
| total_events_group | INTEGER | Total number of group events |
| total_positive_impact | REAL | Total positive impact |
| total_positive_impact_group | REAL | Total positive impact for group |
| total_negative_impact | REAL | Total negative impact |
| total_negative_impact_group | REAL | Total negative impact for group |
| trend | REAL | Trend calculation |
| trend_group | REAL | Group trend calculation |

#### peer_history Table
Storing peer synchronization history for tracking interaction history with each node for diagnostics and reliability analysis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| peer_url | TEXT | URL of the peer node |
| last_sync | INTEGER | Time of last synchronization |
| success_count | INTEGER | Counter of successful synchronization attempts (default 0) |
| fail_count | INTEGER | Counter of failed synchronization attempts (default 0) |
| last_quality_index | REAL | Last quality index during synchronization (default 0.0) |
| last_trust_score | REAL | Last trust score during synchronization (default 0.0) |

---

## 9. Network Discovery Database (discovery_nodes.sqlite)

### 9.1 Schema Management Tables

#### schema_version Table
Tracks database schema versions for version control and migration tracking.

| Column | Type | Description |
|--------|------|-------------|
| version | TEXT | Schema version (primary key) |
| applied_at | INTEGER | Time when version was applied |
| description | TEXT | Description of the version |

### 9.2 Node Discovery and Management Tables

#### discovery_nodes Table
Stores information about discovered nodes in the Truth Training network for tracking peer nodes, their addresses, types, availability and other discovery metadata.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| address | TEXT | Node address (URL or ip:port) |
| type | TEXT | Node type (LAN, WIFI, GLOBAL, RELAY, CLIENT) |
| reachable | INTEGER | Availability flag (0/1) |
| last_seen | INTEGER | Time of last successful contact |
| ttl | INTEGER | Record lifetime before automatic deletion |
| source | TEXT | Source of node discovery |
| node_id | TEXT | Node public key (optional) |
| created_at | INTEGER | Timestamp of creation |
| updated_at | INTEGER | Timestamp of last update |

### 9.3 Network Performance and Monitoring Tables

#### node_performance Table
Monitoring node performance and status for tracking node performance metrics for synchronization optimization.

| Column | Type | Description |
|--------|------|-------------|
| pubkey | TEXT | Primary key (node's public key) |
| last_seen | INTEGER | Time of last contact |
| relay_success_rate | REAL | Percentage of successful transfers (default 0.0) |
| quality_index | REAL | Quality index (0.0 .. 1.0) - continuity of trust indicator (default 0.0) |
| propagation_priority | REAL | Distribution priority (0.0 .. 1.0) (default 0.0) |

#### peer_synchronization Table
Storing peer synchronization history for tracking interaction history with each node for diagnostics and reliability analysis.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| peer_url | TEXT | URL of the peer node |
| last_sync | INTEGER | Time of last synchronization |
| success_count | INTEGER | Counter of successful synchronization attempts (default 0) |
| fail_count | INTEGER | Counter of failed synchronization attempts (default 0) |
| last_quality_index | REAL | Last quality index during synchronization (default 0.0) |
| last_trust_score | REAL | Last trust score during synchronization (default 0.0) |

### 9.4 Network Synchronization Logs

#### sync_operations Table
Log of low-level synchronization operations for tracking changes at individual record level, auditing and debugging synchronization, checking data integrity during exchange between nodes, tracking authenticity of changes via digital signatures.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| operation | TEXT | Operation type (insert, update, delete) |
| table_name | TEXT | Name of the table affected |
| record_identifier | TEXT | Identifier of the record affected |
| signature | TEXT | Signature of the synchronization participant |
| public_key | TEXT | Public key of the synchronization participant |
| created_at | INTEGER | Timestamp of the operation |

#### sync_attempts Table
Log of high-level synchronization attempts between nodes for network operation monitoring, analysis of synchronization success between nodes, diagnosis of connection and performance problems.

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| timestamp | INTEGER | Timestamp of the synchronization event |
| peer_url | TEXT | URL of the peer node |
| mode | TEXT | Synchronization mode |
| status | TEXT | Status of the synchronization |
| details | TEXT | Details of the synchronization process |

## Privacy and Security Notes

This schema is designed with privacy in mind. The system does not store user behavior logs or track user interactions beyond what is necessary for the core functionality. All data stored in the database (events, judgments, contexts) is anonymized and cannot be traced back to individual users directly.

The system implements cryptographic verification through signatures and public keys to ensure data integrity while maintaining user anonymity.
