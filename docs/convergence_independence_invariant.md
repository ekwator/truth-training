# CONVERGENCE INSTANCE INDEPENDENCE INVARIANT
## Critical Architecture Requirement for Group Correlation System

---

## EXECUTIVE SUMMARY

This document establishes a critical architectural invariant that MUST be maintained to preserve the fairness and anti-volume-bias properties of the group correlation system in Truth Training v1.1.0.

**CORE REQUIREMENT**: Convergence instance counts MUST represent independent analytical confirmations, NOT group mass or activity volume.

---

## THE INVARIANT

### Primary Statement:
**Convergence instance count MUST NOT be proportional to participant count.**

### Detailed Requirements:
1. **Instances represent independent analytical confirmations**, not group mass
2. **Convergence instances MUST NOT scale with participant activity or size**
3. **Instance counting MUST be based on independent parameter configurations**, not group membership
4. **Stability classifications are invalid if instances correlate with participant counts**

---

## RISK ANALYSIS

### The Danger:
The current implementation includes safeguards like:
- `WHEN AVG(...) < 0.10 AND COUNT(...) >= 2 THEN 'MODERATELY_STABLE'`
- `WHEN AVG(...) < 0.20 AND COUNT(...) >= 1 THEN 'MINIMALLY_STABLE'`

In Rust:
- `if instance_count >= 1 && avg_quality > 0.7 { ... }`
- `else if instance_count >= 2 && avg_quality > 0.5 { ... }`

These are **only acceptable** if COUNT represents the number of **independent confirmations**, not group mass.

### What Makes It Acceptable:
- Convergence instances result from different manual groups with different priority configurations
- Each instance represents an independent analytical confirmation of correlation stability
- Instance count is determined by diversity of analytical approaches, not group size

### What Would Break It:
If convergence instances ever become dependent on:
- Group size (participant count)
- Activity level within groups
- Network density of participants
- Any metric correlated with participant numbers

### Consequence of Violation:
Volume-bias returns silently, undermining the entire fairness architecture.

---

## ARCHITECTURAL GUARANTEES

### Currently Valid Properties:
✅ Convergence instances arise from different manual groups with different priority configurations
✅ Each manual group represents an independent analytical lens on the same data
✅ Instance count reflects diversity of analytical approaches, not participant numbers
✅ Automatic groups form only when different priority configurations yield similar results

### Required Maintanance:
To preserve this invariant, future modifications MUST ensure:
1. **New manual groups are created by independent analytical decisions**, not by participant cloning
2. **Priority configurations remain diverse and independent**, not correlated with group membership
3. **Convergence detection compares correlation results across different parameter spaces**, not across participant subsets
4. **Automatic group formation requires convergence across truly independent approaches**

### Authoritative Layer for Convergence Detection
The database engine (SQL views and triggers) is the **authoritative execution layer** for all convergence detection operations. Rust code MUST NOT duplicate or replace database logic. Rust examples, if kept, must be explicitly marked as non-authoritative reference or validation examples.

---

## IMPLEMENTATION SAFEGUARDS

### SQL Level Safeguards:
```sql
-- Convergence detection compares correlation results between groups with different priorities
-- NOT between groups with different participant counts
SELECT 
    gc1.group_id as group1_id,
    gc2.group_id as group2_id,
    ABS(gc1.correlation_result - gc2.correlation_result) as result_difference
FROM group_correlations gc1
CROSS JOIN group_correlations gc2
WHERE gc1.group_id < gc2.group_id  -- Compare different groups
```
### Rust Level Safeguards:
**NOTE: The following Rust code is provided for reference/validation purposes only and is NOT the authoritative implementation. The authoritative implementation is in the database engine (SQL views and triggers).**

```rust
// Convergence detection compares correlation results between groups with different priorities
// Quality metric is 1.0 - result_difference (independent of participant count)
for cz in &convergence_zones {
    let quality = 1.0 - cz.result_difference;  // Pure quality metric
    // Update quality metrics for the groups involved
}
```


---

## FUTURE DEVELOPMENT CONSTRAINTS

### Permitted Modifications:
- Adding new parameters to the fixed parameter space
- Improving correlation calculation algorithms
- Enhancing quality measurement techniques
- Adding new types of analytical confirmations

### FORBIDDEN Modifications:
- Making convergence instances dependent on participant counts
- Scaling thresholds based on group size or activity
- Creating automatic convergence instances based on participant metrics
- Correlating instance generation with group membership data

---

## TESTING REQUIREMENTS

### Mandatory Tests:
1. **Size Independence Test**: Verify that convergence instance counts do not correlate with participant counts
2. **Configuration Diversity Test**: Ensure different priority configurations remain truly independent
3. **Quality vs Quantity Test**: Confirm that quality metrics take precedence over instance counts
4. **Fairness Verification Test**: Maintain the existing fairness verification mechanisms

---

## COMPLIANCE VERIFICATION

### Regular Audits Required:
- Monitor correlation between instance counts and participant counts
- Verify that priority configurations remain diverse
- Test that small groups with high-quality approaches can achieve high rankings
- Ensure automatic group formation reflects analytical convergence, not volume

### Red Flags:
- Instance counts correlating with participant counts (r > 0.3)
- Priority configurations becoming homogeneous
- Stability scores correlating strongly with group size
- Automatic groups forming based on participant metrics

---

## DOCUMENTATION MANDATE

### This invariant MUST be referenced in:
- All future architectural documentation
- Code comments near convergence-related calculations
- System design documents
- Implementation guidelines
- Review checklists

### Developer Awareness:
Any developer working on the group correlation system MUST understand this invariant before making changes.

---

## CONCLUSION

This invariant is essential to maintaining the fairness properties of the group correlation system. It prevents the silent return of volume-bias that would undermine the entire architectural approach of using stability under parameter variation rather than majority or volume dominance.

**Violating this invariant would compromise the core fairness architecture and revert the system to volume-based bias.**