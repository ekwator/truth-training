# Analysis of the Possibility to Determine Information Exchange Participants Parameters Using Node Discovery and Collective Intelligence Systems

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Introduction

Based on the study of Truth Training system documentation, including the main model ([model_core.md](model_core.md)), node discovery system, and collective intelligence, a detailed analysis of the possibilities for determining various information exchange participant parameters is presented.

## Node Discovery System Capabilities

The Node Discovery system in Truth Training allows determining the following information exchange participant parameters:

### 1. Geographic Proximity

**Detection Capability:** Limited

**Justification:**
- The system stores information about node network addresses (IP addresses, URLs) in the `discovery_nodes` table
- Node types are classified as LAN, WIFI, GLOBAL, RELAY, CLIENT, allowing to determine approximate localization
- For LAN and Wi-Fi nodes, geographic proximity can be assumed based on IP addresses
- However, precise geolocation positioning is not implemented in the system

**Implementation:**
```sql
-- Example of determining node proximity
SELECT 
    dn1.address as node1_address,
    dn2.address as node2_address,
    CASE 
        WHEN dn1.address LIKE '192.168.%' AND dn2.address LIKE '192.168.%' THEN 'LOCAL_NETWORK'
        WHEN dn1.address LIKE '10.%' AND dn2.address LIKE '10.%' THEN 'PRIVATE_NETWORK'
        ELSE 'DISTANT_NETWORK'
    END as proximity_indicator
FROM discovery_nodes dn1
JOIN discovery_nodes dn2 ON dn1.id != dn2.id
WHERE dn1.type IN ('LAN', 'WIFI') AND dn2.type IN ('LAN', 'WIFI');
```

### 2. Interaction Density

**Detection Capability:** High

**Justification:**
- The `sync_attempts` table contains records of synchronization attempts between nodes
- The `peer_synchronization` table tracks interaction history with each node
- The `sync_operations` table records low-level synchronization operations
- This data allows calculating interaction frequency and success rate

**Implementation:**
```sql
-- Example of analyzing interaction density
SELECT 
    ps.peer_url,
    ps.success_count,
    ps.fail_count,
    CASE
        WHEN (ps.success_count + ps.fail_count) > 0
        THEN ps.success_count * 100.0 / (ps.success_count + ps.fail_count)
        ELSE 0.0
    END as success_rate,
    ps.last_sync
FROM peer_synchronization ps
ORDER BY success_rate DESC;
```

### 3. Temporal Synchronization

**Detection Capability:** High (considering time scales and timings of events, assessments and impacts)

**Justification:**
- The system tracks last contact time (`last_seen`) in the `discovery_nodes` table
- The `sync_attempts` table contains timestamps of synchronization attempts
- Data on node activity synchronization can be analyzed for temporal pattern matching
- Specialized timeline tables (according to specifications, starting from version v1.1.0) allow more precise temporal synchronization determination:
  - `event_timeline` - contains event time boundaries (t_start, t_end) for analyzing event time frames
  - `impact_timeline` - contains consequence assessment time boundaries for analyzing consequence time frames
  - `judgment_timeline` - contains judgment time boundaries for analyzing assessment time frames
- According to specification 08-p2p-sync.md, these tables participate in P2P exchange:
  - "The P2P exchange system synchronizes the following tables between nodes: "truth_event", "event_timeline", "event_links", "impact", "impact_timeline", "impact_links", "judgment", "judgment_timeline", "judgment_links""
  - "Note: The TruthEvent, Impact, and Judgment objects reference their respective Timeline objects via timeline_id, and relationships between entities are maintained through link tables; this enables proper temporal synchronization and relationship preservation of all assessment types"
- This allows analyzing not only node activity but also synchronization of event, consequence, and judgment assessments by participants

**Implementation:**
```sql
-- Example of temporal synchronization analysis through synchronization of event timings
SELECT
    dn1.address as node1_address,
    dn2.address as node2_address,
    sa1.timestamp as sync_time_1,
    sa2.timestamp as sync_time_2,
    ABS(sa1.timestamp - sa2.timestamp) as time_difference_seconds,
    CASE
        WHEN ABS(sa1.timestamp - sa2.timestamp) <= 60 THEN 'HIGH_SYNC'
        WHEN ABS(sa1.timestamp - sa2.timestamp) <= 300 THEN 'MEDIUM_SYNC'
        WHEN ABS(sa1.timestamp - sa2.timestamp) <= 3600 THEN 'LOW_SYNC'
        ELSE 'NO_SYNC'
    END as synchronization_level
FROM sync_attempts sa1
JOIN sync_attempts sa2 ON sa1.timestamp >= sa2.timestamp
    AND sa1.timestamp <= sa2.timestamp + 3600  -- within an hour of each other
    AND sa1.peer_url != sa2.peer_url
JOIN discovery_nodes dn1 ON dn1.address = sa1.peer_url
JOIN discovery_nodes dn2 ON dn2.address = sa2.peer_url
WHERE sa1.status = 'success' AND sa2.status = 'success'
ORDER BY ABS(sa1.timestamp - sa2.timestamp);

-- Example of temporal synchronization analysis through event timings (event_timeline)
SELECT
    p1.public_key as participant1,
    p2.public_key as participant2,
    et1.t_start as event1_start,
    et2.t_start as event2_start,
    ABS(et1.t_start - et2.t_start) as start_time_difference,
    CASE
        WHEN ABS(et1.t_start - et2.t_start) <= 3600 THEN 'SAME_HOUR_TIMING'  -- within an hour
        WHEN ABS(et1.t_start - et2.t_start) <= 86400 THEN 'SAME_DAY_TIMING'  -- within a day
        ELSE 'DIFFERENT_DAYS_TIMING'
    END as timing_synchronization
FROM event_timeline et1
JOIN event_timeline et2 ON ABS(et1.t_start - et2.t_start) <= 86400  -- within 24 hours
JOIN truth_event te1 ON te1.timeline_id = et1.id
JOIN truth_event te2 ON te2.timeline_id = et2.id
JOIN participants p1 ON p1.id = te1.participant_id
JOIN participants p2 ON p2.id = te2.participant_id
WHERE te1.participant_id != te2.participant_id
ORDER BY ABS(et1.t_start - et2.t_start);

-- Example of temporal synchronization analysis through impact timings (impact_timeline)
SELECT
    p1.public_key as participant1,
    p2.public_key as participant2,
    it1.t_start as impact1_start,
    it2.t_start as impact2_start,
    ABS(it1.t_start - it2.t_start) as impact_timing_difference,
    CASE
        WHEN ABS(it1.t_start - it2.t_start) <= 3600 THEN 'SYNCHRONIZED_IMPACT_TIMING'
        WHEN ABS(it1.t_start - it2.t_start) <= 86400 THEN 'CLOSE_IMPACT_TIMING'
        ELSE 'ASYNC_IMPACT_TIMING'
    END as impact_timing_synchronization
FROM impact_timeline it1
JOIN impact_timeline it2 ON ABS(it1.t_start - it2.t_start) <= 86400
JOIN impact i1 ON i1.timeline_id = it1.id
JOIN impact i2 ON i2.timeline_id = it2.id
JOIN participants p1 ON p1.id = i1.participant_id
JOIN participants p2 ON p2.id = i2.participant_id
WHERE i1.participant_id != i2.participant_id
ORDER BY ABS(it1.t_start - it2.t_start);

-- Example of temporal synchronization analysis through judgment timings (judgment_timeline)
SELECT
    p1.public_key as participant1,
    p2.public_key as participant2,
    jt1.t_start as judgment1_start,
    jt2.t_start as judgment2_start,
    ABS(jt1.t_start - jt2.t_start) as judgment_timing_difference,
    CASE
        WHEN ABS(jt1.t_start - jt2.t_start) <= 3600 THEN 'SYNCHRONIZED_JUDGMENT_TIMING'
        WHEN ABS(jt1.t_start - jt2.t_start) <= 86400 THEN 'CLOSE_JUDGMENT_TIMING'
        ELSE 'ASYNC_JUDGMENT_TIMING'
    END as judgment_timing_synchronization
FROM judgment_timeline jt1
JOIN judgment_timeline jt2 ON ABS(jt1.t_start - jt2.t_start) <= 86400
JOIN judgment j1 ON j1.timeline_id = jt1.id
JOIN judgment j2 ON j2.timeline_id = jt2.id
JOIN participants p1 ON p1.id = j1.participant_id
JOIN participants p2 ON p2.id = j2.participant_id
WHERE j1.participant_id != j2.participant_id
ORDER BY ABS(jt1.t_start - jt2.t_start);
```

## Collective Intelligence System Capabilities

The Collective Intelligence system allows determining the following similarity parameters:

### 1. Participant Reputation Similarity

**Detection Capability:** High

**Justification:**
- The `participants` table contains the `reputation_score` field for each participant
- The `reputation_history` table tracks reputation changes over time
- The `participant_reputation_calculation` view allows analyzing reputation by various aspects
- Comparing reputation metrics of different participants allows determining similarity

**Implementation:**
```sql
-- Example of determining reputation similarity
SELECT 
    p1.id as participant1_id,
    p2.id as participant2_id,
    ABS(p1.reputation_score - p2.reputation_score) as reputation_distance,
    CASE
        WHEN ABS(p1.reputation_score - p2.reputation_score) < 0.1 THEN 'VERY_CLOSE'
        WHEN ABS(p1.reputation_score - p2.reputation_score) < 0.2 THEN 'CLOSE'
        WHEN ABS(p1.reputation_score - p2.reputation_score) < 0.4 THEN 'MODERATE'
        ELSE 'DISTANT'
    END as similarity_category
FROM participants p1
CROSS JOIN participants p2
WHERE p1.id < p2.id; -- Avoid duplicates and self-comparison
```

### 2. Event Context Similarity

**Detection Capability:** High

**Justification:**
- The `category`, `forma`, `cause`, `develop`, `effect` tables form the semantic context space
- The `context` table links context elements to events
- The `context_effectiveness_metrics` view allows analyzing effectiveness of different contexts
- Comparing used contexts allows determining participant approach similarities

**Implementation:**
```sql
-- Example of determining context similarity
SELECT 
    te1.participant_id as participant1_id,
    te2.participant_id as participant2_id,
    COUNT(*) as common_context_elements,
    CASE
        WHEN COUNT(*) > 10 THEN 'HIGH_SIMILARITY'
        WHEN COUNT(*) > 5 THEN 'MODERATE_SIMILARITY'
        WHEN COUNT(*) > 1 THEN 'LOW_SIMILARITY'
        ELSE 'NO_SIMILARITY'
    END as context_similarity
FROM truth_event te1
JOIN truth_event te2 ON te1.category_id = te2.category_id
    AND te1.forma_id = te2.forma_id
    AND te1.cause_id = te2.cause_id
    AND te1.develop_id = te2.develop_id
    AND te1.effect_id = te2.effect_id
    AND te1.participant_id != te2.participant_id
GROUP BY te1.participant_id, te2.participant_id;
```

### 3. Judgment Similarity (judgment table)

**Detection Capability:** High

**Justification:**
- The `judgment` table contains participant judgments about events
- Fields `assessment`, `confidence_level`, `reasoning` characterize judgments
- Comparing judgments on the same events allows determining opinion similarity
- The `judgment_links` table allows analyzing relationships between judgments

**Implementation:**
```sql
-- Example of determining judgment similarity
SELECT 
    j1.participant_id as participant1_id,
    j2.participant_id as participant2_id,
    COUNT(*) as matching_judgments,
    AVG(ABS(j1.confidence_level - j2.confidence_level)) as avg_confidence_diff,
    CASE
        WHEN AVG(ABS(j1.confidence_level - j2.confidence_level)) < 0.1 THEN 'HIGH_AGREEMENT'
        WHEN AVG(ABS(j1.confidence_level - j2.confidence_level)) < 0.3 THEN 'MODERATE_AGREEMENT'
        ELSE 'LOW_AGREEMENT'
    END as agreement_level
FROM judgment j1
JOIN judgment j2 ON j1.event_id = j2.event_id
    AND j1.participant_id != j2.participant_id
    AND j1.assessment = j2.assessment
GROUP BY j1.participant_id, j2.participant_id;
```

### 4. Impact Similarity (impact table)

**Detection Capability:** High

**Justification:**
- The `impact` table contains event consequence assessments
- Fields `value`, `trend`, `notes` characterize consequence perception
- Comparing consequence assessments on the same events allows determining similarity
- The `impact_links` table allows analyzing relationships between consequences

**Implementation:**
```sql
-- Example of determining impact similarity
SELECT 
    i1.participant_id as participant1_id,
    i2.participant_id as participant2_id,
    COUNT(*) as matching_impacts,
    AVG(ABS(i1.value - i2.value)) as avg_impact_diff,
    CASE
        WHEN AVG(ABS(i1.value - i2.value)) = 0 THEN 'IDENTICAL'
        WHEN AVG(ABS(i1.value - i2.value)) < 0.2 THEN 'HIGH_SIMILARITY'
        WHEN AVG(ABS(i1.value - i2.value)) < 0.5 THEN 'MODERATE_SIMILARITY'
        ELSE 'LOW_SIMILARITY'
    END as similarity_level
FROM impact i1
JOIN impact i2 ON i1.event_id = i2.event_id
    AND i1.participant_id != i2.participant_id
GROUP BY i1.participant_id, i2.participant_id;
```

### 5. Event Chain Similarity (event_links table)

**Detection Capability:** Medium

**Justification:**
- The `event_links` table allows building graphs of related events
- Analyzing relationships structure between events may reveal approach similarities
- Comparing event graphs of different participants allows determining chain similarities
- However, graph analysis requires complex algorithms

**Implementation:**
```sql
-- Example of determining event chain similarity
WITH participant_event_graphs AS (
    SELECT 
        te.participant_id,
        el.source_impact_id,
        el.target_impact_id,
        el.relation_type
    FROM truth_event te
    JOIN event_links el ON te.id = el.source_impact_id
)
SELECT 
    g1.participant_id as participant1_id,
    g2.participant_id as participant2_id,
    COUNT(*) as common_links,
    CASE
        WHEN COUNT(*) > 10 THEN 'HIGH_GRAPH_SIMILARITY'
        WHEN COUNT(*) > 3 THEN 'MODERATE_GRAPH_SIMILARITY'
        ELSE 'LOW_GRAPH_SIMILARITY'
    END as graph_similarity
FROM participant_event_graphs g1
JOIN participant_event_graphs g2 ON g1.source_impact_id = g2.source_impact_id
    AND g1.target_impact_id = g2.target_impact_id
    AND g1.relation_type = g2.relation_type
    AND g1.participant_id != g2.participant_id
GROUP BY g1.participant_id, g2.participant_id;
```

### 6. Judgment Chain Similarity (judgment_links table)

**Detection Capability:** Medium

**Justification:**
- The `judgment_links` table allows building graphs of related judgments
- Analyzing relationships between judgments may reveal logical approach similarities
- Comparing judgment graphs of different participants allows determining similarity
- Requires complex graph analysis

**Implementation:**
```sql
-- Example of determining judgment chain similarity
WITH participant_judgment_graphs AS (
    SELECT 
        j.participant_id,
        jl.source_judgment_id,
        jl.target_judgment_id,
        jl.relation_type
    FROM judgment j
    JOIN judgment_links jl ON j.id = jl.source_judgment_id
)
SELECT 
    g1.participant_id as participant1_id,
    g2.participant_id as participant2_id,
    COUNT(*) as common_judgment_links,
    CASE
        WHEN COUNT(*) > 5 THEN 'HIGH_JUDGMENT_CHAIN_SIMILARITY'
        WHEN COUNT(*) > 2 THEN 'MODERATE_JUDGMENT_CHAIN_SIMILARITY'
        ELSE 'LOW_JUDGMENT_CHAIN_SIMILARITY'
    END as chain_similarity
FROM participant_judgment_graphs g1
JOIN participant_judgment_graphs g2 ON g1.source_judgment_id = g2.source_judgment_id
    AND g1.target_judgment_id = g2.target_judgment_id
    AND g1.relation_type = g2.relation_type
    AND g1.participant_id != g2.participant_id
GROUP BY g1.participant_id, g2.participant_id;
```

### 7. Impact Chain Similarity (impact_links table)

**Detection Capability:** Medium

**Justification:**
- The `impact_links` table allows building graphs of related consequences
- Analyzing relationships between consequences may reveal cause-effect approach similarities
- Comparing consequence graphs of different participants allows determining similarity
- Also requires complex graph analysis

**Implementation:**
```sql
-- Example of determining impact chain similarity
WITH participant_impact_graphs AS (
    SELECT 
        i.participant_id,
        il.source_impact_id,
        il.target_impact_id,
        il.relation_type
    FROM impact i
    JOIN impact_links il ON i.id = il.source_impact_id
)
SELECT 
    g1.participant_id as participant1_id,
    g2.participant_id as participant2_id,
    COUNT(*) as common_impact_links,
    CASE
        WHEN COUNT(*) > 5 THEN 'HIGH_IMPACT_CHAIN_SIMILARITY'
        WHEN COUNT(*) > 2 THEN 'MODERATE_IMPACT_CHAIN_SIMILARITY'
        ELSE 'LOW_IMPACT_CHAIN_SIMILARITY'
    END as chain_similarity
FROM participant_impact_graphs g1
JOIN participant_impact_graphs g2 ON g1.source_impact_id = g2.source_impact_id
    AND g1.target_impact_id = g2.target_impact_id
    AND g1.relation_type = g2.relation_type
    AND g1.participant_id != g2.participant_id
GROUP BY g1.participant_id, g2.participant_id;
```

## Conclusions

The Truth Training system provides extensive opportunities for analyzing information exchange participant parameters:

1. **Node Discovery** allows determining:
   - Geographic proximity (limited, through node types and IP addresses)
   - Interaction density (high, through synchronization history)
   - Temporal synchronization (high, through analysis of synchronization timestamps and time scales of events, consequences and judgments)

2. **Collective Intelligence** allows determining:
   - Participant reputation similarity (high, through comparison of reputation metrics)
   - Event context similarity (high, through comparison of used categories and types)
   - Judgment similarity (high, through comparison of assessments on the same events)
   - Impact similarity (high, through comparison of impact assessments)
   - Event, judgment and impact chain similarity (medium, through graph analysis)

All these capabilities are implemented through SQL queries to the appropriate tables and views, making the system flexible for analytical tasks. It is particularly worth noting that with the introduction of timeline tables (event_timeline, impact_timeline, judgment_timeline) in version v1.1.0 and their synchronization through P2P exchange, the possibilities for analyzing temporal synchronization of system participants have significantly expanded.