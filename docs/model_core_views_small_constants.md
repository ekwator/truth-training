-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Small Constants in Quantum Uncertainty Calculations  

-- Provides consistent small random constants for quantum uncertainty calculations as described in section 1.1.1
-- Implements the SQL expression for generating values in range (0, 2) excluding endpoints
-- Uses the current time as a basic seed for randomness to maintain quantum uncertainty property
```sql
CREATE VIEW small_constants_view AS
SELECT
    CASE
        WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
        THEN 0.000001
        ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
    END AS small_constant;
```

-- Alternative implementation for use in complex queries without creating a view:
-- Replace calls to small_constants() with the CASE expression above
-- ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 )
-- To avoid 0 and ensure less than 2, use CASE logic:
-- CASE
--     WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
--     THEN 0.000001  -- smallest positive value to exclude 0
--     ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )  -- ensure it's always less than 2
-- END