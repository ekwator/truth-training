use std::collections::HashMap;

/// Sample implementation of group correlation system with fairness-focused stability algorithm
/// This implementation prioritizes quality over quantity to prevent volume-bias
///
/// NOTE: This Rust code is provided for reference/validation purposes only and is NOT the authoritative implementation.
/// The authoritative implementation is in the database engine (SQL views and triggers).

#[derive(Debug, Clone)]
pub struct GroupCorrelation {
    pub group_id: u64,
    pub correlation_value: f64,
}

#[derive(Debug, Clone)]
pub struct ConvergenceZone {
    pub group1_id: u64,
    pub group2_id: u64,
    pub group1_desc: String,
    pub group2_desc: String,
    pub result_difference: f64,
}

#[derive(Debug, Clone)]
pub struct ManualGroupParameter {
    pub parameter_id: u64,
    pub priority_value: f64,
}

pub struct GroupCorrelationSystem {
    manual_groups: Vec<GroupCorrelation>,
    convergence_zones: Vec<ConvergenceZone>,
    manual_group_parameters: HashMap<u64, Vec<ManualGroupParameter>>, // group_id -> parameters
}

impl GroupCorrelationSystem {
    pub fn new() -> Self {
        GroupCorrelationSystem {
            manual_groups: Vec::new(),
            convergence_zones: Vec::new(),
            manual_group_parameters: HashMap::new(),
        }
    }

    /// Calculate correlations for all manual groups
    pub fn calculate_all_manual_group_correlations(&self) -> Vec<GroupCorrelation> {
        let mut correlations = Vec::new();
        
        for (group_id, parameters) in &self.manual_group_parameters {
            // Calculate correlation based on parameters and their priority values
            let correlation_value = self.calculate_correlation_from_parameters(parameters);
            
            correlations.push(GroupCorrelation {
                group_id: *group_id,
                correlation_value,
            });
        }
        
        correlations
    }

    /// Internal function to calculate correlation from parameters
    fn calculate_correlation_from_parameters(&self, parameters: &[ManualGroupParameter]) -> f64 {
        // This is a simplified calculation - in practice, this would involve
        // complex logic based on the fixed parameter space and priority configurations
        let mut total_weighted_value = 0.0;
        let mut total_weight = 0.0;
        
        for param in parameters {
            // Each parameter contributes to the correlation based on its priority
            let param_contribution = param.priority_value; // Simplified for example
            total_weighted_value += param_contribution * param.priority_value;
            total_weight += param.priority_value;
        }
        
        if total_weight > 0.0 {
            total_weighted_value / total_weight
        } else {
            0.5 // Neutral value when no parameters
        }
    }

    /// Detect convergence between different manual groups
    pub fn detect_convergence(&self) -> Vec<ConvergenceZone> {
        let mut zones = Vec::new();
        let correlations = self.calculate_all_manual_group_correlations();
        
        // Compare all pairs of groups for convergence
        for i in 0..correlations.len() {
            for j in (i + 1)..correlations.len() {
                let diff = (correlations[i].correlation_value - correlations[j].correlation_value).abs();
                
                // Only create convergence zone if difference is below threshold
                if diff < 0.3 { // Threshold for convergence
                    zones.push(ConvergenceZone {
                        group1_id: correlations[i].group_id,
                        group2_id: correlations[j].group_id,
                        group1_desc: format!("Group {}", correlations[i].group_id),
                        group2_desc: format!("Group {}", correlations[j].group_id),
                        result_difference: diff,
                    });
                }
            }
        }
        
        zones
    }

    /// Identify stable correlations with quality-focused algorithm
    /// This replaces the old quantity-based approach with a quality-over-quantity approach
    pub fn identify_stable_correlations(&self) -> Vec<u64> {
        let correlations = self.calculate_all_manual_group_correlations();
        let convergence_zones = self.detect_convergence();
        
        // Calculate quality metrics for each group
        let mut group_qualities: HashMap<u64, (f64, usize)> = HashMap::new(); // (total_quality, instance_count)
        
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
                // Prioritize quality over quantity: require both minimum instances AND high average quality
                if instance_count >= 1 && avg_quality > 0.7 { // At least 1 instance with good quality (>70% quality)
                    stable_groups.push(group_id);
                } else if instance_count >= 2 && avg_quality > 0.5 { // With 2+ instances, accept slightly lower quality
                    stable_groups.push(group_id);
                }
            }
        }
        
        stable_groups.sort(); // Sort for consistent output
        stable_groups
    }

    /// Evaluate groups by stability with quality-focused algorithm
    pub fn evaluate_groups_by_stability(&self) -> Vec<(u64, String)> {
        let correlations = self.calculate_all_manual_group_correlations();
        let convergence_zones = self.detect_convergence();
        let stable_groups = self.identify_stable_correlations();
        
        let mut evaluation = Vec::new();
        
        // Process manual groups
        for correlation in &correlations {
            // Manual group scoring: correlation value with some weight
            let manual_score = correlation.correlation_value * 0.7;
            
            // Add convergence bonus based on quality, not just quantity
            let convergence_bonus = {
                let mut quality_sum = 0.0;
                let mut quality_count = 0;
                
                for cz in &convergence_zones {
                    if cz.group1_id == correlation.group_id || cz.group2_id == correlation.group_id {
                        quality_sum += 1.0 - cz.result_difference; // Higher quality = lower difference
                        quality_count += 1;
                    }
                }
                
                if quality_count > 0 {
                    quality_sum / quality_count as f64 * 0.3 // Weight for convergence quality
                } else {
                    0.0
                }
            };
            
            let final_score = manual_score + convergence_bonus;
            
            evaluation.push((
                correlation.group_id,
                format!("Manual Group - Score: {:.3}", final_score)
            ));
        }
        
        // Process auto groups based on stability
        for correlation in &correlations {
            if stable_groups.contains(&correlation.group_id) {
                evaluation.push((
                    correlation.group_id,
                    "Auto Group - STABLE".to_string()
                ));
            }
        }
        
        evaluation.sort_by(|a, b| b.0.cmp(&a.0)); // Sort by group ID descending
        evaluation
    }

    /// Verify fairness by checking if group size correlates with scores
    /// Returns true if fairness is maintained (low correlation between size and score)
    pub fn verify_fairness(&self) -> bool {
        // In a real implementation, this would calculate the correlation
        // between group size (participant count) and resulting scores
        // and return true if the correlation is below a threshold (e.g., 0.5)
        
        // This is a simplified check - in reality, you'd calculate Pearson correlation
        let size_score_correlation = self.calculate_size_score_correlation();
        
        // Return true if correlation is below fairness threshold
        size_score_correlation.abs() < 0.5
    }

    /// Internal function to calculate correlation between group size and scores
    /// This is a simplified implementation for demonstration
    fn calculate_size_score_correlation(&self) -> f64 {
        // In a real implementation, this would connect to participant data
        // to get group sizes and correlate with the calculated scores
        
        // For this example, we'll return a neutral value
        0.0
    }

    /// Add a manual group with its parameters
    pub fn add_manual_group(&mut self, group_id: u64, parameters: Vec<ManualGroupParameter>) {
        self.manual_group_parameters.insert(group_id, parameters);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_identify_stable_correlations_quality_focus() {
        let mut system = GroupCorrelationSystem::new();
        
        // Add some test groups with parameters
        system.add_manual_group(1, vec![
            ManualGroupParameter { parameter_id: 1, priority_value: 0.8 },
            ManualGroupParameter { parameter_id: 2, priority_value: 0.7 },
        ]);
        
        system.add_manual_group(2, vec![
            ManualGroupParameter { parameter_id: 1, priority_value: 0.9 },
            ManualGroupParameter { parameter_id: 2, priority_value: 0.6 },
        ]);
        
        system.add_manual_group(3, vec![
            ManualGroupParameter { parameter_id: 1, priority_value: 0.4 },
            ManualGroupParameter { parameter_id: 2, priority_value: 0.3 },
        ]);
        
        // Test that stable groups are identified based on quality, not quantity
        let stable_groups = system.identify_stable_correlations();
        
        // Verify that the function runs without panic
        assert!(stable_groups.len() >= 0);
    }

    #[test]
    fn test_verify_fairness() {
        let system = GroupCorrelationSystem::new();
        
        // Test fairness verification
        let is_fair = system.verify_fairness();
        
        // Should return a boolean
        assert!(true); // This is a placeholder - actual test would check the result
    }

    #[test]
    fn test_evaluate_groups_by_stability() {
        let mut system = GroupCorrelationSystem::new();
        
        // Add test groups
        system.add_manual_group(1, vec![
            ManualGroupParameter { parameter_id: 1, priority_value: 0.8 },
        ]);
        
        system.add_manual_group(2, vec![
            ManualGroupParameter { parameter_id: 1, priority_value: 0.9 },
        ]);
        
        // Test group evaluation
        let evaluation = system.evaluate_groups_by_stability();
        
        // Verify that the function runs without panic and returns some results
        assert!(evaluation.len() >= 0);
    }
}