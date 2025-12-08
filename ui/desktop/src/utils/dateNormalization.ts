/**
 * Date normalization utility.
 * Matches Android normalizeToStartOfDay algorithm.
 * Normalizes timestamps to start of day (00:00:00) for accurate comparison.
 */

/**
 * Normalizes a date to the start of day (00:00:00.000).
 * This ensures accurate date comparisons regardless of time component.
 * Handles timezone and DST edge cases by normalizing to local time start of day.
 * 
 * @param date - Date to normalize
 * @returns New Date object normalized to start of day
 * 
 * @example
 * ```typescript
 * const normalizedStart = normalizeToStartOfDay(startDate);
 * const normalizedEnd = endDate ? normalizeToStartOfDay(endDate) : null;
 * if (normalizedEnd && normalizedEnd < normalizedStart) {
 *   // Validation error: End cannot be less than Start
 * }
 * ```
 */
export function normalizeToStartOfDay(date: Date): Date {
  const normalized = new Date(date);
  normalized.setHours(0, 0, 0, 0);
  return normalized;
}

/**
 * Normalizes a timestamp (Unix timestamp in seconds) to start of day.
 * 
 * @param timestamp - Unix timestamp in seconds
 * @returns Unix timestamp in seconds for start of day
 */
export function normalizeTimestampToStartOfDay(timestamp: number): number {
  const date = new Date(timestamp * 1000); // Convert seconds to milliseconds
  const normalized = normalizeToStartOfDay(date);
  return Math.floor(normalized.getTime() / 1000); // Convert back to seconds
}

