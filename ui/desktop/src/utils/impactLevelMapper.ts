/**
 * ImpactLevelMapper utility
 * Maps impact levels (1-5) to boolean values and provides helper functions
 * Matching Android implementation pattern
 */

/**
 * Maps impact level (1-5) to boolean value
 * Levels 1-3 → false (negative impact)
 * Levels 4-5 → true (positive impact)
 * 
 * @param impactLevel - Impact level in range 1-5
 * @returns Boolean value: true for positive (4-5), false for negative (1-3)
 * @throws Error if impact level is out of range
 */
export function mapToBoolean(impactLevel: number): boolean {
  if (impactLevel < 1 || impactLevel > 5) {
    throw new Error(`Impact level must be in range 1-5, got: ${impactLevel}`);
  }
  return impactLevel > 3;
}

/**
 * Maps boolean value to impact level range
 * 
 * @param value - Boolean value: true (positive) or false (negative)
 * @returns Range object with min and max levels
 */
export function mapToRange(value: boolean): { min: number; max: number } {
  return value ? { min: 4, max: 5 } : { min: 1, max: 3 };
}

/**
 * Gets minimum impact level for a boolean value
 * 
 * @param value - Boolean value: true (positive) or false (negative)
 * @returns Minimum level: 4 for positive, 1 for negative
 */
export function getMinLevel(value: boolean): number {
  return value ? 4 : 1;
}

/**
 * Gets maximum impact level for a boolean value
 * 
 * @param value - Boolean value: true (positive) or false (negative)
 * @returns Maximum level: 5 for positive, 3 for negative
 */
export function getMaxLevel(value: boolean): number {
  return value ? 5 : 3;
}

/**
 * Validates if impact level is in valid range
 * 
 * @param impactLevel - Impact level to validate
 * @returns True if level is in range 1-5, false otherwise
 */
export function isValid(impactLevel: number): boolean {
  return impactLevel >= 1 && impactLevel <= 5;
}

/**
 * Gets display text for impact level range based on boolean value
 * 
 * @param value - Boolean value: true (positive) or false (negative)
 * @returns Display text: "Positive (Level 4-5)" or "Negative (Level 1-3)"
 */
export function getDisplayText(value: boolean): string {
  return value ? 'Positive (Level 4-5)' : 'Negative (Level 1-3)';
}

