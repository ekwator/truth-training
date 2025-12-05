# Functional Specification: Rust Core Modules

**Version:** v1.0.0  
**Spec ID:** 22  
**Updated:** 2025-01-XX

Use `/spec` as the primary decision source before reading `/docs`.

## Overview

This document provides a comprehensive functional specification for all Rust modules that form the core of Truth Training v1.0.0. It covers modules in `app/`, `core/`, `src/`, and `ui/desktop/src-tauri/` (Rust/Tauri backend), organized by module and responsibility.

## Related Documents

- [Architecture Overview](03-architecture.md)
- [Cross-Platform Architecture](18-cross-platform-architecture.md)
- [Data Model](04-data-model.md)
- [API Specification](05-api.md)
- [P2P & Sync](08-p2p-sync.md)
- [Expert System](06-expert-system.md)
- [Technical Specification](../docs/Technical_Specification.md)
- [Data Schema](../docs/Data_Schema.md)

## Core Library (`core/`)

### Module: `core/src/lib.rs`

**Purpose:** Main library entry point and module exports.

**Functions:**
- Re-exports all public modules and types
- Provides configuration constants (TTL, intervals, timeouts)
- Exports storage functions, models, and collective intelligence functions

**Key Exports:**
- `storage::*` - Database operations
- `models::*` - Data models
- `expert_simple::*` - Expert system algorithms
- `trust_propagation::*` - Trust propagation logic
- `collective_intelligence::*` - Collective intelligence layer
- Configuration constants (discovery timing, TTL values, health check settings)

### Module: `core/src/storage.rs`

**Purpose:** Database operations and SQLite schema management.

**Key Functions:**

#### Database Initialization
- `init_db(conn: &Connection) -> Result<(), CoreError>` - Creates all database tables (idempotent)
- `open_db(path: &str) -> Result<Connection, CoreError>` - Opens SQLite database connection

#### Truth Events
- `add_truth_event(conn: &Connection, event: NewTruthEvent) -> Result<i64, CoreError>` - Creates new truth event
- `get_truth_event(conn: &Connection, id: i64) -> Result<Option<TruthEvent>, CoreError>` - Retrieves event by ID
- `set_event_detected(conn: &Connection, id: i64, detected: bool, end: Option<i64>, corrected: bool) -> Result<(), CoreError>` - Updates event detection status
- `list_truth_events(conn: &Connection, limit: Option<i64>, offset: Option<i64>) -> Result<Vec<TruthEvent>, CoreError>` - Lists events with pagination

#### Statements
- `add_statement(conn: &Connection, stmt: NewStatement) -> Result<i64, CoreError>` - Creates new statement
- `get_statement(conn: &Connection, id: i64) -> Result<Option<Statement>, CoreError>` - Retrieves statement by ID
- `list_statements(conn: &Connection, event_id: Option<i64>) -> Result<Vec<Statement>, CoreError>` - Lists statements, optionally filtered by event

#### Impacts
- `add_impact(conn: &Connection, event_id: i64, type_id: i64, positive: bool, notes: Option<String>) -> Result<i64, CoreError>` - Creates impact record
- `get_impact(conn: &Connection, id: i64) -> Result<Option<Impact>, CoreError>` - Retrieves impact by ID
- `list_impacts(conn: &Connection, event_id: Option<i64>) -> Result<Vec<Impact>, CoreError>` - Lists impacts, optionally filtered by event

#### Progress Metrics
- `recalc_progress_metrics(conn: &Connection, timestamp: i64) -> Result<i64, CoreError>` - Recalculates and stores progress metrics
- `get_latest_progress_metrics(conn: &Connection) -> Result<Option<ProgressMetrics>, CoreError>` - Retrieves latest metrics

#### Knowledge Base
- `seed_knowledge_base(conn: &mut Connection, locale: &str) -> Result<(), CoreError>` - Seeds reference knowledge base (categories, formas, causes, develops, effects) with locale-specific data
  - **Supported locales:** `"ru"` (Russian) and `"en"` (English)
  - **Locale-aware seeding:** Populates knowledge base tables (category, forma, cause, develop, effect, impact_type) with translated strings based on locale parameter
  - **Default behavior:** If unsupported locale is provided, returns `CoreError::InvalidArg`
  - **Usage:** Called during database initialization (`init_app`) and when locale changes to ensure knowledge base matches current UI language
- `get_category(conn: &Connection, id: i64) -> Result<Option<Category>, CoreError>` - Retrieves category by ID
- `list_categories(conn: &Connection) -> Result<Vec<Category>, CoreError>` - Lists all categories

#### Context Templates
- `add_context(conn: &Connection, ctx: NewContext) -> Result<i64, CoreError>` - Creates context template
- `get_context(conn: &Connection, id: i64) -> Result<Option<Context>, CoreError>` - Retrieves context by ID
- `get_context_by_name(conn: &Connection, name: &str) -> Result<Option<Context>, CoreError>` - Retrieves context by name
- `list_contexts(conn: &Connection) -> Result<Vec<Context>, CoreError>` - Lists all contexts
- `match_context(conn: &Connection, category_id: Option<i64>, forma_id: Option<i64>, cause_id: Option<i64>, develop_id: Option<i64>, effect_id: Option<i64>) -> Result<Option<Context>, CoreError>` - Matches context by embedded fields

#### Node Management
- `add_node(conn: &Connection, node: NewNode) -> Result<i64, CoreError>` - Adds or updates peer node
- `get_node(conn: &Connection, id: i64) -> Result<Option<Node>, CoreError>` - Retrieves node by ID
- `get_node_by_address(conn: &Connection, address: &str) -> Result<Option<Node>, CoreError>` - Retrieves node by address
- `list_nodes(conn: &Connection, filter: Option<NodeFilter>) -> Result<Vec<Node>, CoreError>` - Lists nodes with optional filtering
- `patch_node(conn: &Connection, id: i64, patch: NodePatch) -> Result<(), CoreError>` - Updates node fields
- `delete_node(conn: &Connection, id: i64) -> Result<(), CoreError>` - Deletes node

#### Node Ratings & Trust
- `add_node_rating(conn: &Connection, node_id: i64, rater_id: i64, trust_score: f32, notes: Option<String>) -> Result<i64, CoreError>` - Creates node rating
- `get_node_ratings(conn: &Connection, node_id: Option<i64>) -> Result<Vec<NodeRating>, CoreError>` - Lists ratings, optionally filtered by node
- `recalc_collective_truth(conn: &Connection) -> Result<(), CoreError>` - Recalculates collective truth scores

#### Sync Logs
- `add_sync_log(conn: &Connection, peer_address: &str, direction: &str, status: &str, message_count: Option<i64>) -> Result<i64, CoreError>` - Records sync operation
- `list_sync_logs(conn: &Connection, limit: Option<i64>) -> Result<Vec<SyncLog>, CoreError>` - Lists recent sync logs
- `clear_sync_logs(conn: &Connection) -> Result<(), CoreError>` - Clears all sync logs

#### Peer History
- `record_peer_sync_attempt(conn: &Connection, peer_address: &str, success: bool, message_count: Option<i64>, error: Option<&str>) -> Result<(), CoreError>` - Records peer sync attempt
- `get_peer_history(conn: &Connection, peer_address: Option<&str>) -> Result<Vec<PeerHistoryEntry>, CoreError>` - Retrieves peer history
- `get_peer_summary(conn: &Connection) -> Result<PeerSummary, CoreError>` - Aggregates peer statistics

#### Graph Data
- `get_graph_data(conn: &Connection, filter: Option<NodeFilter>) -> Result<GraphData, CoreError>` - Generates network graph data with nodes and links
- `get_graph_summary(conn: &Connection) -> Result<GraphSummary, CoreError>` - Generates network summary statistics

**Shared Responsibilities:**
- SQLite connection management
- Transaction handling
- Error conversion (rusqlite::Error -> CoreError)
- Data validation and constraint enforcement

### Module: `core/src/models.rs`

**Purpose:** Data models and type definitions.

**Key Types:**

#### Events
- `TruthEvent` - Complete truth event with all fields
- `NewTruthEvent` - Input structure for creating events
- `EventStatus` - Event status enumeration

#### Statements
- `Statement` - Statement record with truth score
- `NewStatement` - Input structure for creating statements

#### Impacts
- `Impact` - Impact record with type and notes
- `ImpactType` - Impact type enumeration

#### Contexts
- `Context` - Context template with embedded fields
- `NewContext` - Input structure for creating contexts
- `Category`, `Forma`, `Cause`, `Develop`, `Effect` - Knowledge base entities

#### Nodes
- `Node` - Peer node with discovery metadata
- `NewNode` - Input structure for creating nodes
- `NodePatch` - Partial node update structure
- `NodeFilter` - Filtering criteria for node queries
- `NodeType` - Node type enumeration (Hub, Leaf)
- `NodeSource` - Discovery source enumeration (LAN, Global, Manual)
- `NodeRating` - Trust rating for a node

#### Progress Metrics
- `ProgressMetrics` - Aggregated progress statistics

#### Graph & Network
- `GraphData` - Network graph structure (nodes + links)
- `GraphNode` - Graph node representation
- `GraphLink` - Graph link representation
- `GraphSummary` - Network summary statistics
- `PeerHistoryEntry` - Peer sync history entry
- `PeerSummary` - Aggregated peer statistics

#### Authentication
- `RbacUser` - User with role-based access control
- `UserRole` - Role enumeration (admin, node, observer)

#### Errors
- `CoreError` - Unified error type for all core operations

**Shared Responsibilities:**
- Serialization/deserialization (Serde)
- Type safety and validation
- Database schema mapping

### Module: `core/src/expert_simple.rs`

**Purpose:** Expert system algorithms for truth assessment.

**Key Functions:**

#### Question Generation
- `questions_for_context(context_name: &str) -> Vec<Question>` - Generates expert questions for a context
  - Returns standardized question set (src_independent, alt_hypothesis, consistency, etc.)
  - Questions have weights, truth_bias, and type (YesNo, TriState, Scale1to5)

#### Answer Evaluation
- `evaluate_answers(questions: &[Question], answers: &Answers) -> Suggestion` - Evaluates user answers
  - Calculates weighted score (-1.0 to +1.0)
  - Computes confidence level (0.0 to 1.0)
  - Generates rationale explanation
  - Suggests detected status if confidence is high

**Key Types:**
- `Question` - Expert question with id, text, kind, weight, truth_bias
- `QuestionKind` - Question type enumeration (YesNo, TriState, Scale1to5)
- `Answers` - HashMap of question_id -> answer value
- `Suggestion` - Evaluation result with score, confidence, rationale, suggested_detected

**Shared Responsibilities:**
- Heuristic-based truth assessment
- Weighted scoring algorithms
- Confidence calculation

### Module: `core/src/trust_propagation.rs`

**Purpose:** Trust propagation and reputation management.

**Key Functions:**

#### Trust Blending
- `propagate_from_remote(local_trust: f32, remote_trust: f32) -> f32` - Blends local and remote trust scores
  - Formula: `local * 0.8 + remote * 0.2`
  - No time-based decay (fairness to offline nodes)

#### Quality Index
- `compute_quality_index(conn: &Connection, node_id: i64) -> Result<f32, CoreError>` - Calculates quality index (0.0-1.0)
  - Adaptive formula with EMA (Exponential Moving Average)
  - Continuity indicator for node reliability
- `blend_quality(local: f32, remote: f32) -> f32` - Blends quality indices

#### Propagation Priority
- `compute_propagation_priority(trust_norm: f32, quality_index: f32, relay_success_rate: f32) -> f32` - Calculates propagation priority (0.0-1.0)
  - Formula: `0.4 * trust_norm + 0.3 * quality_index + 0.3 * relay_success_rate`
  - Used for network health visualization

**Shared Responsibilities:**
- Trust score calculation and blending
- Quality metrics computation
- Network health metrics

### Module: `core/src/collective_intelligence/`

**Purpose:** Collective intelligence layer for consensus and judgment aggregation.

#### Submodule: `mod.rs`
- Module exports and public API

#### Submodule: `models.rs`
- `Judgment` - Individual judgment record
- `ConsensusResult` - Aggregated consensus result
- `ReputationScore` - Reputation scoring structure

#### Submodule: `judgment.rs`
- Judgment aggregation algorithms
- Consensus calculation functions

#### Submodule: `consensus.rs`
- Consensus building mechanisms
- Voting and weighting algorithms

#### Submodule: `reputation.rs`
- Reputation calculation
- Trust network analysis

**Shared Responsibilities:**
- Collective truth assessment
- Consensus building
- Reputation management

### Module: `core/src/knowledge.rs`

**Purpose:** Knowledge base management and reference data.

**Key Functions:**
- Knowledge base seeding and initialization
- Reference data queries (categories, formas, causes, develops, effects)
- Locale-specific knowledge base support

**Shared Responsibilities:**
- Reference data management
- Knowledge base synchronization

### Module: `core/src/sync.rs`

**Purpose:** Core synchronization logic (shared across platforms).

**Key Functions:**
- `merge_node_lists(local: Vec<Node>, remote: Vec<Node>) -> Vec<Node>` - Merges node lists with deterministic rules
  - Priority: Local > Global
  - TTL-based cleanup
  - Duplicate detection by address

**Shared Responsibilities:**
- Data merging algorithms
- Conflict resolution
- Synchronization primitives

### Module: `core/src/auth.rs`

**Purpose:** Authentication and authorization.

**Key Functions:**
- JWT token generation and validation
- Role-based access control (RBAC)
- User authentication

**Shared Responsibilities:**
- Security and access control
- Token management

### Module: `core/src/config.rs`

**Purpose:** Configuration management and constants.

**Key Constants:**
- Discovery timing configuration (LAN, Wi-Fi, Global intervals)
- TTL values (LAN_TTL_SECS, WIFI_TTL_SECS, GLOBAL_TTL_SECS)
- Health check settings (timeout, retry limit)
- Cleanup intervals

**Key Functions:**
- `default_ttl_for(source: NodeSource) -> i64` - Returns default TTL for discovery source
- `validate_ttl(ttl: i64) -> Result<(), CoreError>` - Validates TTL value

**Shared Responsibilities:**
- Configuration defaults
- Timing constants
- Validation rules

### Module: `core/src/weights.rs`

**Purpose:** Weight calculation for expert system.

**Key Functions:**
- Weight normalization
- Bias adjustment algorithms

**Shared Responsibilities:**
- Expert system weight management

## Main Library (`src/`)

### Module: `src/lib.rs`

**Purpose:** Main library entry point with feature gates.

**Feature-Gated Modules:**
- `#[cfg(feature = "desktop")]` - Desktop-only modules (api, middleware, net, p2p, server_diagnostics, sync)
- `#[cfg(feature = "mobile")]` - Mobile-only modules (android)
- Always compiled: `identity`, `node`

### Module: `src/identity/`

**Purpose:** Identity management and cryptographic operations.

#### Submodule: `identity_manager.rs`
- `IdentityManager` - Manages cryptographic identities
- Key generation and storage
- Ed25519 key pair management

#### Submodule: `mod.rs`
- Module exports

**Shared Responsibilities:**
- Cryptographic identity management
- Key pair generation and storage

### Module: `src/node.rs`

**Purpose:** Node configuration and state management.

**Key Functions:**
- Node initialization
- Configuration loading
- State management

**Shared Responsibilities:**
- Node lifecycle management
- Configuration persistence

### Module: `src/api.rs` (Desktop Only)

**Purpose:** HTTP REST API endpoints (Actix-web).

**Key Endpoints:**

#### Information
- `GET /api/v1/info` - Node information (name, version, p2p_enabled, db_path, peer_count)
- `GET /api/v1/stats` - Database statistics (events, statements, impacts, ratings, trust scores)

#### Events
- `POST /api/v1/events` - Create new event
- `GET /api/v1/events/{id}` - Get event by ID
- `GET /api/v1/events` - List events (with pagination)
- `PUT /api/v1/events/{id}` - Update event
- `DELETE /api/v1/events/{id}` - Delete event

#### Statements
- `POST /api/v1/statements` - Create statement
- `GET /api/v1/statements/{id}` - Get statement by ID
- `GET /api/v1/statements` - List statements

#### Impacts
- `POST /api/v1/impacts` - Create impact
- `GET /api/v1/impacts/{id}` - Get impact by ID
- `GET /api/v1/impacts` - List impacts

#### Contexts
- `POST /api/v1/contexts` - Create context template
- `GET /api/v1/contexts/{id}` - Get context by ID
- `GET /api/v1/contexts` - List contexts
- `GET /api/v1/contexts/match` - Match context by fields
- `PUT /api/v1/contexts/{id}` - Update context
- `DELETE /api/v1/contexts/{id}` - Delete context

#### Nodes
- `POST /api/v1/nodes` - Add/update node
- `GET /api/v1/nodes/{id}` - Get node by ID
- `GET /api/v1/nodes` - List nodes (with filtering)
- `PUT /api/v1/nodes/{id}` - Update node
- `DELETE /api/v1/nodes/{id}` - Delete node

#### Sync
- `POST /api/v1/sync` - Full synchronization
- `POST /api/v1/incremental_sync` - Incremental synchronization
- `GET /api/v1/sync/status` - Sync status

#### Network
- `GET /api/v1/network/local` - Local network information (peer history, summary)
- `GET /api/v1/graph/json` - Network graph data (JSON format)
- `GET /api/v1/graph/ascii` - Network graph data (ASCII format)

#### Nearby Sync
- `POST /api/v1/nearby_sync/start` - Start nearby sync service
- `POST /api/v1/nearby_sync/stop` - Stop nearby sync service
- `GET /api/v1/nearby_sync/status` - Nearby sync status

#### Ratings
- `POST /api/v1/ratings` - Create node rating
- `GET /api/v1/ratings` - List ratings

#### Collective Intelligence
- `POST /api/v1/recalc_collective` - Recalculate collective truth

#### Health
- `GET /health` - Health check endpoint

**Shared Responsibilities:**
- HTTP request/response handling
- JSON serialization/deserialization
- Error handling and status codes
- Authentication middleware integration

### Module: `src/middleware/` (Desktop Only)

**Purpose:** HTTP middleware for authentication and request processing.

#### Submodule: `envelope.rs`
- Request/response envelope handling
- Signature verification middleware

#### Submodule: `mod.rs`
- Module exports

**Shared Responsibilities:**
- Request authentication
- Signature verification
- Error handling

### Module: `src/p2p/` (Desktop Only)

**Purpose:** P2P networking and synchronization.

#### Submodule: `mod.rs`
- Module exports

#### Submodule: `encryption.rs`
- `CryptoIdentity` - Ed25519 cryptographic identity
- `public_key_hex()` - Get public key as hex string
- `sign(message: &[u8]) -> Vec<u8>` - Sign message
- `verify(message: &[u8], signature: &[u8], public_key: &[u8]) -> bool` - Verify signature

#### Submodule: `node.rs`
- `Node` - P2P node state management
- `poll_global_registries()` - Polls global node registries
- `run_http_reachability_checks()` - Checks HTTP reachability of nodes
- Peer discovery and management
- TTL-based cleanup

#### Submodule: `sync.rs`
- `SyncData` - Synchronization data structure
- `sync_push()` - Push data to peer
- `sync_pull()` - Pull data from peer
- `get_relay_stats()` - Get relay statistics
- Conflict resolution
- Data reconciliation

#### Submodule: `wifi_direct.rs`
- Wi-Fi Direct discovery and connection
- Platform-specific Wi-Fi operations

**Shared Responsibilities:**
- Peer discovery (UDP multicast, global registry)
- Data synchronization
- Cryptographic operations
- Network health monitoring

### Module: `src/net.rs` (Desktop Only)

**Purpose:** Network utilities for UDP discovery.

**Key Functions:**
- UDP beacon sender/listener
- LAN peer discovery (`239.255.0.1:52525`)
- Multicast group management

**Shared Responsibilities:**
- UDP networking
- Peer discovery
- Network communication

### Module: `src/sync.rs` (Desktop Only)

**Purpose:** Desktop-specific synchronization logic.

**Key Functions:**
- Full sync orchestration
- Incremental sync
- Conflict resolution strategies

**Shared Responsibilities:**
- Synchronization coordination
- Data consistency

### Module: `src/server_diagnostics.rs` (Desktop Only)

**Purpose:** Server health checks and diagnostics.

**Key Functions:**
- `check_api_health()` - Checks API endpoint availability
- `check_db_health()` - Checks database connectivity
- `check_p2p_health()` - Checks P2P listener status
- `run_full_diagnostics()` - Runs all health checks

**Shared Responsibilities:**
- Health monitoring
- Diagnostic reporting
- System status checks

### Module: `src/android/` (Mobile Only)

**Purpose:** Android-specific FFI bindings.

#### Submodule: `mod.rs`
- JNI function exports
- Android-specific functions

#### Submodule: `verify_json.rs`
- JSON signature verification for Android
- Ed25519 signature validation

**Shared Responsibilities:**
- Android JNI integration
- JSON processing
- Cryptographic verification

## Application (`app/`)

### Module: `app/src/main.rs`

**Purpose:** CLI application entry point.

**Key Commands:**
- `Init` - Initialize database schema
- `Seed { locale }` - Seed knowledge base (ru|en)
- `AddEvent { description, context, vector, start }` - Add truth event
- `Detect { id, detected, end, corrected }` - Mark event detected
- `Impact { event, type_id, positive, notes }` - Add impact record
- `Recalc` - Recalculate progress metrics
- `Show { id }` - Show event by ID
- `Assess { event, answers, apply }` - Assess event via expert system
- `AddStatement { event, text, context, score }` - Add statement
- `ShowStatement { id }` - Show statement by ID

**Shared Responsibilities:**
- CLI argument parsing (Clap)
- Command routing
- Database operations
- Expert system integration

### Module: `app/src/bin/truthctl.rs`

**Purpose:** Administrative CLI tool entry point.

**Key Commands:**
- `peers add/list/remove` - Peer management
- `sync { peer }` - Synchronize with peer
- `status` - Node status summary
- `config show/set/reset` - Configuration management
- `logs show/clear` - Sync log management
- `graph show` - Network graph visualization
- `diagnose` - Run diagnostics
- `reset-data` - Reset local data

**Shared Responsibilities:**
- CLI interface
- Peer registry management
- Configuration persistence
- Diagnostic tools

### Module: `app/src/cli.rs`

**Purpose:** CLI command implementations.

**Key Functions:**
- Command parsing and execution
- Peer registry operations
- Sync orchestration
- Status reporting

**Shared Responsibilities:**
- CLI command handling
- User interaction

### Module: `app/src/config_utils.rs`

**Purpose:** Configuration file management.

**Key Functions:**
- Load configuration from `~/.truthctl/config.json`
- Save configuration
- Default configuration generation
- Configuration validation

**Shared Responsibilities:**
- Configuration persistence
- File I/O operations

### Module: `app/src/diagnostics.rs`

**Purpose:** Diagnostic tool implementations.

**Key Functions:**
- Server health checks
- Database diagnostics
- P2P diagnostics
- Network diagnostics

**Shared Responsibilities:**
- Diagnostic reporting
- Health monitoring

### Module: `app/src/status_utils.rs`

**Purpose:** Status reporting utilities.

**Key Functions:**
- Aggregate node status
- Peer statistics
- Sync log summary
- Configuration summary

**Shared Responsibilities:**
- Status aggregation
- Report generation

## Tauri Backend (`ui/desktop/src-tauri/src/`)

### Module: `src-tauri/src/main.rs`

**Purpose:** Tauri application entry point.

**Key Functions:**
- Tauri application initialization
- Command registration
- Window management

**Shared Responsibilities:**
- Tauri application lifecycle
- Window management

### Module: `src-tauri/src/lib.rs`

**Purpose:** Tauri library exports and setup.

**Key Functions:**
- Command registration
- Application setup
- Error handling

**Shared Responsibilities:**
- Tauri integration
- Command routing

### Module: `src-tauri/src/commands/`

**Purpose:** Tauri command implementations (invoked from frontend).

#### Submodule: `events.rs`
- `create_event_fast()` - Create event with embedded context
- `get_event_fast(id)` - Get event by ID
- `list_events_fast(limit, offset)` - List events with pagination

#### Submodule: `impacts.rs`
- `add_impact(event_id, impact_level, notes)` - Add impact record
- Validation: impact_level must be 1-5

#### Submodule: `judgments.rs`
- `submit_judgment_fast(event_id, assessment, confidence_level, reasoning)` - Submit judgment
- `judgments_list_fast(event_id)` - List judgments for event
- `get_judgment_stats()` - Get judgment statistics

#### Submodule: `knowledge_base.rs`
- `knowledge_base_list()` - List knowledge base items (parses Data_Schema.md)
- Context template operations

#### Submodule: `summary.rs`
- `get_overall_metrics()` - Get overall statistics
- `list_event_rows()` - List event summary rows
- `export_overall_summary_txt()` - Export summary as text

#### Submodule: `config.rs`
- `get_app_config()` - Get application configuration
- `save_app_config(config)` - Save configuration
- `core_status()` - Get core status
- `test_http_connection()` - Test HTTP connection

#### Submodule: `logs.rs`
- `list_logs(limit, offset)` - List logs with pagination (35 lines/page)
- `clear_logs()` - Clear all logs

#### Submodule: `commands.rs`
- Command registration and routing

**Shared Responsibilities:**
- Frontend-backend communication
- Database operations
- Validation
- Error handling

### Module: `src-tauri/src/storage.rs`

**Purpose:** Tauri-specific storage operations.

**Key Functions:**
- Database connection management
- Transaction handling
- Data persistence

**Shared Responsibilities:**
- Storage abstraction
- Database access

### Module: `src-tauri/src/discovery.rs`

**Purpose:** Node discovery for Tauri backend.

**Key Functions:**
- Discovery service management
- Peer detection
- Network scanning

**Shared Responsibilities:**
- Discovery coordination
- Network operations

### Module: `src-tauri/src/settings.rs`

**Purpose:** Application settings management.

**Key Functions:**
- Settings loading/saving
- Configuration persistence
- Default values

**Shared Responsibilities:**
- Settings management
- Configuration

### Module: `src-tauri/src/logging.rs`

**Purpose:** Logging utilities for Tauri.

**Key Functions:**
- Log initialization
- Log level configuration
- Log file management

**Shared Responsibilities:**
- Logging infrastructure
- Log management

## Cross-Platform Considerations

### Shared Functionality
- Database operations (SQLite via rusqlite)
- Data models and types
- Expert system algorithms
- Trust propagation logic
- Cryptographic operations (Ed25519)

### Desktop-Only Functionality
- HTTP REST API (Actix-web)
- CLI tools (truthctl)
- Full P2P networking
- Server diagnostics
- UDP discovery
- Async runtime (Tokio)

### Mobile-Only Functionality
- Android JNI bindings
- JSON signature verification
- Lightweight async runtime
- Minimal P2P protocol

### Tauri-Specific Functionality
- Tauri command interface
- Frontend-backend bridge
- Window management
- Desktop-specific storage paths

## Error Handling

All modules use `CoreError` as the unified error type:
- Database errors (rusqlite::Error)
- Validation errors
- Network errors
- Cryptographic errors
- Configuration errors

Error propagation follows Rust best practices with `Result<T, CoreError>` return types.

## Version Information

This specification reflects **Truth Training v1.0.0** functionality. All modules and functions described are implemented and tested as of this version.

---

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.
- See [spec/README.md](README.md) for specification index.

