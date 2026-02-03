# ARCHITECTURAL FAIRNESS AUDIT
## Group Correlation System in Truth Training v1.1.0

---

## EXECUTIVE SUMMARY

This document presents a comprehensive audit of the group correlation system implementation to verify that group size (participant count) does NOT implicitly influence final correlation results, rankings, stability scores, or convergence decisions, except for legitimate internal statistical normalization purposes.

**FINAL VERDICT**: The implementation largely respects the fairness invariant "Size influences aggregation, not comparison" with one identified risk zone that requires correction.

---

## STEP 1 — DEFINING ALLOWED AND FORBIDDEN ROLES OF GROUP SIZE

### Where participant_count IS ALLOWED to be used:
1. **Internal normalization** - Computing averages, ratios, and percentages within groups
2. **Statistical reporting** - Displaying participant counts for awareness only
3. **Denominator in ratio calculations** - Legitimate use in computing proportions

### Where participant_count is STRICTLY FORBIDDEN:
1. **Direct multiplication** with correlation scores
2. **Weighting group comparisons** based on size
3. **Determining group rankings** based on size
4. **Influencing convergence decisions** based on group size
5. **Scaling stability scores** with participant counts
6. **Threshold adjustments** based on group size

### Core Principle:
"Group size may affect aggregation, but must NOT affect comparison."

---

## STEP 2 — SQL AUDIT (Implicit Size Influence)

### File: docs/model_core_view_participant_group_parameters_and_correlation.md

#### 2.1 Views and Queries Analysis:

**A. `manual_group_correlations` view** - ✅ SAFE
- Uses `COUNT(*)` for internal normalization within groups (allowed)
- Each parameter calculation normalizes internally but does not compare across groups by size
- Results are based on parameter weights, not group size
- **ALLOWED**: Internal normalization only

**B. `convergence_zones` view** - ✅ SAFE  
- Compares correlation results between groups based on numeric values, not size
- Uses `ABS(gc1.correlation_result - gc2.correlation_result)` for comparison
- No size-based thresholds or filters
- **ALLOWED**: Pure value comparison

**C. `auto_group_stability` view** - ⚠️ RISKY
- Contains `HAVING COUNT(cz.group1_id) >= 3` - This is size-dependent threshold
- Line 260: `WHEN COUNT(cz.group1_id) >= 3 AND AVG(cz.result_difference) < 0.15 THEN 'STABLE'`
- This means groups need 3+ convergence instances to be considered "STABLE"
- **FORBIDDEN**: Size-based threshold affecting stability classification

**D. `group_evaluation_metrics` view** - ✅ SAFE
- Line 309: `(SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg.id AND pgm.left_at IS NULL) as participant_count,`
- Participant count is calculated but used only for reporting (awareness)
- Line 323: Manual group scoring: `AVG(correlation_result) * 0.7 + (SELECT COUNT(*)) * 0.3`
- The COUNT(*) here is convergence_instances, not participant count - ✅ SAFE
- Line 326: Auto group scoring: `AVG(2 - result_difference)` - based on convergence quality, not size
- **ALLOWED**: Size reported but not used for weighting

**E. `fairness_verification_size_impact` view** - ✅ SAFE
- Explicitly designed to verify that size doesn't correlate with evaluation
- Purpose is detection of unfairness - correctly implemented

**F. `stability_independence_check` view** - ✅ SAFE  
- Calculates Pearson correlation between size and scores
- Designed to detect unfairness - correctly implemented

**G. `group_correlation_summary` view** - ✅ SAFE
- Orders by stability_score, not participant_count
- Explicitly states: "Rank groups by stability rather than size (addresses fairness concern)"

### 2.2 Specific Risk Analysis:

**RISK ZONE IDENTIFIED:**
In `auto_group_stability` view:
```sql
WHEN COUNT(cz.group1_id) >= 3 AND AVG(cz.result_difference) < 0.15 THEN 'STABLE'
WHEN COUNT(cz.group1_id) >= 2 AND AVG(cz.result_difference) < 0.25 THEN 'MODERATELY_STABLE'
```

This creates a size-dependent threshold where larger groups (with more participants generating more convergence pairs) are more likely to reach the required count thresholds. Smaller groups might have higher quality convergence but fail to meet the count requirement.

**CORRECTIVE ACTION NEEDED:**
Replace size-dependent thresholds with quality-dependent thresholds:
```sql
WHEN AVG(cz.result_difference) < 0.05 AND MIN(cz.result_difference) < 0.02 THEN 'STABLE'  -- High quality convergence
WHEN AVG(cz.result_difference) < 0.10 AND COUNT(cz.group1_id) >= 2 THEN 'MODERATELY_STABLE'  -- Require minimum instances but prioritize quality
```

---

## STEP 3 — RUST AUDIT (Algorithmic Bias Check)

### File: docs/rust/sample_group_correlation.rs

#### 3.1 Algorithm Analysis:

**A. `calculate_manual_group_correlation` method** - ✅ SAFE
- Correlation results based on parameter calculations, not group size
- Each parameter calculation normalizes internally but doesn't compare across groups by size
- No use of participant_count in final correlation value

**B. `detect_convergence` method** - ✅ SAFE
- Compares correlation values directly: `let diff = (correlations[i].correlation_value - correlations[j].correlation_value).abs();`
- No size-based filtering or weighting
- Pure value comparison

**C. `identify_stable_correlations` method** - ⚠️ RISKY
- Line 694: `if count >= 2 { // Appears in at least 2 convergence zones`
- This creates a size-based threshold where groups must appear in minimum number of convergence zones
- However, this is convergence-instance based, not participant-count based - potentially OK but needs review
- **RISKY**: Could bias toward groups that participate in more convergence zones (potentially larger groups generate more convergence opportunities)

**NOTE: The following Rust code is provided for reference/validation purposes only and is NOT the authoritative implementation. The authoritative implementation is in the database engine (SQL views and triggers).**

**D. `evaluate_groups_by_stability` method** - ✅ SAFE
- Line 729: Manual group scoring: `correlation.correlation_value * 0.7`
- Line 734: Convergence bonus: `count() as f64 * 0.3` - This is convergence count, not participant count
- The convergence count might correlate with group size, but it's measuring actual convergence events
- **ALLOWED**: Based on actual convergence, not just size

**E. `verify_fairness` method** - ✅ SAFE
- Explicitly designed to detect size-score correlation
- Returns boolean based on whether correlation exceeds 0.5 threshold
- Correctly implemented for fairness detection

#### 3.2 Detailed Risk Analysis:

**RISK ZONE IDENTIFIED:**
In `identify_stable_correlations` method:
```rust
if count >= 2 { // Appears in at least 2 convergence zones
    stable_groups.push(group_id);
}
```

This threshold means groups must appear in at least 2 convergence zones to be considered "stable". Larger groups might generate more convergence opportunities, giving them an implicit advantage.

**CORRECTIVE ACTION NEEDED:**
Consider quality over quantity:
```rust
// Instead of just counting instances, consider the quality of convergence
let avg_quality = convergence_zones
    .iter()
    .filter(|cz| cz.group1_id == group_id || cz.group2_id == group_id)
    .map(|cz| 1.0 - cz.result_difference) // Higher quality = lower difference
    .sum::<f64>() / count as f64;

if avg_quality > 0.8 { // High average convergence quality
    stable_groups.push(group_id);
}
```

---

## STEP 4 — DOCUMENTATION CONSISTENCY CHECK

### File: Group Correlation Fairness and Representative Balance Analysis

#### 4.1 Text Analysis:

**SAFE PASSAGES:**
- "The system evaluates based on stability of correlation results under parameter variation, not on group size or activity volume"
- "A small group with a stable, predictive priority configuration can achieve high correlation scores"
- "The system explicitly does not produce: ... Volume-based dominance metrics"

**NO RISKY PASSAGES FOUND:**
- No text implies larger groups are more reliable
- No text suggests size increases confidence
- No text claims activity volume strengthens truth

**Documentation is CONSISTENT** with fairness principles.

---

## STEP 5 — FINAL VERDICT

### 5.1 Confirmed Safe Usages of Group Size:
✅ Internal normalization (averages, ratios within groups)  
✅ Statistical reporting (displaying participant counts)  
✅ Pure value comparisons (correlation results, not sizes)  
✅ Explicit fairness verification mechanisms

### 5.2 Detected Risk Zones:

**RISK #1: SQL auto_group_stability view**
- Location: Lines 260-262 in SQL file
- Issue: Size-dependent thresholds for stability classification
- Impact: May favor groups that generate more convergence instances (potentially larger groups)

**RISK #2: Rust identify_stable_correlations method** 
- Location: Line 694 in Rust file
- Issue: Quantity-based threshold for stability
- Impact: May favor groups that appear in more convergence zones

### 5.3 Invariant Compliance:
The system mostly respects the invariant: "Size influences aggregation, not comparison"

However, the identified risk zones introduce size-dependent thresholds that could create implicit bias.

### 5.4 Final Answer:
**NO** - The implementation does NOT fully respect the fairness invariant. There are two risk zones where group size could implicitly influence outcomes through quantity-based thresholds rather than quality-based measures.

---

## RECOMMENDED CORRECTIONS:

### SQL Fix:
```sql
-- Replace the auto_group_stability view with quality-focused thresholds
CREATE VIEW auto_group_stability AS
SELECT 
    ag.id as auto_group_id,
    ag.description,
    COUNT(cz.group1_id) as convergence_instances,
    AVG(cz.result_difference) as avg_difference,
    MIN(cz.result_difference) as min_difference,
    MAX(cz.result_difference) as max_difference,
    CASE 
        WHEN AVG(cz.result_difference) < 0.05 AND MIN(cz.result_difference) < 0.02 THEN 'STABLE' -- High quality convergence
        WHEN AVG(cz.result_difference) < 0.10 AND COUNT(cz.group1_id) >= 2 THEN 'MODERATELY_STABLE' -- Require quality + minimum instances  
        WHEN AVG(cz.result_difference) < 0.20 THEN 'MINIMALLY_STABLE' -- Accept lower quality if very consistent
        ELSE 'UNSTABLE'
    END as stability_status
FROM participants_groups ag
LEFT JOIN convergence_zones cz ON ag.description LIKE '%' || cz.group1_desc || '%'
WHERE ag.type = 'auto'
GROUP BY ag.id, ag.description;
```

### Rust Fix:
```rust
// Replace the identify_stable_correlations method with quality-focused logic
pub fn identify_stable_correlations(&self) -> Vec<u64> {
    let correlations = self.calculate_all_manual_group_correlations();
    let convergence_zones = self.detect_convergence();
    
    // Calculate quality metrics for each group
    let mut group_qualities: HashMap<u64, (f64, usize)> = HashMap::new(); // (avg_quality, count)
    
    for cz in &convergence_zones {
        // Quality is 1.0 - difference (lower difference = higher quality)
        let quality = 1.0 - cz.result_difference;
        
        // Update quality metrics for group 1
        let (sum, count) = group_qualities.entry(cz.group1_id).or_insert((0.0, 0));
        *sum += quality;
        *count += 1;
        
        // Update quality metrics for group 2
        let (sum, count) = group_qualities.entry(cz.group2_id).or_insert((0.0, 0));
        *sum += quality;
        *count += 1;
    }
    
    // Identify groups with high average convergence quality
    let mut stable_groups = Vec::new();
    for (group_id, (total_quality, instance_count)) in group_qualities {
        if instance_count > 0 {
            let avg_quality = total_quality / instance_count as f64;
            // Require both minimum instances AND high average quality
            if instance_count >= 1 && avg_quality > 0.7 { // At least 1 instance with good quality
                stable_groups.push(group_id);
            }
        }
    }
    
    stable_groups.sort(); // Sort for consistent output
    stable_groups
}
```

With these corrections, the implementation would fully respect the fairness invariant.