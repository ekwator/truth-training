# Data Model: Collective Intelligence Layer

## Entities

### Participant
**Purpose**: Represents an individual contributor to the collective intelligence system
**Key Attributes**:
- `id`: Unique identifier (UUID)
- `public_key`: Cryptographic identity (ed25519 public key)
- `reputation_score`: Current reputation (0.0 to 1.0)
- `total_judgments`: Count of judgments submitted
- `accurate_judgments`: Count of judgments that proved accurate
- `created_at`: Timestamp of participant registration
- `last_activity`: Timestamp of last judgment submission

**Validation Rules**:
- Reputation score must be between 0.0 and 1.0
- Public key must be valid ed25519 format
- Total judgments must be >= accurate judgments

**State Transitions**:
- New participant: reputation_score = 0.5 (neutral starting point)
- After judgment: reputation_score updated based on accuracy
- Reputation decay: gradual decrease over time without activity

### Event
**Purpose**: Represents an observable occurrence that can be evaluated by participants
**Key Attributes**:
- `id`: Unique identifier (UUID)
- `title`: Human-readable event description
- `description`: Detailed event information
- `event_type`: Category of event (e.g., "fact_check", "prediction", "assessment")
- `created_by`: Participant who created the event
- `created_at`: Timestamp of event creation
- `status`: Current status ("active", "resolved", "archived")
- `resolution_data`: Final outcome data (when resolved)

**Validation Rules**:
- Title must be non-empty and < 200 characters
- Description must be < 2000 characters
- Event type must be from predefined list
- Status must be valid enum value

**State Transitions**:
- Created: status = "active"
- Consensus reached: status = "resolved"
- Archived: status = "archived" (after resolution)

### Judgment
**Purpose**: Represents a participant's assessment of an event
**Key Attributes**:
- `id`: Unique identifier (UUID)
- `participant_id`: Reference to Participant
- `event_id`: Reference to Event
- `assessment`: Participant's judgment (e.g., "true", "false", "uncertain")
- `confidence_level`: Participant's confidence (0.0 to 1.0)
- `reasoning`: Optional explanation of judgment
- `submitted_at`: Timestamp of judgment submission
- `signature`: Cryptographic signature of judgment

**Validation Rules**:
- Assessment must be from predefined list
- Confidence level must be between 0.0 and 1.0
- Signature must be valid ed25519 signature
- Participant must not have already judged this event

**State Transitions**:
- Submitted: judgment recorded and signature verified
- Evaluated: accuracy determined when event is resolved

### Consensus
**Purpose**: Represents the collective decision emerging from multiple judgments
**Key Attributes**:
- `id`: Unique identifier (UUID)
- `event_id`: Reference to Event
- `consensus_value`: Weighted consensus result
- `confidence_score`: Overall confidence in consensus
- `participant_count`: Number of participants who contributed
- `calculated_at`: Timestamp of consensus calculation
- `algorithm_version`: Version of consensus algorithm used

**Validation Rules**:
- Consensus value must be valid assessment type
- Confidence score must be between 0.0 and 1.0
- Participant count must be > 0

**State Transitions**:
- Calculated: consensus computed from available judgments
- Updated: consensus recalculated when new judgments arrive

### ReputationHistory
**Purpose**: Tracks reputation changes over time for audit and analysis
**Key Attributes**:
- `id`: Unique identifier (UUID)
- `participant_id`: Reference to Participant
- `old_reputation`: Previous reputation score
- `new_reputation`: Updated reputation score
- `change_reason`: Reason for reputation change
- `event_id`: Reference to Event that triggered change
- `updated_at`: Timestamp of reputation update

**Validation Rules**:
- Old and new reputation must be between 0.0 and 1.0
- Change reason must be from predefined list
- Event ID must reference valid event

## Relationships

### One-to-Many Relationships
- Participant → Judgments (one participant can submit multiple judgments)
- Event → Judgments (one event can have multiple judgments)
- Event → Consensus (one event can have one consensus)
- Participant → ReputationHistory (one participant can have multiple reputation changes)

### Many-to-One Relationships
- Judgment → Participant (many judgments belong to one participant)
- Judgment → Event (many judgments belong to one event)
- Consensus → Event (one consensus belongs to one event)
- ReputationHistory → Participant (many reputation changes belong to one participant)

## Database Schema Extensions

### New Tables
```sql
-- Participants table
CREATE TABLE participants (
    id UUID PRIMARY KEY,
    public_key TEXT UNIQUE NOT NULL,
    reputation_score REAL NOT NULL DEFAULT 0.5,
    total_judgments INTEGER NOT NULL DEFAULT 0,
    accurate_judgments INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP
);

-- Events table
CREATE TABLE events (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    event_type TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES participants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL DEFAULT 'active',
    resolution_data JSONB
);

-- Judgments table
CREATE TABLE judgments (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL REFERENCES participants(id),
    event_id UUID NOT NULL REFERENCES events(id),
    assessment TEXT NOT NULL,
    confidence_level REAL NOT NULL,
    reasoning TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    signature TEXT NOT NULL,
    UNIQUE(participant_id, event_id)
);

-- Consensus table
CREATE TABLE consensus (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    consensus_value TEXT NOT NULL,
    confidence_score REAL NOT NULL,
    participant_count INTEGER NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    algorithm_version TEXT NOT NULL
);

-- Reputation history table
CREATE TABLE reputation_history (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL REFERENCES participants(id),
    old_reputation REAL NOT NULL,
    new_reputation REAL NOT NULL,
    change_reason TEXT NOT NULL,
    event_id UUID REFERENCES events(id),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Indexes
```sql
-- Performance indexes
CREATE INDEX idx_judgments_event_id ON judgments(event_id);
CREATE INDEX idx_judgments_participant_id ON judgments(participant_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_created_at ON events(created_at);
CREATE INDEX idx_reputation_history_participant_id ON reputation_history(participant_id);
CREATE INDEX idx_reputation_history_updated_at ON reputation_history(updated_at);
```
