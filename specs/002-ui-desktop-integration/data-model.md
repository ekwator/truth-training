# Data Model: UI Desktop Integration (Tauri)

**Feature**: UI Desktop Integration (Tauri)  
**Date**: 2025-01-18  
**Phase**: 1 - Design & Contracts

## Frontend Data Models

### Event
Represents a truth claim or statement in the UI layer

```typescript
interface Event {
  id: string;                    // UUID from core system
  description: string;           // Event description
  context?: string;              // Additional context
  createdAt: Date;              // Creation timestamp
  updatedAt: Date;              // Last update timestamp
  consensusStatus: ConsensusStatus; // Current consensus state
  participantCount: number;      // Number of participants
  localStatus: LocalEventStatus; // Local UI state
}

enum ConsensusStatus {
  PENDING = "pending",
  IN_PROGRESS = "in_progress", 
  CONVERGED = "converged",
  DIVERGED = "diverged"
}

enum LocalEventStatus {
  SYNCED = "synced",
  PENDING_SYNC = "pending_sync",
  SYNC_ERROR = "sync_error",
  LOCAL_ONLY = "local_only"
}
```

### Judgment
Represents a user's evaluation of an event

```typescript
interface Judgment {
  id: string;                   // UUID from core system
  eventId: string;              // Reference to event
  assessment: "true" | "false";  // User's assessment
  confidenceLevel: number;       // 0.0 to 1.0
  reasoning?: string;           // Optional reasoning
  submittedAt: Date;            // Submission timestamp
  localStatus: LocalJudgmentStatus; // Local UI state
}

enum LocalJudgmentStatus {
  SYNCED = "synced",
  PENDING_SYNC = "pending_sync", 
  SYNC_ERROR = "sync_error",
  LOCAL_ONLY = "local_only"
}
```

### Participant
Represents a participant in the collective intelligence system

```typescript
interface Participant {
  id: string;                   // UUID from core system
  publicKey: string;            // Ed25519 public key
  reputation: number;           // Current reputation score
  totalJudgments: number;       // Total judgments submitted
  accuracyRate: number;         // Historical accuracy
  lastActiveAt: Date;           // Last activity timestamp
}
```

### SyncStatus
Represents the current synchronization state

```typescript
interface SyncStatus {
  isOnline: boolean;            // Network connectivity
  lastSyncAt?: Date;           // Last successful sync
  pendingOperations: number;    // Queued operations count
  syncErrors: SyncError[];      // Recent sync errors
  coreSystemStatus: CoreSystemStatus; // Core system health
}

interface SyncError {
  id: string;
  operation: string;            // Failed operation type
  error: string;               // Error message
  timestamp: Date;             // Error timestamp
  retryCount: number;          // Number of retry attempts
}

enum CoreSystemStatus {
  HEALTHY = "healthy",
  DEGRADED = "degraded", 
  UNAVAILABLE = "unavailable",
  UNKNOWN = "unknown"
}
```

### UI State Models

### AppState
Global application state

```typescript
interface AppState {
  currentUser: AnonymousUser;   // Anonymous user context
  syncStatus: SyncStatus;       // Current sync state
  offlineQueue: OfflineOperation[]; // Pending operations
  uiPreferences: UIPreferences; // User preferences
  errorState: ErrorState;       // Current error state
}

interface AnonymousUser {
  sessionId: string;            // Session identifier
  createdAt: Date;              // Session start time
  localKeyPair: KeyPair;       // Local Ed25519 key pair
}

interface KeyPair {
  publicKey: string;
  privateKey: string;           // Encrypted locally
}

interface UIPreferences {
  theme: "light" | "dark" | "auto";
  language: string;
  autoSync: boolean;
  notifications: boolean;
}

interface ErrorState {
  hasError: boolean;
  errorType?: ErrorType;
  message?: string;
  retryable: boolean;
}

enum ErrorType {
  NETWORK = "network",
  CORE_SYSTEM = "core_system",
  VALIDATION = "validation",
  UNKNOWN = "unknown"
}
```

### OfflineOperation
Represents operations queued for later sync

```typescript
interface OfflineOperation {
  id: string;                   // Local operation ID
  type: OperationType;          // Operation type
  payload: any;                 // Operation data
  timestamp: Date;               // Queue timestamp
  retryCount: number;           // Retry attempts
  maxRetries: number;           // Maximum retry attempts
}

enum OperationType {
  CREATE_EVENT = "create_event",
  SUBMIT_JUDGMENT = "submit_judgment",
  UPDATE_PREFERENCES = "update_preferences"
}
```

## Data Validation Rules

### Event Validation
- `description`: Required, 1-1000 characters
- `context`: Optional, max 2000 characters
- `id`: Required, valid UUID format

### Judgment Validation  
- `assessment`: Required, must be "true" or "false"
- `confidenceLevel`: Required, 0.0 to 1.0 inclusive
- `reasoning`: Optional, max 1000 characters
- `eventId`: Required, must reference existing event

### Participant Validation
- `id`: Required, valid UUID format
- `publicKey`: Required, valid Ed25519 public key format
- `reputation`: Required, 0.0 to 1.0 inclusive
- `totalJudgments`: Required, non-negative integer

## State Transitions

### Event Lifecycle
1. **Created** → `LOCAL_ONLY` status
2. **Synced** → `SYNCED` status  
3. **Sync Error** → `SYNC_ERROR` status
4. **Retry Success** → `SYNCED` status

### Judgment Lifecycle
1. **Submitted** → `LOCAL_ONLY` status
2. **Synced** → `SYNCED` status
3. **Sync Error** → `SYNC_ERROR` status
4. **Retry Success** → `SYNCED` status

### Sync Status Transitions
1. **Online** → `isOnline: true`
2. **Offline** → `isOnline: false`, queue operations
3. **Sync Success** → Update `lastSyncAt`, clear errors
4. **Sync Error** → Add to `syncErrors`, increment retry count

## Data Relationships

### Event ↔ Judgment
- One-to-many relationship
- Event can have multiple judgments
- Judgment belongs to one event

### Participant ↔ Judgment  
- One-to-many relationship
- Participant can submit multiple judgments
- Judgment belongs to one participant (anonymous)

### SyncStatus ↔ All Entities
- One-to-one relationship with app state
- Tracks sync status for all entities
- Manages offline queue for all operations

## Local Storage Schema

### SQLite Tables (via core API)
- `events_ci`: Core system events
- `judgments_ci`: Core system judgments  
- `participants`: Core system participants
- `consensus_ci`: Core system consensus data

### Local Cache Tables
- `local_events`: Local event cache
- `local_judgments`: Local judgment cache
- `offline_queue`: Pending operations
- `sync_status`: Sync state tracking
- `user_preferences`: UI preferences

## Data Flow Patterns

### Read Operations
1. Check local cache first
2. If stale or missing, fetch from core API
3. Update local cache
4. Return data to UI

### Write Operations (Online)
1. Validate data locally
2. Send to core API
3. Update local cache on success
4. Update UI state

### Write Operations (Offline)
1. Validate data locally
2. Add to offline queue
3. Update local cache optimistically
4. Sync when online

### Sync Operations
1. Process offline queue
2. Send operations to core API
3. Update local cache
4. Clear successful operations from queue
5. Retry failed operations with backoff