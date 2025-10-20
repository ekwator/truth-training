# Quickstart: Collective Intelligence Layer

## Overview
This quickstart guide demonstrates how to use the Collective Intelligence Layer to submit judgments, view consensus, and track reputation in the Truth Training platform.

## Prerequisites
- Truth Training platform running locally on `http://localhost:8080`
- Valid participant account with cryptographic identity
- JWT authentication token

## Step 1: Submit a Judgment

### Submit your assessment of an event
```bash
curl -X POST http://localhost:8080/api/v1/judgments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "550e8400-e29b-41d4-a716-446655440000",
    "assessment": "true",
    "confidence_level": 0.8,
    "reasoning": "Based on multiple reliable sources and expert analysis",
    "public_key": "<base64 32-byte ed25519 public key>",
    "signature": "<base64 64-byte ed25519 signature of canonical message>"
  }'
```

**Expected Response:**
```json
{ "id": "660e8400-e29b-41d4-a716-446655440001" }
```

## Step 2: View Event Consensus

### Get the current consensus for an event
```bash
curl -X GET "http://localhost:8080/api/v1/consensus/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "id": "880e8400-e29b-41d4-a716-446655440003",
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "consensus_value": "true",
  "confidence_score": 0.82,
  "participant_count": 15,
  "calculated_at": "2025-01-27T10:35:00Z",
  "algorithm_version": "1.0.0"
}
```

## Step 3: Check Your Reputation

### View your current reputation score
```bash
curl -X GET "http://localhost:8080/api/v1/reputation/770e8400-e29b-41d4-a716-446655440002" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "participant_id": "770e8400-e29b-41d4-a716-446655440002",
  "reputation_score": 0.78,
  "total_judgments": 25,
  "accurate_judgments": 20,
  "accuracy_rate": 0.8,
  "last_activity": "2025-01-27T10:30:00Z"
}
```

## Step 4: Calculate Consensus

### Trigger consensus calculation for an event
```bash
curl -X POST "http://localhost:8080/api/v1/consensus/550e8400-e29b-41d4-a716-446655440000/calculate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:** current consensus object for the event.

## Step 5: View Reputation Leaderboard

### See top participants by reputation
```bash
curl -X GET "http://localhost:8080/api/v1/reputation/leaderboard?limit=10&min_judgments=5" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "leaderboard": [
    {
      "participant_id": "990e8400-e29b-41d4-a716-446655440004",
      "reputation_score": 0.95,
      "total_judgments": 150,
      "accuracy_rate": 0.94,
      "rank": 1
    },
    {
      "participant_id": "770e8400-e29b-41d4-a716-446655440002",
      "reputation_score": 0.78,
      "total_judgments": 25,
      "accuracy_rate": 0.8,
      "rank": 2
    }
  ],
  "total_participants": 2
}
```

## Step 6: View All Judgments for an Event

### See all participant judgments for an event
```bash
curl -X GET "http://localhost:8080/api/v1/judgments?event_id=550e8400-e29b-41d4-a716-446655440000&include_participant_info=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "judgments": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "participant_id": "770e8400-e29b-41d4-a716-446655440002",
      "event_id": "550e8400-e29b-41d4-a716-446655440000",
      "assessment": "true",
      "confidence_level": 0.8,
      "reasoning": "Based on multiple reliable sources and expert analysis",
      "submitted_at": "2025-01-27T10:30:00Z",
      "participant_reputation": 0.75
    }
  ],
  "total_count": 1
}
```

## Common Error Scenarios

### 1. Duplicate Judgment Submission
```bash
# Attempting to submit a second judgment for the same event
curl -X POST http://localhost:8080/api/v1/judgments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "550e8400-e29b-41d4-a716-446655440000",
    "assessment": "false",
    "confidence_level": 0.6
  }'
```

**Expected Error Response:**
```json
{
  "error": "conflict",
  "message": "Participant has already submitted a judgment for this event",
  "details": {
    "participant_id": "770e8400-e29b-41d4-a716-446655440002",
    "event_id": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### 2. Insufficient Judgments for Consensus
```bash
# Attempting to calculate consensus with only 1 judgment
curl -X POST "http://localhost:8080/api/v1/consensus/550e8400-e29b-41d4-a716-446655440000/calculate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Error Response:**
```json
{
  "error": "insufficient_data",
  "message": "At least 3 judgments required for consensus calculation",
  "details": {
    "current_judgment_count": 1,
    "minimum_required": 3
  }
}
```

## Integration Testing Scenarios

### Scenario 1: Complete Judgment and Consensus Flow
1. Submit judgment for event
2. Wait for other participants to submit judgments
3. Trigger consensus calculation
4. Verify consensus reflects weighted participant opinions
5. Check reputation updates based on judgment accuracy

### Scenario 2: Reputation Evolution
1. Submit multiple judgments over time
2. Track reputation changes as judgments are evaluated
3. Verify reputation reflects accuracy rate
4. Check leaderboard position updates

### Scenario 3: Consensus Convergence
1. Create event with initial uncertain consensus
2. Submit judgments with varying confidence levels
3. Observe consensus evolution over time
4. Verify convergence toward truth as more data becomes available

## Performance Expectations
- Judgment submission: < 100ms response time
- Consensus calculation: < 200ms for 100 participants
- Reputation updates: < 50ms response time
- Leaderboard queries: < 150ms response time

## Security Considerations
- All API calls require valid JWT authentication
- Judgment signatures are cryptographically verified
- Reputation updates are audited and logged
- Participant privacy is maintained in leaderboard data
