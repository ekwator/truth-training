# Research: Collective Intelligence Layer

## Research Tasks

### 1. Consensus Calculation Performance Requirements
**Task**: Research consensus calculation performance requirements for collective intelligence systems

**Decision**: Target <100ms for consensus calculation with up to 1000 concurrent participants
**Rationale**: Real-time consensus is critical for user experience. 100ms provides responsive feedback while allowing for complex weighted calculations. 1000 participants represents a reasonable scale for initial implementation.
**Alternatives considered**: 
- <50ms (too aggressive for complex algorithms)
- <500ms (too slow for real-time interaction)
- 10,000+ participants (premature optimization)

### 2. Scale and Scope Requirements
**Task**: Research expected number of concurrent participants and events for collective intelligence systems

**Decision**: Support 100-1000 concurrent participants with 10,000-100,000 events per day
**Rationale**: Based on similar collective intelligence platforms and P2P networks. This scale allows for meaningful consensus while remaining computationally feasible.
**Alternatives considered**:
- 10-100 participants (too small for meaningful collective intelligence)
- 10,000+ participants (requires distributed consensus algorithms beyond current scope)

### 3. Consensus Algorithm Best Practices
**Task**: Find best practices for consensus algorithms in collective intelligence systems

**Decision**: Implement weighted consensus with reputation-based influence and outlier filtering
**Rationale**: Weighted consensus respects participant expertise while outlier filtering prevents manipulation. This aligns with Truth Training's philosophical principles of dynamic reputation and self-correcting systems.
**Alternatives considered**:
- Simple majority voting (doesn't account for participant expertise)
- Pure averaging (vulnerable to outlier manipulation)
- Complex Byzantine fault tolerance (overkill for current scale)

### 4. Reputation System Design Patterns
**Task**: Research reputation system design patterns for collective intelligence

**Decision**: Implement exponential moving average reputation with accuracy-based updates
**Rationale**: Exponential moving average provides smooth reputation changes while emphasizing recent performance. Accuracy-based updates align with Truth Training's principle of learning and correction.
**Alternatives considered**:
- Linear reputation updates (too slow to adapt)
- Binary reputation (lacks nuance)
- Complex multi-dimensional reputation (premature complexity)

### 5. Integration with Existing P2P Network
**Task**: Research integration patterns for collective intelligence with existing P2P networks

**Decision**: Extend existing P2P message types with collective intelligence payloads
**Rationale**: Leverages existing cryptographic identity and message signing infrastructure. Maintains consistency with current architecture.
**Alternatives considered**:
- Separate collective intelligence network (unnecessary complexity)
- Centralized consensus server (violates P2P principles)
- Blockchain-based consensus (overkill for current requirements)

### 6. Data Storage and Historical Records
**Task**: Research data storage patterns for collective intelligence historical records

**Decision**: Store judgments and consensus history in SQLite with time-series optimization
**Rationale**: SQLite provides ACID compliance and integrates with existing storage layer. Time-series optimization supports efficient historical queries.
**Alternatives considered**:
- Separate time-series database (additional complexity)
- In-memory storage only (loses historical data)
- External database (violates self-contained principle)

## Consolidated Findings

### Performance Targets
- Consensus calculation: <100ms for 1000 participants
- Concurrent participants: 100-1000
- Events per day: 10,000-100,000
- Storage: SQLite with time-series optimization

### Algorithm Choices
- Consensus: Weighted consensus with reputation-based influence
- Reputation: Exponential moving average with accuracy-based updates
- Outlier filtering: Statistical methods (z-score based)
- Integration: Extend existing P2P message types

### Architecture Decisions
- Storage: Extend existing SQLite schema
- API: Add endpoints to existing Actix-web server
- Testing: Comprehensive integration tests for consensus algorithms
- Security: Leverage existing ed25519_dalek cryptographic infrastructure
