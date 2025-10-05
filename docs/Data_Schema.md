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
* **context\_id** (INTEGER, FK → context.id)
* **vector** (BOOLEAN) — event direction (true = outgoing from user, false = incoming from external subject)
* **detected** (BOOLEAN) — whether the event was identified as truth or lie
* **corrected** (BOOLEAN) — event correction indicator
* **timestamp\_start** (INTEGER) — event start time (UNIX)
* **timestamp\_end** (INTEGER) — event end time (UNIX)
* **code** (INTEGER) — event classification code (default: 1)

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
