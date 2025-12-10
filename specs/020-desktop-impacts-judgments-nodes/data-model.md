# Data Model: Desktop Impacts, Judgments, and Network Nodes UI

**Feature**: Desktop Impacts, Judgments, and Network Nodes UI  
**Date**: 2025-01-XX  
**Status**: Complete

## Overview

This document describes the data models, entities, and relationships for the Desktop UI implementation of impacts, judgments, and network nodes functionality. The models are based on the existing core storage schema and match Android implementation patterns.

## Core Entities

### Impact

Represents a recorded impact assessment for an event.

**Storage Schema** (core/src/storage.rs):
```rust
pub struct Impact {
    pub id: i64,
    pub event_id: i64,
    pub type_id: i64,        // Impact type ID (1-5 mapped to type_id)
    pub value: bool,          // Boolean value: true (positive) or false (negative)
    pub notes: Option<String>,
    pub created_at: i64,      // Unix timestamp (seconds)
}
```

**Desktop TypeScript Interface**:
```typescript
export interface Impact {
  id: string;
  event_id: string | number;
  impact_level: number;      // 1-5 (mapped from type_id for UI)
  notes?: string;
  created_at: string;        // ISO 8601 timestamp
}
```

**API Request**:
```typescript
export interface AddImpactRequest {
  event_id: string | number;
  impact_level: number;       // 1-5
  notes?: string;
}
```

**Mapping Logic**:
- User selects impact level 1-5 in UI
- ImpactLevelMapper maps: 1-3 → false, 4-5 → true
- Backend stores: `type_id = impact_level`, `value = mapped boolean`
- UI displays: "Positive (Level 4-5)" or "Negative (Level 1-3)" based on `value`

**Relationships**:
- Belongs to: Event (via `event_id`)
- Has: Optional notes (text field)

---

### Judgment

Represents a user's assessment/judgment of an event.

**Storage Schema** (core/src/storage.rs):
```rust
pub struct Judgment {
    pub id: i64,
    pub participant_id: String,
    pub event_id: i64,
    pub assessment: String,      // "true", "false", or "uncertain"
    pub confidence_level: f64,    // 0.0 to 1.0
    pub reasoning: Option<String>,
    pub submitted_at: i64,        // Unix timestamp (seconds)
    pub signature: String,
    pub weight: Option<f64>,
}
```

**Desktop TypeScript Interface**:
```typescript
export interface Judgment {
  id: string;
  participant_id: string;
  event_id: number;
  assessment: JudgmentAssessment;  // 'confirm' | 'reject' | 'abstain' (mapped from "true"/"false"/"uncertain")
  confidence_level: number;         // 0.0 to 1.0
  reasoning?: string;
  submitted_at: string;             // ISO 8601 timestamp
  signature: string;
  weight?: number;
}

export type JudgmentAssessment = 'confirm' | 'reject' | 'abstain';
```

**API Request**:
```typescript
export interface CreateJudgmentRequest {
  event_id: number;
  assessment: JudgmentAssessment;  // Maps to "true"/"false"/"uncertain" in backend
  confidence_level: number;         // 0.0 to 1.0
  reasoning?: string;
  signature: string;
}
```

**Assessment Mapping**:
- Desktop UI: 'confirm' | 'reject' | 'abstain'
- Backend API: "true" | "false" | "uncertain"
- Mapping: confirm → "true", reject → "false", abstain → "uncertain"

**Relationships**:
- Belongs to: Event (via `event_id`)
- Has: Optional reasoning (text field)
- Has: Participant signature (cryptographic)

---

### NodeRecord

Represents a network node discovered in the network.

**Storage Schema** (core/src/storage.rs):
```rust
pub struct Node {
    pub id: i64,
    pub address: String,
    pub node_type: NodeType,       // LAN, WIFI, GLOBAL, RELAY, CLIENT
    pub reachable: bool,
    pub ttl: i64,                  // Time-to-live in seconds
    pub last_seen: i64,            // Unix timestamp (seconds)
    pub updated_at: i64,           // Unix timestamp (seconds)
    pub source: Option<String>,
    pub node_id: Option<String>,
    pub created_at: i64,           // Unix timestamp (seconds)
}
```

**Desktop TypeScript Interface**:
```typescript
export interface NodeRecord {
  id: number;
  address: string;
  node_type: string;              // "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
  reachable: boolean;
  ttl: number;                   // Time-to-live in seconds
  last_seen: number;              // Unix timestamp (seconds)
  updated_at: number;             // Unix timestamp (seconds)
  source?: string;
  node_id?: string;
  expires_in: number;             // Calculated: (last_seen + ttl - now).max(0)
}
```

**Node Type Mapping**:
- Technical Types: LAN, WIFI, GLOBAL, RELAY, CLIENT
- User-Friendly Types: Hub, Leaf
- Mapping: Hub = RELAY, GLOBAL; Leaf = LAN, WIFI, CLIENT

**Calculated Fields**:
- `expires_in`: `(last_seen + ttl - now).max(0)` - time until TTL expires
- `age`: `now - last_seen` - time since last seen

**Relationships**:
- Standalone entity (no foreign keys)
- Can be filtered by: node_type, reachable, address

---

## Data Flow

### Adding Impact

1. **User Input** (AddImpactModal):
   - Impact level: 1-5 (slider)
   - Notes: optional text

2. **Frontend Processing**:
   - Validate impact level (1-5)
   - Map impact level to boolean: ImpactLevelMapper.mapToBoolean(level)
   - Create AddImpactRequest

3. **API Call**:
   - `ApiService.addImpact(request)`
   - Tauri command: `add_impact`
   - Backend validates and stores

4. **Storage**:
   - `truth_storage::add_impact()` stores in SQLite
   - Returns impact_id

5. **UI Update**:
   - Reload impacts list for event
   - Display new impact in list

### Submitting Judgment

1. **User Input** (SubmitJudgmentModal):
   - Assessment: 'confirm' | 'reject' | 'abstain'
   - Confidence: 0.0-1.0 (slider)
   - Reasoning: optional text

2. **Frontend Processing**:
   - Validate assessment and confidence
   - Map assessment: confirm → "true", reject → "false", abstain → "uncertain"
   - Generate signature (if required)
   - Create CreateJudgmentRequest

3. **API Call**:
   - `ApiService.createJudgment(request)`
   - Tauri command: `submit_judgment_fast`
   - Backend validates and stores

4. **Storage**:
   - `db.insert_judgment()` stores in SQLite
   - Returns judgment with id and submitted_at

5. **UI Update**:
   - Reload judgments list for event
   - Display new judgment in list

### Viewing Node Details

1. **User Action**:
   - Click on node in NodesPanel

2. **Data Loading**:
   - Option A: Filter `list_nodes()` by address or node_id
   - Option B: Add `get_node_by_id()` Tauri command
   - Load node data

3. **Display**:
   - Show all node fields
   - Calculate expires_in and age
   - Map node_type to Hub/Leaf

4. **Refresh**:
   - Reload node data on refresh action

---

## API Contracts

### Impact API

**Add Impact**:
- Endpoint: `add_impact` (Tauri command)
- Request: `AddImpactRequest { event_id, impact_level, notes? }`
- Response: `Impact { id, event_id, impact_level, notes?, created_at }`
- Validation: impact_level must be 1-5

**Get Impacts for Event** (TODO):
- Endpoint: `get_impacts_for_event` (Tauri command - needs implementation)
- Request: `{ event_id: number }`
- Response: `Impact[]`
- Alternative: Filter all impacts by event_id in frontend

### Judgment API

**Submit Judgment**:
- Endpoint: `submit_judgment_fast` (Tauri command)
- Request: `SubmitJudgmentRequest { event_id, assessment, confidence_level, reasoning? }`
- Response: `Judgment { id, event_id, assessment, confidence_level, reasoning?, submitted_at }`
- Validation: assessment must be "true"/"false"/"uncertain", confidence_level must be 0.0-1.0

**Get Judgments for Event**:
- Endpoint: `judgments_list_fast` (Tauri command)
- Request: `{ eventId?: string, page: number, perPage: number }`
- Response: `{ data: Judgment[], total: number }`

### Node API

**List Nodes**:
- Endpoint: `list_nodes` (Tauri command)
- Request: `{ nodeType?: string, reachable?: boolean }`
- Response: `NodeRecord[]`

**Get Node by ID** (TODO):
- Endpoint: `get_node_by_id` (Tauri command - needs implementation)
- Request: `{ nodeId: number }`
- Response: `NodeRecord`
- Alternative: Filter `list_nodes()` result by id in frontend

---

## Database Schema

### Impacts Table

```sql
CREATE TABLE impact (
    id INTEGER PRIMARY KEY,
    event_id INTEGER NOT NULL,
    type_id INTEGER NOT NULL,
    value INTEGER NOT NULL,  -- 0 (false) or 1 (true)
    notes TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES truth_events(id)
);
```

### Judgments Table

```sql
CREATE TABLE judgments (
    id TEXT PRIMARY KEY,
    participant_id TEXT NOT NULL,
    event_id INTEGER NOT NULL,
    assessment TEXT NOT NULL,  -- "true", "false", or "uncertain"
    confidence_level REAL NOT NULL,  -- 0.0 to 1.0
    reasoning TEXT,
    submitted_at TEXT NOT NULL,  -- ISO 8601 timestamp
    signature TEXT NOT NULL,
    weight REAL,
    FOREIGN KEY (event_id) REFERENCES truth_events(id)
);
```

### Nodes Table

```sql
CREATE TABLE nodes (
    id INTEGER PRIMARY KEY,
    address TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,  -- "LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT"
    reachable INTEGER NOT NULL,  -- 0 (false) or 1 (true)
    ttl INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    source TEXT,
    node_id TEXT,
    created_at INTEGER NOT NULL
);
```

---

## Data Validation Rules

### Impact Validation

- `impact_level`: Must be integer in range [1, 5]
- `event_id`: Must be valid event ID (exists in truth_events table)
- `notes`: Optional, max length TBD (typically 1000 characters)

### Judgment Validation

- `assessment`: Must be "true", "false", or "uncertain" (backend) / 'confirm', 'reject', or 'abstain' (frontend)
- `confidence_level`: Must be float in range [0.0, 1.0]
- `event_id`: Must be valid event ID
- `reasoning`: Optional, max length TBD (typically 2000 characters)
- `signature`: Required, format TBD (cryptographic signature)

### Node Validation

- `address`: Required, valid network address format
- `node_type`: Must be one of: "LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT"
- `reachable`: Boolean (0 or 1)
- `ttl`: Must be positive integer (seconds)
- `last_seen`: Valid Unix timestamp
- `source`: Optional string
- `node_id`: Optional string

---

## Relationships Diagram

```
Event (truth_events)
  ├── has many → Impact (impact.event_id)
  └── has many → Judgment (judgments.event_id)

Node (nodes)
  └── standalone (no foreign keys)
```

---

## Data Migration Notes

No migrations required. All tables and fields already exist in core storage schema.

---

## References

- Core Storage: `core/src/storage.rs`
- Desktop Tauri Commands: `ui/desktop/src-tauri/src/commands/impacts.rs`, `commands/judgments.rs`, `discovery.rs`
- Desktop Types: `ui/desktop/src/types/judgments.ts`, `types/api.ts`
- Android Entities: `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/`

