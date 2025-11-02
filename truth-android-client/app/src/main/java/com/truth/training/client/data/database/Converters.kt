package com.truth.training.client.data.database

/**
 * Type converters for Room database.
 * Currently, all date fields are stored as ISO 8601 strings directly,
 * so no conversion is needed. This class is kept for future use if we
 * decide to store dates as Long (timestamp) or Instant.
 */
class Converters {
    // No converters needed: dates are stored as ISO 8601 strings
    // If we need to convert between String and Instant/Long in the future,
    // converters will be added here.
}

