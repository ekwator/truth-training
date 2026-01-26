-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for System Health Metrics and Performance Indicators  

-- Provides system-wide health metrics and performance indicators
-- Aggregates data from various system tables to assess overall system stability and performance
-- Links: Various tables including participants, truth_event, impact, judgment, event_ci, etc.
```sql
CREATE VIEW system_health_monitoring AS
SELECT
    -- Overall system metrics
    (SELECT COUNT(*) FROM participants) as total_participants,
    (SELECT COUNT(*) FROM truth_event) as total_events,
    (SELECT COUNT(*) FROM impact) as total_impacts,
    (SELECT COUNT(*) FROM judgment) as total_judgments,
    (SELECT COUNT(*) FROM event_ci) as total_event_neurons,
    (SELECT COUNT(*) FROM event_links) as total_event_relationships,
    
    -- Activity metrics
    (SELECT COUNT(*) FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400)) as events_last_24h,
    (SELECT COUNT(*) FROM impact WHERE signature IS NOT NULL AND timeline_id > (SELECT strftime('%s', 'now') - 86400)) as impacts_last_24h,
    (SELECT COUNT(*) FROM judgment WHERE signature IS NOT NULL AND timeline_id > (SELECT strftime('%s', 'now') - 86400)) as judgments_last_24h,
    
    -- Data quality metrics
    (SELECT AVG(reputation_score) FROM participants) as average_participant_reputation,
    (SELECT MIN(reputation_score) FROM participants) as min_participant_reputation,
    (SELECT MAX(reputation_score) FROM participants) as max_participant_reputation,
    
    -- System stability metrics
    (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NOT NULL) as events_with_scores,
    (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL) as average_collective_score,
    (SELECT COUNT(*) FROM truth_event WHERE impact_score IS NOT NULL) as events_with_impact_scores,
    (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL) as average_impact_score,
    (SELECT COUNT(*) FROM truth_event WHERE judgment_score IS NOT NULL) as events_with_judgment_scores,
    (SELECT AVG(judgment_score) FROM truth_event WHERE judgment_score IS NOT NULL) as average_judgment_score,
    
    -- Consensus metrics
    (SELECT COUNT(*) FROM consensus_ci) as total_consensus_records,
    (SELECT AVG(confidence_score) FROM consensus_ci) as average_consensus_confidence,
    
    -- Timeline metrics
    (SELECT COUNT(*) FROM truth_event WHERE timeline_id IS NOT NULL) as events_with_timelines,
    
    -- System health indicators
    CASE
        WHEN (SELECT COUNT(*) FROM participants) = 0 THEN 'CRITICAL'
        WHEN (SELECT AVG(reputation_score) FROM participants) < 0.3 THEN 'WARNING'
        WHEN (SELECT AVG(reputation_score) FROM participants) < 0.7 THEN 'FAIR'
        ELSE 'GOOD'
    END as participant_health_status,
    
    CASE
        WHEN (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NOT NULL) * 100.0 / 
             (SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE COUNT(*) END FROM truth_event) < 10 THEN 'CRITICAL'
        WHEN (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NOT NULL) * 100.0 / 
             (SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE COUNT(*) END FROM truth_event) < 30 THEN 'WARNING'
        WHEN (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NOT NULL) * 100.0 / 
             (SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE COUNT(*) END FROM truth_event) < 70 THEN 'FAIR'
        ELSE 'GOOD'
    END as scoring_health_status,
    
    -- Timestamp of analysis
    (SELECT strftime('%s', 'now')) as health_check_timestamp;
```

-- View for detailed system performance metrics
-- This view provides more granular performance indicators
```sql
CREATE VIEW system_performance_metrics AS
SELECT
    shm.total_participants,
    shm.total_events,
    shm.total_impacts,
    shm.total_judgments,
    -- Calculate engagement rates
    CASE
        WHEN shm.total_participants > 0 
        THEN shm.total_events * 1.0 / shm.total_participants
        ELSE 0
    END as events_per_participant,
    CASE
        WHEN shm.total_events > 0 
        THEN shm.total_impacts * 1.0 / shm.total_events
        ELSE 0
    END as impacts_per_event,
    CASE
        WHEN shm.total_events > 0 
        THEN shm.total_judgments * 1.0 / shm.total_events
        ELSE 0
    END as judgments_per_event,
    
    -- Growth metrics
    (SELECT COUNT(*) FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400*7)) as events_last_week,
    (SELECT COUNT(*) FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400*30)) as events_last_month,
    CASE
        WHEN (SELECT COUNT(*) FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400*14) AND created_at < (SELECT strftime('%s', 'now') - 86400*7)) > 0
        THEN (
            (SELECT COUNT(*) FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400*7)) * 1.0 / 
            (SELECT COUNT(*) FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400*14) AND created_at < (SELECT strftime('%s', 'now') - 86400*7)) - 1
        ) * 100
        ELSE 0
    END as weekly_growth_rate,
    
    -- Data completeness metrics
    (SELECT COUNT(*) FROM participants WHERE last_activity IS NOT NULL) as participants_with_activity,
    (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NOT NULL AND impact_score IS NOT NULL AND judgment_score IS NOT NULL) as events_with_complete_scores,
    CASE
        WHEN shm.total_events > 0
        THEN (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NOT NULL AND impact_score IS NOT NULL AND judgment_score IS NOT NULL) * 100.0 / shm.total_events
        ELSE 0
    END as data_completeness_percentage,
    
    -- Quality metrics
    shm.average_participant_reputation,
    shm.average_collective_score,
    shm.average_impact_score,
    shm.average_judgment_score,
    
    -- System load indicators
    (SELECT COUNT(*) FROM impact WHERE created_at > (SELECT strftime('%s', 'now') - 3600)) as impacts_last_hour,
    (SELECT COUNT(*) FROM judgment WHERE created_at > (SELECT strftime('%s', 'now') - 3600)) as judgments_last_hour,
    (SELECT AVG(julianday('now') - julianday(created_at, 'unixepoch')) FROM participants) as avg_participant_age_days,
    
    shm.health_check_timestamp
FROM system_health_monitoring shm;
```

-- View for identifying system bottlenecks and issues
-- This view highlights potential problems in the system
```sql
CREATE VIEW system_bottleneck_analysis AS
SELECT
    spm.total_participants,
    spm.total_events,
    spm.events_per_participant,
    spm.impacts_per_event,
    spm.judgments_per_event,
    
    -- Identify low activity
    CASE
        WHEN spm.events_last_24h = 0 THEN 'CRITICAL: No activity in last 24 hours'
        WHEN spm.events_last_24h < (SELECT AVG(events_last_24h) FROM (SELECT COUNT(*) as events_last_24h FROM truth_event WHERE created_at > (SELECT strftime('%s', 'now') - 86400) GROUP BY date(created_at, 'unixepoch', 'localtime'))) * 0.1 
        THEN 'WARNING: Very low activity'
        ELSE 'NORMAL: Activity within expected range'
    END as activity_status,
    
    -- Identify data quality issues
    CASE
        WHEN spm.data_completeness_percentage < 10 THEN 'CRITICAL: Poor data completeness'
        WHEN spm.data_completeness_percentage < 30 THEN 'WARNING: Suboptimal data completeness'
        WHEN spm.data_completeness_percentage < 70 THEN 'FAIR: Acceptable data completeness'
        ELSE 'GOOD: Excellent data completeness'
    END as data_quality_status,
    
    -- Identify reputation issues
    CASE
        WHEN spm.average_participant_reputation < 0.3 THEN 'CRITICAL: Very low average reputation'
        WHEN spm.average_participant_reputation < 0.5 THEN 'WARNING: Low average reputation'
        WHEN spm.average_participant_reputation < 0.7 THEN 'FAIR: Moderate average reputation'
        ELSE 'GOOD: High average reputation'
    END as reputation_status,
    
    -- Identify scoring issues
    CASE
        WHEN spm.events_with_complete_scores = 0 THEN 'CRITICAL: No events with complete scores'
        WHEN spm.events_with_complete_scores * 1.0 / spm.total_events < 0.1 THEN 'CRITICAL: Very few events with complete scores'
        WHEN spm.events_with_complete_scores * 1.0 / spm.total_events < 0.3 THEN 'WARNING: Low percentage of complete scores'
        ELSE 'NORMAL: Adequate scoring coverage'
    END as scoring_status,
    
    -- Calculate system health score (0-100)
    CASE
        WHEN spm.data_completeness_percentage < 10 OR spm.average_participant_reputation < 0.3 OR spm.events_last_24h = 0
        THEN 10  -- Critical
        WHEN spm.data_completeness_percentage < 30 OR spm.average_participant_reputation < 0.5 OR spm.events_with_complete_scores * 1.0 / spm.total_events < 0.1
        THEN 30  -- Warning
        WHEN spm.data_completeness_percentage < 70 OR spm.average_participant_reputation < 0.7
        THEN 60  -- Fair
        ELSE 90  -- Good
    END as overall_system_health_score,
    
    -- Recommendations
    CASE
        WHEN spm.events_last_24h = 0 THEN 'Increase participant engagement'
        WHEN spm.data_completeness_percentage < 30 THEN 'Improve data collection processes'
        WHEN spm.average_participant_reputation < 0.5 THEN 'Implement reputation building mechanisms'
        ELSE 'Continue current operations'
    END as recommended_action
FROM system_performance_metrics spm;
```

-- View for system trend analysis
-- This view tracks system health over time
```sql
CREATE VIEW system_trend_analysis AS
WITH daily_stats AS (
    SELECT 
        date(created_at, 'unixepoch', 'localtime') as day,
        COUNT(*) as events_created
    FROM truth_event
    WHERE created_at > (SELECT strftime('%s', 'now') - 86400*30)  -- Last 30 days
    GROUP BY date(created_at, 'unixepoch', 'localtime')
),
weekly_averages AS (
    SELECT 
        date(day, '-6 days') as week_start,
        AVG(events_created) as avg_daily_events,
        MIN(events_created) as min_daily_events,
        MAX(events_created) as max_daily_events,
        SUM(events_created) as total_weekly_events
    FROM daily_stats
    GROUP BY date(day, '-6 days')
)
SELECT
    wa.week_start,
    wa.avg_daily_events,
    wa.min_daily_events,
    wa.max_daily_events,
    wa.total_weekly_events,
    -- Trend calculation (simplified)
    CASE
        WHEN wa.avg_daily_events > (SELECT AVG(avg_daily_events) FROM weekly_averages) * 1.1 THEN 'UPWARD'
        WHEN wa.avg_daily_events < (SELECT AVG(avg_daily_events) FROM weekly_averages) * 0.9 THEN 'DOWNWARD'
        ELSE 'STABLE'
    END as activity_trend,
    -- Compare to previous period
    wa.avg_daily_events - (SELECT AVG(avg_daily_events) FROM weekly_averages WHERE week_start < wa.week_start ORDER BY week_start DESC LIMIT 1 OFFSET 0) as change_from_prev_week
FROM weekly_averages wa
ORDER BY wa.week_start DESC;