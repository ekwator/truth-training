-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Analytics on Event Timelines to Understand Temporal Patterns  

-- Provides analytics on event timelines to understand temporal patterns in truth evolution and impact manifestation
-- Links: truth_event.id → event_ci.created_by → truth_event.timeline_id → event_timeline.id, impact.timeline_id → impact_timeline.id, judgment.timeline_id → judgment_timeline.id
```sql
CREATE VIEW event_timeline_analytics AS
SELECT
    te.id as event_id,
    te.description,
    te.created_at as event_created_at,
    -- Get timeline information
    et.t_start as event_t_start,
    et.t_end as event_t_end,
    -- Calculate timeline duration
    CASE
        WHEN et.t_end IS NOT NULL THEN et.t_end - et.t_start
        ELSE (SELECT strftime('%s', 'now')) - et.t_start
    END as event_duration_seconds,
    -- Count of judgments and impacts over time
    (SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as total_judgments,
    (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as total_impacts,
    -- First and last judgment/impact times
    (SELECT MIN(j.created_at) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as first_judgment_time,
    (SELECT MAX(j.created_at) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as last_judgment_time,
    (SELECT MIN(i.timeline_id) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as first_impact_time,
    (SELECT MAX(i.timeline_id) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as last_impact_time,
    -- Time from event creation to first judgment/impact
    (SELECT MIN(j.created_at) - te.created_at FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as time_to_first_judgment,
    (SELECT MIN(i.timeline_id) - te.created_at FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as time_to_first_impact,
    -- Truth evolution metrics
    te.collective_score as current_truth_score,
    te.impact_score as current_impact_score,
    te.judgment_score as current_judgment_score,
    -- Stability indicators
    (SELECT COUNT(*) FROM event_state_history esh WHERE esh.event_id = (SELECT id FROM event_ci WHERE created_by = te.id LIMIT 1)) as assessment_history_points,
    -- Timeline phase classification
    CASE
        WHEN et.t_end IS NULL OR et.t_end > (SELECT strftime('%s', 'now')) THEN 'ACTIVE'
        WHEN et.t_end <= (SELECT strftime('%s', 'now')) AND et.t_end > (SELECT strftime('%s', 'now') - 86400*7) THEN 'RECENTLY_ENDED'
        ELSE 'ENDED_LONG_AGO'
    END as timeline_phase,
    -- Engagement level based on activity
    CASE
        WHEN (SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) + 
             (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) >= 10 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) + 
             (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) >= 5 THEN 'MEDIUM'
        ELSE 'LOW'
    END as engagement_level,
    -- Rate of assessment
    CASE
        WHEN (et.t_end IS NOT NULL AND et.t_end > et.t_start) THEN
            ((SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) + 
             (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id)) * 1.0 / 
            (et.t_end - et.t_start + 1) * 86400  -- Per day
        WHEN et.t_end IS NULL THEN
            ((SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) + 
             (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id)) * 1.0 / 
            ((SELECT strftime('%s', 'now')) - et.t_start + 1) * 86400  -- Per day
        ELSE 0
    END as assessments_per_day,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM truth_event te
JOIN event_timeline et ON te.timeline_id = et.id;
```

-- View for analyzing truth evolution over time
-- This view tracks how truth assessments change over the event timeline
```sql
CREATE VIEW truth_evolution_analytics AS
SELECT
    eta.event_id,
    eta.description,
    eta.event_created_at,
    eta.event_duration_seconds,
    eta.current_truth_score,
    eta.current_impact_score,
    eta.current_judgment_score,
    -- Calculate truth volatility (how much the truth score has changed)
    (SELECT AVG(ABS(esh.truth_score - LAG(esh.truth_score, 1, esh.truth_score) OVER (ORDER BY esh.recorded_at)))
     FROM event_state_history esh 
     WHERE esh.event_id = (SELECT id FROM event_ci WHERE created_by = eta.event_id LIMIT 1)
    ) as truth_volatility,
    -- Rate of truth convergence
    CASE
        WHEN eta.assessment_history_points > 1 THEN
            ABS(eta.current_truth_score - (SELECT truth_score FROM event_state_history WHERE event_id = (SELECT id FROM event_ci WHERE created_by = eta.event_id LIMIT 1) ORDER BY recorded_at LIMIT 1 OFFSET 0)) / 
            eta.assessment_history_points
        ELSE 0
    END as truth_convergence_rate,
    -- Direction of truth change
    CASE
        WHEN eta.current_truth_score > (SELECT truth_score FROM event_state_history WHERE event_id = (SELECT id FROM event_ci WHERE created_by = eta.event_id LIMIT 1) ORDER BY recorded_at DESC LIMIT 1 OFFSET 0) THEN 'INCREASING'
        WHEN eta.current_truth_score < (SELECT truth_score FROM event_state_history WHERE event_id = (SELECT id FROM event_ci WHERE created_by = eta.event_id LIMIT 1) ORDER BY recorded_at DESC LIMIT 1 OFFSET 0) THEN 'DECREASING'
        ELSE 'STABLE'
    END as truth_trend_direction,
    -- Impact manifestation pattern
    CASE
        WHEN eta.current_impact_score > 0.5 THEN 'HIGH_IMPACT_MANIFESTED'
        WHEN eta.current_impact_score > 0 THEN 'MODERATE_IMPACT_MANIFESTED'
        WHEN eta.current_impact_score < 0 THEN 'NEGATIVE_IMPACT_MANIFESTED'
        ELSE 'LOW_OR_NO_IMPACT_MANIFESTED'
    END as impact_manifestation_pattern,
    -- Time from event to truth stabilization
    (SELECT MIN(esh.recorded_at) - eta.event_created_at
     FROM event_state_history esh
     WHERE esh.event_id = (SELECT id FROM event_ci WHERE created_by = eta.event_id LIMIT 1)
       AND ABS(esh.truth_score - eta.current_truth_score) < 0.05  -- Close to current value
       AND esh.recorded_at > (SELECT MIN(recorded_at) FROM event_state_history WHERE event_id = esh.event_id)  -- After initial fluctuation
    ) as time_to_truth_stabilization,
    -- Judgment vs Impact timing differences
    eta.time_to_first_judgment,
    eta.time_to_first_impact,
    CASE
        WHEN eta.time_to_first_judgment IS NOT NULL AND eta.time_to_first_impact IS NOT NULL THEN
            ABS(eta.time_to_first_judgment - eta.time_to_first_impact)
        ELSE NULL
    END as judgment_impact_timing_difference,
    -- Assessment saturation (how many assessments relative to event duration)
    eta.assessments_per_day,
    -- Classification based on timeline characteristics
    CASE
        WHEN eta.timeline_phase = 'ACTIVE' AND eta.engagement_level = 'HIGH' AND eta.assessments_per_day > 5 THEN 'HIGHLY_ACTIVE_EVOLVING_EVENT'
        WHEN eta.timeline_phase = 'ACTIVE' AND eta.engagement_level = 'LOW' THEN 'QUIET_EVENT'
        WHEN eta.timeline_phase = 'RECENTLY_ENDED' AND eta.truth_volatility < 0.1 THEN 'STABLE_CONCLUSION'
        WHEN eta.timeline_phase = 'RECENTLY_ENDED' AND eta.truth_volatility > 0.3 THEN 'CONTROVERSIAL_CONCLUSION'
        WHEN eta.assessments_per_day > 10 THEN 'HIGHLY_CONTENTIOUS_EVENT'
        ELSE 'TYPICAL_EVENT'
    END as event_classification
FROM event_timeline_analytics eta;
```

-- View for identifying temporal patterns in event assessment
-- This view looks for common patterns in how events are assessed over time
```sql
CREATE VIEW temporal_pattern_analysis AS
WITH event_assessment_milestones AS (
    SELECT 
        te.id as event_id,
        -- Time to first assessment
        (SELECT MIN(j.created_at) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as first_judgment,
        (SELECT MIN(i.timeline_id) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as first_impact,
        -- Time to 50% of total assessments
        (SELECT created_at FROM (
            SELECT j.created_at, ROW_NUMBER() OVER (ORDER BY j.created_at) as rn
            FROM judgment j JOIN event_ci ec ON j.event_id = ec.id 
            WHERE ec.created_by = te.id
            ORDER BY j.created_at
        ) WHERE rn = (SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) / 2) as halfway_judgment_time,
        (SELECT timeline_id FROM (
            SELECT i.timeline_id, ROW_NUMBER() OVER (ORDER BY i.timeline_id) as rn
            FROM impact i JOIN event_ci ec ON i.event_id = ec.id 
            WHERE ec.created_by = te.id
            ORDER BY i.timeline_id
        ) WHERE rn = (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) / 2) as halfway_impact_time,
        -- Peak assessment activity
        (SELECT created_at FROM (
            SELECT j.created_at, 
                   COUNT(*) OVER (PARTITION BY date(j.created_at, 'unixepoch', 'localtime')) as daily_count
            FROM judgment j JOIN event_ci ec ON j.event_id = ec.id 
            WHERE ec.created_by = te.id
            ORDER BY daily_count DESC
            LIMIT 1
        )) as peak_judgment_date,
        (SELECT timeline_id FROM (
            SELECT i.timeline_id, 
                   COUNT(*) OVER (PARTITION BY date(i.timeline_id, 'unixepoch', 'localtime')) as daily_count
            FROM impact i JOIN event_ci ec ON i.event_id = ec.id 
            WHERE ec.created_by = te.id
            ORDER BY daily_count DESC
            LIMIT 1
        )) as peak_impact_date
    FROM truth_event te
)
SELECT
    eam.event_id,
    -- Time to first assessment
    eam.first_judgment - te.created_at as time_to_first_judgment,
    eam.first_impact - te.created_at as time_to_first_impact,
    -- Time to halfway point
    eam.halfway_judgment_time - te.created_at as time_to_halfway_judgment,
    eam.halfway_impact_time - te.created_at as time_to_halfway_impact,
    -- Peak activity timing
    eam.peak_judgment_date - te.created_at as time_to_peak_judgment,
    eam.peak_impact_date - te.created_at as time_to_peak_impact,
    -- Pattern classification
    CASE
        WHEN (eam.first_judgment - te.created_at) < 3600 THEN 'INSTANT_REACTION_EVENT'  -- Within 1 hour
        WHEN (eam.first_judgment - te.created_at) < 86400 THEN 'RAPID_RESPONSE_EVENT'   -- Within 1 day
        WHEN (eam.first_judgment - te.created_at) < 86400*7 THEN 'DELAYED_RESPONSE_EVENT' -- Within 1 week
        ELSE 'LATE_RESPONSE_EVENT'
    END as response_pattern,
    CASE
        WHEN ABS((eam.first_judgment - te.created_at) - (eam.first_impact - te.created_at)) < 3600 THEN 'SYNCHRONIZED_ASSESSMENT'
        WHEN (eam.first_judgment - te.created_at) < (eam.first_impact - te.created_at) THEN 'TRUTH_FIRST_ASSESSMENT'
        ELSE 'IMPACT_FIRST_ASSESSMENT'
    END as assessment_sequence_pattern,
    -- Duration of intensive assessment period
    CASE
        WHEN eam.halfway_judgment_time IS NOT NULL AND eam.first_judgment IS NOT NULL
        THEN eam.halfway_judgment_time - eam.first_judgment
        ELSE NULL
    END as intensive_judgment_period,
    CASE
        WHEN eam.halfway_impact_time IS NOT NULL AND eam.first_impact IS NOT NULL
        THEN eam.halfway_impact_time - eam.first_impact
        ELSE NULL
    END as intensive_impact_period,
    -- How concentrated the assessments are
    CASE
        WHEN eam.peak_judgment_date IS NOT NULL AND eam.first_judgment IS NOT NULL
        THEN (eam.peak_judgment_date - eam.first_judgment) * 1.0 / 
             (SELECT MAX(j.created_at) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) - eam.first_judgment + 1
        ELSE NULL
    END as judgment_concentration_ratio,
    CASE
        WHEN eam.peak_impact_date IS NOT NULL AND eam.first_impact IS NOT NULL
        THEN (eam.peak_impact_date - eam.first_impact) * 1.0 / 
             (SELECT MAX(i.timeline_id) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) - eam.first_impact + 1
        ELSE NULL
    END as impact_concentration_ratio
FROM event_assessment_milestones eam
JOIN truth_event te ON eam.event_id = te.id;
```

-- View for event lifecycle stage analysis
-- This view categorizes events based on their current stage in the assessment lifecycle
```sql
CREATE VIEW event_lifecycle_stage AS
SELECT
    tea.event_id,
    tea.description,
    tea.event_classification,
    tea.timeline_phase,
    tea.engagement_level,
    tea.truth_trend_direction,
    -- Determine lifecycle stage
    CASE
        -- Early stage: recently created with few assessments
        WHEN (SELECT julianday('now') - julianday(tea.event_created_at, 'unixepoch')) < 1 
             AND tea.total_judgments + tea.total_impacts < 5 THEN 'EMERGENCE'
        -- Active stage: ongoing assessment activity
        WHEN tea.timeline_phase = 'ACTIVE' 
             AND tea.engagement_level IN ('HIGH', 'MEDIUM') 
             AND ABS(tea.truth_convergence_rate) > 0.05 THEN 'FLUX'
        -- Convergence stage: assessments stabilizing
        WHEN tea.assessment_history_points > 3 
             AND tea.truth_volatility < 0.1 
             AND ABS(tea.truth_convergence_rate) < 0.02 THEN 'CONVERGENCE'
        -- Stable stage: truth has stabilized
        WHEN tea.assessment_history_points > 5 
             AND tea.truth_volatility < 0.05 
             AND tea.engagement_level = 'LOW' THEN 'STABLE'
        -- Controversial: high volatility, high engagement
        WHEN tea.truth_volatility > 0.2 
             AND tea.engagement_level = 'HIGH' THEN 'CONTROVERSY'
        -- Dormant: low activity despite age
        WHEN (SELECT julianday('now') - julianday(tea.event_created_at, 'unixepoch')) > 30 
             AND tea.engagement_level = 'LOW' THEN 'DORMANT'
        ELSE 'REGULAR'
    END as lifecycle_stage,
    -- Estimated time to convergence
    CASE
        WHEN tea.lifecycle_stage = 'EMERGENCE' THEN 'NEAR_TERM'
        WHEN tea.lifecycle_stage = 'FLUX' THEN 'MEDIUM_TERM'
        WHEN tea.lifecycle_stage = 'CONVERGENCE' THEN 'SHORT_TERM'
        WHEN tea.lifecycle_stage = 'STABLE' THEN 'ALREADY_STABLE'
        WHEN tea.lifecycle_stage = 'CONTROVERSY' THEN 'LONG_TERM_OR_NEVER'
        WHEN tea.lifecycle_stage = 'DORMANT' THEN 'UNKNOWN'
        ELSE 'STANDARD_TIMELINE'
    END as convergence_timeline,
    -- Priority for monitoring
    CASE
        WHEN tea.lifecycle_stage IN ('EMERGENCE', 'FLUX', 'CONTROVERSY') THEN 'HIGH'
        WHEN tea.lifecycle_stage = 'CONVERGENCE' THEN 'MEDIUM'
        ELSE 'LOW'
    END as monitoring_priority,
    -- Predicted stability date
    CASE
        WHEN tea.lifecycle_stage IN ('EMERGENCE', 'FLUX') THEN
            date('now', '+' || CAST((tea.time_to_truth_stabilization / 86400) AS INTEGER) || ' days')
        WHEN tea.lifecycle_stage = 'CONVERGENCE' THEN
            date('now', '+' || CAST((tea.assessments_per_day > 0 ? 7 : 1) AS INTEGER) || ' days')
        ELSE 'ALREADY_STABLE'
    END as predicted_stability_date,
    -- Assessment maturity index (0-1 scale)
    LEAST((tea.total_judgments + tea.total_impacts) * 1.0 / 20, 1.0) as assessment_maturity
FROM truth_evolution_analytics tea;