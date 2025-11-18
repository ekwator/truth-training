# Final Data Schema for the “Truth Training” Platform

---

## 1. **knowledge\_base** Block

### **Table: category**

* **id** (INTEGER, PK)
* **name** (TEXT) — category name (e.g., “Social”, “Financial”)
* **description** (TEXT) — category description

### **Table: cause**

* **id** (INTEGER, PK)
* **name** (TEXT) — cause (e.g., “Fear”, “Benefit”, “Mercy”)
* **quality** (BOOLEAN) — logical evaluation (true = positive, false = negative)
* **description** (TEXT) — cause description

### **Table: develop**

* **id** (INTEGER, PK)
* **name** (TEXT) — manifestation (e.g., “Concealment”, “Manipulation”)
* **quality** (BOOLEAN) — logical evaluation (true = positive, false = negative)
* **description** (TEXT) — manifestation description

### **Table: effect**

* **id** (INTEGER, PK)
* **name** (TEXT) — consequence (e.g., “Distrust”, “Disappointment”)
* **quality** (BOOLEAN) — logical evaluation (true = positive, false = negative)
* **description** (TEXT) — consequence description

### **Table: forma**

* **id** (INTEGER, PK)
* **name** (TEXT) — form of logic (e.g., “Deception”, “Truth”, “Self-deception”)
* **quality** (BOOLEAN) — logical evaluation (true = positive, false = negative)
* **description** (TEXT) — form description

### **Table: context**

* **id** (INTEGER, PK)
* **name** (TEXT) — context (e.g., “Interpersonal Relationships”, “Politics”)
* **category\_id** (INTEGER, FK → category.id)
* **forma\_id** (INTEGER, FK → forma.id)
* **cause\_id** (INTEGER, FK → cause.id)
* **develop\_id** (INTEGER, FK → develop.id)
* **effect\_id** (INTEGER, FK → effect.id)
* **description** (TEXT) — context description

### **Table: impact\_type**

* **id** (INTEGER, PK)
* **name** (TEXT) — type of impact (e.g., “Reputation”, “Finance”, “Emotions”)
* **description** (TEXT) — impact type description

---

## 2. **base** Block

### **Table: truth\_events**

* **id** (INTEGER, PK)
* **description** (TEXT) — event description
* **category\_id** (INTEGER, FK → category.id, nullable) — embedded context field
* **forma\_id** (INTEGER, FK → forma.id, nullable) — embedded context field
* **cause\_id** (INTEGER, FK → cause.id, nullable) — embedded context field
* **develop\_id** (INTEGER, FK → develop.id, nullable) — embedded context field
* **effect\_id** (INTEGER, FK → effect.id, nullable) — embedded context field
* **vector** (BOOLEAN) — event direction (true = outgoing from user, false = incoming from external subject)
* **detected** (BOOLEAN) — whether the event was identified as truth or lie
* **corrected** (BOOLEAN) — event correction indicator
* **timestamp\_start** (INTEGER) — event start time (UNIX)
* **timestamp\_end** (INTEGER) — event end time (UNIX)
* **code** (INTEGER) — event classification code (default: 1)
* **collective_score** (REAL, nullable) — Collective truth score (0–1), computed from independent user evaluations (impacts)

**Note (v1.0.0 Breaking Change)**: The `context_id` foreign key has been removed in favor of embedding context fields directly in events (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`). This improves query performance by eliminating JOINs. All context fields are nullable, allowing events with partial or no context information.

### **Table: impact**

* **id** (INTEGER, PK)
* **event\_id** (INTEGER, FK → truth\_events.id)
* **type\_id** (INTEGER, FK → impact\_type.id)
* **value** (BOOLEAN) — true = positive impact, false = negative
* **notes** (TEXT, NULLABLE) — comment

### **Table: progress\_metrics**

* **id** (INTEGER, PK)
* **timestamp** (INTEGER) — date and time of statistics calculation
* **total\_events** (INTEGER) — number of user events
* **total\_events\_group** (INTEGER) — number of all events
* **total\_positive\_impact** (REAL) — positive evaluation of the user’s subjective progress cost
* **total\_positive\_impact\_group** (REAL) — positive evaluation of overall dynamics
* **total\_negative\_impact** (REAL) — negative evaluation of the user’s subjective progress cost
* **total\_negative\_impact\_group** (REAL) — negative evaluation of overall dynamics
* **trend** (REAL) — individual progress dynamics
* **trend\_group** (REAL) — overall progress dynamics

`progress_metrics` is generated based on data from `truth_events` and `impact` tables.

---

**Note:** The impact weight is not stored in the `impact` table — it is calculated in `progress_metrics` based on the number of events and their outcomes.

---

## 3. Context Template System (v1.0.0)

### Template Matching and Duplicate Detection

The `context` table stores reusable context templates that can be matched to events based on embedded context fields:

- **Template Matching**: When displaying events, the system matches event embedded fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) to context templates using **non-NULL field comparison**. Only fields that have values (non-NULL) are compared; NULL values are ignored in the matching logic.

- **Duplicate Detection**: When creating a new context template, the system checks for duplicates by comparing only non-NULL fields. If an existing template has identical non-NULL field values, creation is rejected with a 409 Conflict error.

- **Foreign Key Validation**: All embedded context fields must reference existing records in their respective tables (`category`, `forma`, `cause`, `develop`, `effect`). Invalid references are rejected immediately with a 400 Bad Request error.

### Migration Notes

**⚠️ BREAKING CHANGE (v1.0.0)**: No automatic database migrations are executed. Manual migration required for existing databases:

1. **Schema Migration**: Remove `context_id` column from `truth_events` table and add embedded fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`).

2. **Data Migration**: For existing events with `context_id`, extract context template values and populate embedded fields:
   - Query `context` table to get template values
   - Update `truth_events` with embedded field values
   - Ensure all FK references are valid before migration

3. **Index Creation**: Add indexes on embedded FK fields for query performance:
   ```sql
   CREATE INDEX IF NOT EXISTS idx_truth_events_category ON truth_events(category_id);
   CREATE INDEX IF NOT EXISTS idx_truth_events_forma ON truth_events(forma_id);
   CREATE INDEX IF NOT EXISTS idx_truth_events_cause ON truth_events(cause_id);
   CREATE INDEX IF NOT EXISTS idx_truth_events_develop ON truth_events(develop_id);
   CREATE INDEX IF NOT EXISTS idx_truth_events_effect ON truth_events(effect_id);
   ```

4. **Validation**: After migration, verify all FK references are valid and no orphaned data exists.

---

## 4. Node Discovery (v2.0.0)

### Table: nodes

| Column      | Type     | Notes                                                                                  |
|-------------|----------|----------------------------------------------------------------------------------------|
| id          | INTEGER  | Primary key (`AUTOINCREMENT`)                                                          |
| address     | TEXT     | URL or `ip:port` of the peer                                                           |
| type        | TEXT     | `LAN`, `WIFI`, `GLOBAL`, `RELAY`, `CLIENT`                                             |
| reachable   | INTEGER  | `0/1` flag updated by health checks                                                    |
| last_seen   | INTEGER  | UNIX epoch seconds of the last successful handshake                                    |
| ttl         | INTEGER  | Time-to-live (seconds) before the record is considered stale                           |
| source      | TEXT     | Discovery source (`local_broadcast`, `wifi_scan`, `global_registry`, `manual`, `peer_sync`) |
| node_id     | TEXT     | Optional Ed25519 public key (hex)                                                      |
| created_at  | INTEGER  | Creation timestamp                                                                     |
| updated_at  | INTEGER  | Last modification timestamp                                                            |

Default TTLs (codified in `core/src/config.rs`):

| Node Type | TTL (seconds) |
|-----------|---------------|
| LAN       | 120           |
| Wi-Fi     | 300           |
| Global    | 3600          |
| Relay     | 3600          |
| Client    | 600           |

Discovery cadence defaults (also exported via `DiscoveryTimingConfig`):

- LAN broadcast interval: **30s**
- Wi-Fi scan interval: **45s**
- Global registry poll interval: **3600s**
- Cleanup interval: **60s**
- Health check timeout: **5s** (3 retries)

> **Merging Rule**: When duplicate addresses exist, prefer local (LAN/Wi-Fi) entries over Global; if both local, use the latest `last_seen`, then lexicographic address order.

Indexes:

```sql
CREATE INDEX IF NOT EXISTS idx_nodes_address ON nodes(address);
CREATE INDEX IF NOT EXISTS idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(type);
CREATE INDEX IF NOT EXISTS idx_nodes_reachable ON nodes(reachable);
```

Cleanup heuristic (executed every 60s):

```sql
DELETE FROM nodes WHERE strftime('%s','now') - last_seen > ttl;
DELETE FROM nodes WHERE reachable = 0 AND strftime('%s','now') - last_seen > (ttl / 2);
```
