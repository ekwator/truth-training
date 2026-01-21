-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Collective Event Assessment Logic  

-- Function to calculate event truthfulness as statistical function (local)  
-- cs_i = f-local(I(E_i))  
-- I_i(E_i) = {I_i(1), I_i(2), ..., I_i(n)} - set of participant impact  
-- Divide by sign: P_i = ΣI_i(ij)^(+), N_i = ΣI_i(ij)^(-)  
```sql
CREATE VIEW local_collective_score_calculation AS
SELECT 
    te.id as event_id,
    te.collective_score as cs_i,
    -- Calculate positive and negative impact sums
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) as P_i,
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0) as N_i,
    -- Calculate total impact count
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) as total_impact_count
FROM truth_event te;
```
-- Function to calculate event truthfulness globally  
-- truth_score_i-global = f-global({ cs_i-local_j })  
-- {cs_i-local_j} - local assessments of different nodes  
```sql
CREATE VIEW global_truth_score_calculation AS
SELECT 
    s.event_id,
    AVG(s.truth_score) as global_truth_score,
    COUNT(s.truth_score) as node_count,
    -- Calculate the global truth score based on local collective scores
    (SELECT AVG(collective_score) FROM truth_event WHERE global_id = (
        SELECT global_id FROM truth_event WHERE id = s.event_id
    )) as aggregated_local_collective_score
FROM statements s
GROUP BY s.event_id;
```
-- Function to calculate event truthfulness (not stored but calculated)  
-- Truth(E_i) = (P_i - N_i) / (|I(E_i)| + ε)  
-- ε - protection from division by zero  
-- result ∈ (-1, +1)  
```sql
CREATE VIEW event_truthfulness_calculation AS
SELECT 
    te.id as event_id,
    te.description,
    -- Calculate P_i (positive impacts) and N_i (negative impacts)
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) as P_i,
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0) as N_i,
    -- Total impact count (|I(E_i)|)
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) as total_impact_count,
    -- Calculate truth with epsilon for division by zero protection
    CASE 
        WHEN (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) = 0 
        THEN 0.0
        ELSE ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) - 
              (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0)) * 1.0 / 
             ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) + (CASE
   WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
   THEN 0.000001
   ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
END))
    END as calculated_truth,
    -- Interpretation: +1: stably confirmed, -1: stably refuted, ≈0: conflict/lack of data
    CASE
        WHEN ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) -
              (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0)) * 1.0 /
             ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) + (CASE
   WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
   THEN 0.000001
   ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
END)) > 0.7
        THEN 'stably_confirmed'
        WHEN ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) -
              (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0)) * 1.0 /
             ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) + (CASE
   WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
   THEN 0.000001
   ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
END)) < -0.7
        THEN 'stably_refuted'
        ELSE 'conflict_or_lack_of_data'
    END as truth_interpretation
FROM truth_event te;
```
-- Function to calculate group ratings based on collective scores
```sql
CREATE VIEW group_ratings_calculation AS
SELECT
    p.group_membership as group_id,
    COUNT(p.public_key) as members,
    AVG(p.reputation_score) as avg_score,
    -- Calculate coherence as 1 - (sum of absolute deviations from mean / max possible deviation)
    1 - (SUM(ABS(p.reputation_score - AVG(p.reputation_score))) OVER (PARTITION BY p.group_membership) / (COUNT(*) OVER (PARTITION BY p.group_membership) * 2)) as coherence,
    (SELECT strftime('%s', 'now')) as last_updated
FROM participants p
WHERE p.group_membership IS NOT NULL
GROUP BY p.group_membership;
```

-- Function to calculate event projection in truth-impact space for classification
-- Uses pre-calculated aggregated values from consensus_ci and impact_metrics tables
-- This view supports the event_projection table by providing the calculated quadrant classification
```sql
CREATE VIEW event_projection_calculation AS
SELECT
    ec.id as event_id,
    COALESCE(cc.confidence_score, 0.5) as truth_score,
    COALESCE(im.total_magnitude, 0.0) as impact_score,
    CASE
        WHEN COALESCE(cc.confidence_score, 0.5) >= 0.5 AND COALESCE(im.total_magnitude, 0.0) >= 0 THEN 'Q1'
        WHEN COALESCE(cc.confidence_score, 0.5) >= 0.5 AND COALESCE(im.total_magnitude, 0.0) < 0 THEN 'Q2'
        WHEN COALESCE(cc.confidence_score, 0.5) < 0.5 AND COALESCE(im.total_magnitude, 0.0) >= 0 THEN 'Q3'
        ELSE 'Q4'
    END as calculated_quadrant,
    (SELECT strftime('%s', 'now')) as calculated_at
FROM event_ci ec
LEFT JOIN consensus_ci cc ON ec.id = cc.event_id
LEFT JOIN impact_metrics im ON ec.id = im.event_id;
```