package com.truth.training.client.utils

/**
 * Utility for mapping impact levels (1-5) to boolean values.
 * Matches Desktop UI implementation pattern.
 * 
 * Mapping logic:
 * - Levels 1-3 → false (negative impact)
 * - Levels 4-5 → true (positive impact)
 */
object ImpactLevelMapper {
    
    /**
     * Maps impact level (1-5) to boolean value.
     * 
     * @param impactLevel Impact level in range 1-5
     * @return true for positive impact (4-5), false for negative impact (1-3)
     * @throws IllegalArgumentException if impactLevel is not in valid range
     */
    fun mapToBoolean(impactLevel: Int): Boolean {
        require(impactLevel in 1..5) { "Impact level must be in range 1-5, got: $impactLevel" }
        return impactLevel > 3
    }
    
    /**
     * Maps boolean value to impact level range.
     * 
     * @param value Boolean value (true = positive, false = negative)
     * @return Range of valid impact levels: true → 4-5, false → 1-3
     */
    fun mapToRange(value: Boolean): IntRange {
        return if (value) 4..5 else 1..3
    }
    
    /**
     * Gets the minimum impact level for a boolean value.
     * 
     * @param value Boolean value (true = positive, false = negative)
     * @return Minimum impact level: true → 4, false → 1
     */
    fun getMinLevel(value: Boolean): Int {
        return if (value) 4 else 1
    }
    
    /**
     * Gets the maximum impact level for a boolean value.
     * 
     * @param value Boolean value (true = positive, false = negative)
     * @return Maximum impact level: true → 5, false → 3
     */
    fun getMaxLevel(value: Boolean): Int {
        return if (value) 5 else 3
    }
    
    /**
     * Validates that impact level is in valid range.
     * 
     * @param impactLevel Impact level to validate
     * @return true if impact level is in range 1-5, false otherwise
     */
    fun isValid(impactLevel: Int): Boolean {
        return impactLevel in 1..5
    }
}

