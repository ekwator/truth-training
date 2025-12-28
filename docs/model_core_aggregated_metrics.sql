-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved
-- SQL Model for Aggregated System Metrics and Expert Functions
-- Based on docs/model_core.md:2420-2689

-- Table for aggregated system progress metrics
CREATE TABLE progress_metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    total_events INTEGER NOT NULL,
    total_events_group INTEGER NOT NULL,
    total_positive_impacts REAL NOT NULL,
    total_positive_impacts_group REAL NOT NULL,
    total_negative_impacts REAL NOT NULL,
    total_negative_impact_group REAL NOT NULL,
    trend REAL NOT NULL,
    trend_group REAL NOT NULL,
    last_updated INTEGER NOT NULL
);

-- Function to calculate system trend
-- Trend = (Σ P - Σ N) / total_events
-- Where P — positive impacts, N — negative impacts
CREATE VIEW system_trend_calculation AS
SELECT 
    total_events,
    total_positive_impacts,
    total_negative_impacts,
    CASE 
        WHEN total_events > 0 
        THEN (total_positive_impacts - total_negative_impacts) / total_events
        ELSE 0.0
    END as calculated_trend,
    total_positive_impacts - total_negative_impacts as impact_balance
FROM progress_metrics;

-- Function to calculate group vs individual comparison
CREATE VIEW group_efficiency_calculation AS
SELECT 
    total_events,
    total_events_group,
    CASE 
        WHEN total_events > 0 AND (total_events_group * 1.0 / total_events) > 0.5
        THEN 'HIGH'  -- group collaboration effective
        ELSE 'LOW'   -- individual assessment dominant
    END as system_efficiency,
    (total_events_group * 1.0 / total_events) as group_to_total_ratio
FROM progress_metrics;

-- Table for linking judgment to applied heuristics
CREATE TABLE judgment_heuristics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    judgment_id INTEGER NOT NULL,
    heuristic_id INTEGER NOT NULL,
    influence REAL NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (judgment_id) REFERENCES judgment(id),
    FOREIGN KEY (heuristic_id) REFERENCES expert_heuristics(id)
);

-- Function to calculate heuristic influence on judgments
CREATE VIEW heuristic_influence_calculation AS
SELECT 
    jh.judgment_id,
    jh.heuristic_id,
    eh.name as heuristic_name,
    eh.domain as heuristic_domain,
    jh.influence,
    -- Calculate cumulative influence of all heuristics on a judgment
    SUM(jh.influence) OVER (PARTITION BY jh.judgment_id) as total_influence_per_judgment
FROM judgment_heuristics jh
JOIN expert_heuristics eh ON jh.heuristic_id = eh.id;

-- Table for storing expert heuristics
CREATE TABLE expert_heuristics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    domain TEXT NOT NULL,
    weight REAL NOT NULL,
    confidence REAL NOT NULL,
    proven_accuracy REAL NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Function to calculate heuristic weight based on accuracy
-- w_i = f(accuracy_i, reliability_i, domain_relevance_i)
CREATE TRIGGER update_heuristic_weight
AFTER UPDATE ON expert_heuristics
FOR EACH ROW
WHEN NEW.proven_accuracy != OLD.proven_accuracy
BEGIN
    UPDATE expert_heuristics 
    SET 
        weight = CASE 
            WHEN NEW.proven_accuracy > 0.8 THEN 1.0 -- HIGH
            WHEN NEW.proven_accuracy > 0.6 THEN 0.7  -- MEDIUM
            ELSE 0.3  -- LOW
        END,
        updated_at = (SELECT strftime('%s', 'now'))
    WHERE id = NEW.id;
END;

-- Function to apply heuristics based on confidence threshold
CREATE VIEW applicable_heuristics AS
SELECT 
    eh.id,
    eh.name,
    eh.domain,
    eh.weight,
    eh.confidence,
    eh.proven_accuracy,
    CASE 
        WHEN eh.confidence > 0.7 THEN eh.weight
        ELSE eh.weight * 0.5  -- reduced weight
    END as effective_weight
FROM expert_heuristics eh;

-- Function to detect conflicting heuristics
CREATE VIEW conflicting_heuristics AS
SELECT 
    jh1.judgment_id,
    jh1.heuristic_id as heuristic1_id,
    jh2.heuristic_id as heuristic2_id,
    eh1.name as heuristic1_name,
    eh2.name as heuristic2_name,
    'CONFLICT_DETECTED' as conflict_status
FROM judgment_heuristics jh1
JOIN judgment_heuristics jh2 ON jh1.judgment_id = jh2.judgment_id
JOIN expert_heuristics eh1 ON jh1.heuristic_id = eh1.id
JOIN expert_heuristics eh2 ON jh2.heuristic_id = eh2.id
WHERE jh1.heuristic_id != jh2.heuristic_id
  AND ((eh1.domain = eh2.domain AND eh1.name != eh2.name) 
       OR (eh1.confidence > 0.8 AND eh2.confidence > 0.8 AND eh1.weight > 0.8 AND eh2.weight > 0.8));

-- Function to calculate final event assessment as aggregated function
-- Final event assessment is aggregated function of all applied heuristics and judgment
CREATE VIEW final_event_assessment AS
SELECT 
    ec.id as event_id,
    -- Combine judgment confidence with heuristic influences
    COALESCE(cc.confidence_score, 0) as base_confidence,
    COALESCE(SUM(jh.influence), 0) as total_heuristic_influence,
    (COALESCE(cc.confidence_score, 0) + COALESCE(SUM(jh.influence), 0)) / 2 as combined_assessment
FROM event_ci ec
LEFT JOIN consensus_ci cc ON ec.id = cc.event_id
LEFT JOIN judgment j ON ec.id = j.event_id
LEFT JOIN judgment_heuristics jh ON j.id = jh.judgment_id
GROUP BY ec.id, cc.confidence_score;

-- Domain classification for heuristics
-- "logic" — formal logic rules
-- "statistical" — statistical inference rules
-- "empirical" — experience-based rules
-- "contextual" — context-sensitive rules
-- "domain_specific" — specialized knowledge rules
CREATE VIEW domain_classification_stats AS
SELECT 
    domain,
    COUNT(*) as heuristic_count,
    AVG(weight) as avg_weight,
    AVG(confidence) as avg_confidence,
    AVG(proven_accuracy) as avg_accuracy
FROM expert_heuristics
GROUP BY domain;

-- Function to update progress metrics when new event is processed
CREATE TRIGGER update_progress_metrics_after_event
AFTER INSERT ON truth_event
BEGIN
    INSERT OR REPLACE INTO progress_metrics (
        id,
        total_events,
        total_events_group,
        total_positive_impacts,
        total_positive_impacts_group,
        total_negative_impacts,
        total_negative_impact_group,
        trend,
        trend_group,
        last_updated
    )
    SELECT 
        1,  -- Single record for overall metrics
        (SELECT COUNT(*) FROM truth_event) as total_events,
        (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
            SELECT public_key FROM participants WHERE group_membership IS NOT NULL
        )) as total_events_group,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) as total_positive_impacts,
        (SELECT SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END) 
         FROM impact i 
         JOIN truth_event te ON i.event_id = te.id 
         WHERE te.participant_id IN (
             SELECT public_key FROM participants WHERE group_membership IS NOT NULL
         )) as total_positive_impacts_group,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact) as total_negative_impacts,
        (SELECT SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END) 
         FROM impact i 
         JOIN truth_event te ON i.event_id = te.id 
         WHERE te.participant_id IN (
             SELECT public_key FROM participants WHERE group_membership IS NOT NULL
         )) as total_negative_impact_group,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event) > 0 
            THEN ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) - 
                  (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) * 1.0 / 
                 (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END as trend,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                SELECT public_key FROM participants WHERE group_membership IS NOT NULL
            )) > 0
            THEN ((SELECT SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END) 
                   FROM impact i 
                   JOIN truth_event te ON i.event_id = te.id 
                   WHERE te.participant_id IN (
                       SELECT public_key FROM participants WHERE group_membership IS NOT NULL
                   )) - 
                  (SELECT SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END) 
                   FROM impact i 
                   JOIN truth_event te ON i.event_id = te.id 
                   WHERE te.participant_id IN (
                       SELECT public_key FROM participants WHERE group_membership IS NOT NULL
                   ))) * 1.0 / 
                 (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                     SELECT public_key FROM participants WHERE group_membership IS NOT NULL
                 ))
            ELSE 0.0
        END as trend_group,
        (SELECT strftime('%s', 'now')) as last_updated
    WHERE NOT EXISTS (SELECT 1 FROM progress_metrics WHERE id = 1);
END;

-- Function to update progress metrics when new impact is recorded
CREATE TRIGGER update_progress_metrics_after_impact
AFTER INSERT ON impact
BEGIN
    UPDATE progress_metrics 
    SET 
        total_positive_impacts = (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact),
        total_negative_impacts = (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact),
        trend = CASE 
            WHEN (SELECT COUNT(*) FROM truth_event) > 0 
            THEN ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) - 
                  (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) * 1.0 / 
                 (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END,
        last_updated = (SELECT strftime('%s', 'now'))
    WHERE id = 1;
END;

-- Indexes for performance optimization
CREATE INDEX idx_progress_metrics_last_updated ON progress_metrics(last_updated);
CREATE INDEX idx_judgment_heuristics_judgment_id ON judgment_heuristics(judgment_id);
CREATE INDEX idx_judgment_heuristics_heuristic_id ON judgment_heuristics(heuristic_id);
CREATE INDEX idx_expert_heuristics_domain ON expert_heuristics(domain);
CREATE INDEX idx_expert_heuristics_updated_at ON expert_heuristics(updated_at);