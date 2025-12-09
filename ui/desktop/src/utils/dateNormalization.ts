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

/**
 * Validates date range (End >= Start after normalization).
 * Matches Android validation exactly.
 * 
 * @param startTimestamp - Start timestamp (Unix timestamp in seconds or milliseconds)
 * @param endTimestamp - End timestamp (Unix timestamp in seconds or milliseconds, can be null)
 * @returns Validation result with error message if invalid
 * 
 * @example
 * ```typescript
 * const result = validateDateRange(startTimestamp, endTimestamp);
 * if (!result.valid) {
 *   // Display error: result.error
 * }
 * ```
 */
export function validateDateRange(
  startTimestamp: number,
  endTimestamp: number | null
): { valid: boolean; error?: string } {
  // Normalize timestamps (handle both seconds and milliseconds)
  const startMs = startTimestamp < 1e12 ? startTimestamp * 1000 : startTimestamp;
  const endMs = endTimestamp !== null 
    ? (endTimestamp < 1e12 ? endTimestamp * 1000 : endTimestamp)
    : null;

  const normalizedStart = normalizeToStartOfDay(new Date(startMs));
  const normalizedEnd = endMs !== null 
    ? normalizeToStartOfDay(new Date(endMs))
    : null;

  if (normalizedEnd !== null && normalizedEnd < normalizedStart) {
    return {
      valid: false,
      error: 'End Timestamp cannot be less than Start Timestamp',
    };
  }

  return { valid: true };
}

