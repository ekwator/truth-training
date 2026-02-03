# Small Constants View for Quantum Uncertainty

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view provides consistent small random constants for quantum uncertainty calculations as described in section 1.1.1, implementing the SQL expression for generating values in range (0, 2) excluding endpoints.

## Purpose
The `small_constants_view` is designed to maintain the quantum uncertainty property essential to the model by generating time-based pseudo-random values that can be used in SQL triggers and stored procedures across desktop and mobile platforms.

## SQL Implementation

```sql
-- View to provide consistent small random constants for quantum uncertainty calculations
CREATE VIEW small_constants_view AS
SELECT 
    CASE
        WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
        THEN 0.000001  -- smallest positive value to exclude 0
        ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )  -- ensure it's always less than 2
    END AS small_constant_value,
    CURRENT_TIMESTAMP AS calculated_at;

-- Alternative implementation for use in triggers and complex queries
-- This implementation uses SQLite built-in functions to generate a random value between 0 and 2, excluding both endpoints
-- Using the current time as a basic seed for randomness
-- The expression combines Unix epoch time with fractional seconds to create a pseudo-random value
-- Add a small epsilon to avoid 0, and ensure it's always less than 2

-- For use in triggers and queries, we define the following expression:
-- ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 )

-- To avoid 0 and ensure less than 2, we use CASE logic:
/*
CASE
    WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
    THEN 0.000001  -- smallest positive value to exclude 0
    ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )  -- ensure it's always less than 2
END
*/

-- This implementation replaces the Rust function to maintain the same quantum uncertainty behavior while enabling use in SQL triggers and stored procedures across desktop and mobile platforms.
-- The implementation uses SQLite's built-in time functions to generate a time-based pseudo-random value in the range (0, 2), maintaining the quantum uncertainty property essential to the model.
*/
```

## Usage in Triggers and Procedures
This view can be used in triggers to generate small random constants that maintain the quantum uncertainty behavior:

```sql
-- Example usage in a trigger
CREATE TRIGGER example_trigger
AFTER INSERT ON truth_event
BEGIN
    UPDATE event_state_history
    SET impact_score = impact_score + (SELECT small_constant_value FROM small_constants_view LIMIT 1)
    WHERE event_id = NEW.id;
END;
```

## Relationship to Model Core
This view implements the quantum uncertainty concept described in the model core documentation, where "small_constants" is a global small random in system time function value (0, 2). The view ensures that all nodes in the distributed system have access to consistent random values that are time-based and maintain the quantum uncertainty property.

## Notes
- This implementation replaces the Rust function to maintain the same quantum uncertainty behavior while enabling use in SQL triggers and stored procedures across desktop and mobile platforms.
- The implementation uses SQLite's built-in time functions to generate a time-based pseudo-random value in the range (0, 2), maintaining the quantum uncertainty property essential to the model.