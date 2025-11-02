# Data Model: Android Client v1.0.0

**Feature**: Align Truth Training Android Client with Desktop v1.0.0 Features  
**Branch**: 007-title-align-truth  
**Date**: 2025-11-02

---

## Overview

This document defines the Room database schema for Android, matching Desktop v1.0.0 SQLite schema with embedded context fields. All entities must support offline-first operations with background synchronization.

---

## Database Schema

### Room Database Configuration

```kotlin
@Database(
    entities = [
        EventEntity::class,
        ContextTemplateEntity::class,
        JudgmentEntity::class,
        ImpactEntity::class,
        SummaryEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TruthDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun contextTemplateDao(): ContextTemplateDao
    abstract fun judgmentDao(): JudgmentDao
    abstract fun impactDao(): ImpactDao
    abstract fun summaryDao(): SummaryDao
    abstract fun syncQueueDao(): SyncQueueDao
}
```

**Database Name**: `truth_training.db`  
**Location**: `app/data/databases/truth_training.db`  
**Journal Mode**: WAL (Write-Ahead Logging) for concurrent access

---

## Entities

### EventEntity

Represents a training event with embedded context fields (v1.0.0).

```kotlin
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = ContextTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        // Similar foreign keys for forma_id, cause_id, develop_id, effect_id
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["category_id", "forma_id", "cause_id", "develop_id", "effect_id"])
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String,  // Format: "event_{uuid}"
    
    @ColumnInfo(name = "title")
    val title: String,  // Required, max 200 chars
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    // Embedded context fields (v1.0.0 - replaces context_id)
    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "forma_id")
    val formaId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "cause_id")
    val causeId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "develop_id")
    val developId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "effect_id")
    val effectId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "start_date")
    val startDate: String? = null,  // ISO 8601 format
    
    @ColumnInfo(name = "end_date")
    val endDate: String? = null,  // ISO 8601 format
    
    @ColumnInfo(name = "created_at")
    val createdAt: String,  // ISO 8601 format, required
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,  // ISO 8601 format
    
    @ColumnInfo(name = "status")
    val status: String  // "active" | "inactive" | "archived" | "pending"
)
```

**Validation Rules**:
- `title`: Required, max 200 characters
- Date order: `startDate` must be <= `endDate` (if both provided)
- All context fields optional (nullable)
- Foreign key references must exist in knowledge base (validated at API level)

---

### ContextTemplateEntity

Represents a reusable context template for event creation.

```kotlin
@Entity(
    tableName = "context_templates",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["category_id", "forma_id", "cause_id", "develop_id", "effect_id"])
    ]
)
data class ContextTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    
    @ColumnInfo(name = "name")
    val name: String,  // Required, unique
    
    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,
    
    @ColumnInfo(name = "forma_id")
    val formaId: Int? = null,
    
    @ColumnInfo(name = "cause_id")
    val causeId: Int? = null,
    
    @ColumnInfo(name = "develop_id")
    val developId: Int? = null,
    
    @ColumnInfo(name = "effect_id")
    val effectId: Int? = null,
    
    @ColumnInfo(name = "description")
    val description: String? = null
)
```

**Validation Rules**:
- `name`: Required, unique
- Duplicate detection: Templates with identical non-NULL fields cannot be created (409 Conflict)
- All context fields optional (nullable)
- Foreign key references must exist in knowledge base (validated at API level)

**Duplicate Detection Logic**:
- Compare non-NULL fields only (NULL values ignored)
- If all non-NULL fields match existing template → 409 Conflict

---

### JudgmentEntity

Represents a user's assessment of an event.

```kotlin
@Entity(
    tableName = "judgments",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["submitted_at"])
    ]
)
data class JudgmentEntity(
    @PrimaryKey
    val id: String,  // Format: "judg_{uuid}"
    
    @ColumnInfo(name = "event_id")
    val eventId: String,  // FK to events
    
    @ColumnInfo(name = "assessment")
    val assessment: String,  // "true" | "false" | "uncertain"
    
    @ColumnInfo(name = "confidence_level")
    val confidenceLevel: Double,  // Range: 0.0-1.0
    
    @ColumnInfo(name = "reasoning")
    val reasoning: String? = null,
    
    @ColumnInfo(name = "submitted_at")
    val submittedAt: String  // ISO 8601 format, required
)
```

**Validation Rules**:
- `assessment`: Must be "true", "false", or "uncertain"
- `confidenceLevel`: Must be between 0.0 and 1.0 (inclusive)
- `eventId`: Must reference existing event

---

### ImpactEntity

Represents an impact assessment for an event.

```kotlin
@Entity(
    tableName = "impacts",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"])]
)
data class ImpactEntity(
    @PrimaryKey
    val id: String,  // Format: "impact_{uuid}"
    
    @ColumnInfo(name = "event_id")
    val eventId: String,  // FK to events
    
    @ColumnInfo(name = "impact_level")
    val impactLevel: Int,  // Range: 1-5
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String  // ISO 8601 format, required
)
```

**Validation Rules**:
- `impactLevel`: Must be integer between 1 and 5 (inclusive)
- `eventId`: Must reference existing event

---

### SummaryEntity

Represents a summary and recommendations for an event (1:1 relationship with events).

```kotlin
@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"], unique = true)]
)
data class SummaryEntity(
    @PrimaryKey
    val id: String,  // Format: "summ_{uuid}"
    
    @ColumnInfo(name = "event_id")
    val eventId: String,  // FK to events, unique
    
    @ColumnInfo(name = "summary_text")
    val summaryText: String? = null,
    
    @ColumnInfo(name = "recommendations")
    val recommendations: String? = null,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String  // ISO 8601 format, required
)
```

**Validation Rules**:
- `eventId`: Must be unique (1:1 relationship with events)

---

### SyncQueueEntity

Represents pending synchronization operations for offline-first architecture.

```kotlin
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["operation_type"]),
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    
    @ColumnInfo(name = "operation_type")
    val operationType: String,  // "CREATE" | "UPDATE" | "DELETE"
    
    @ColumnInfo(name = "entity_type")
    val entityType: String,  // "EVENT" | "CONTEXT_TEMPLATE" | "JUDGMENT" | "IMPACT" | "SUMMARY"
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,  // ID of the entity
    
    @ColumnInfo(name = "payload")
    val payload: String,  // JSON serialized entity data
    
    @ColumnInfo(name = "status")
    val status: String,  // "PENDING" | "SYNCING" | "COMPLETED" | "FAILED"
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,  // Default: 0, max: 3
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String,  // ISO 8601 format
    
    @ColumnInfo(name = "synced_at")
    val syncedAt: String? = null  // ISO 8601 format, set when sync completes
)
```

**Sync Strategy**:
- Local-wins: Local changes take precedence over remote changes
- Background sync: Processed via WorkManager when online
- Retry logic: Max 3 retries per operation, exponential backoff
- Conflict resolution: Local timestamp > remote timestamp = keep local

---

## Data Access Objects (DAOs)

### EventDao

```kotlin
@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun listEvents(limit: Int, offset: Int): List<EventEntity>
    
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Update
    suspend fun updateEvent(event: EventEntity)
    
    @Delete
    suspend fun deleteEvent(event: EventEntity)
    
    @Query("SELECT COUNT(*) FROM events")
    suspend fun getEventCount(): Int
    
    @Query("SELECT * FROM events WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun listEventsByStatus(status: String, limit: Int, offset: Int): List<EventEntity>
}
```

### ContextTemplateDao

```kotlin
@Dao
interface ContextTemplateDao {
    @Query("SELECT * FROM context_templates ORDER BY name ASC")
    suspend fun listTemplates(): List<ContextTemplateEntity>
    
    @Query("SELECT * FROM context_templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): ContextTemplateEntity?
    
    @Query("SELECT * FROM context_templates WHERE name = :name")
    suspend fun getTemplateByName(name: String): ContextTemplateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ContextTemplateEntity): Long
    
    @Update
    suspend fun updateTemplate(template: ContextTemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: ContextTemplateEntity)
    
    // Template matching: find template with matching non-NULL fields
    @Query("""
        SELECT * FROM context_templates 
        WHERE (:categoryId IS NULL OR category_id = :categoryId)
          AND (:formaId IS NULL OR forma_id = :formaId)
          AND (:causeId IS NULL OR cause_id = :causeId)
          AND (:developId IS NULL OR develop_id = :developId)
          AND (:effectId IS NULL OR effect_id = :effectId)
        LIMIT 1
    """)
    suspend fun matchTemplate(
        categoryId: Int?,
        formaId: Int?,
        causeId: Int?,
        developId: Int?,
        effectId: Int?
    ): ContextTemplateEntity?
    
    // Duplicate detection: check if template with identical non-NULL fields exists
    @Query("""
        SELECT COUNT(*) FROM context_templates 
        WHERE (:categoryId IS NULL OR category_id = :categoryId)
          AND (:formaId IS NULL OR forma_id = :formaId)
          AND (:causeId IS NULL OR cause_id = :causeId)
          AND (:developId IS NULL OR develop_id = :developId)
          AND (:effectId IS NULL OR effect_id = :effectId)
          AND id != :excludeId
    """)
    suspend fun countDuplicateTemplates(
        categoryId: Int?,
        formaId: Int?,
        causeId: Int?,
        developId: Int?,
        effectId: Int?,
        excludeId: Int?
    ): Int
}
```

### JudgmentDao

```kotlin
@Dao
interface JudgmentDao {
    @Query("SELECT * FROM judgments WHERE event_id = :eventId ORDER BY submitted_at DESC LIMIT :limit OFFSET :offset")
    suspend fun listJudgmentsForEvent(eventId: String, limit: Int, offset: Int): List<JudgmentEntity>
    
    @Query("SELECT COUNT(*) FROM judgments WHERE event_id = :eventId")
    suspend fun getJudgmentCountForEvent(eventId: String): Int
    
    @Query("SELECT * FROM judgments WHERE id = :id")
    suspend fun getJudgmentById(id: String): JudgmentEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJudgment(judgment: JudgmentEntity)
    
    @Update
    suspend fun updateJudgment(judgment: JudgmentEntity)
    
    @Delete
    suspend fun deleteJudgment(judgment: JudgmentEntity)
    
    // Judgment statistics for consensus calculation
    @Query("SELECT COUNT(*) FROM judgments WHERE event_id = :eventId AND assessment = :assessment")
    suspend fun countJudgmentsByAssessment(eventId: String, assessment: String): Int
    
    @Query("SELECT AVG(confidence_level) FROM judgments WHERE event_id = :eventId")
    suspend fun getAverageConfidence(eventId: String): Double?
}
```

### ImpactDao, SummaryDao, SyncQueueDao

Similar patterns for other entities (see full implementation in tasks.md).

---

## Data Synchronization

### Sync Strategy

**Local-Wins Conflict Resolution**:
1. Local changes saved immediately to Room database
2. Sync queue entry created for background processing
3. When online, WorkManager processes queue
4. Conflict resolution: Local timestamp > remote timestamp = keep local
5. Remote changes merged if local timestamp < remote timestamp

**Sync Queue Processing**:
1. WorkManager triggers periodic sync (every 15 minutes when online)
2. Process all PENDING operations in order
3. For each operation:
   - Mark status = SYNCING
   - Send API request
   - On success: Mark COMPLETED, delete from queue
   - On failure: Increment retry_count, mark FAILED if retry_count >= 3

---

## Migration from SharedPreferences

**Current State** (v0.3.0):
- SharedPreferences: Stores JWT tokens only

**Target State** (v1.0.0):
- Room Database: Full data model (events, templates, judgments, etc.)
- SharedPreferences: Optionally keep for tokens (or migrate to Room)

**Migration Strategy**:
1. Create Room database with schema version 1
2. Migrate token storage (optional: keep SharedPreferences for tokens)
3. Initialize empty database on first launch
4. Sync from server on first connection

---

## Relationship Diagram

```
EventEntity (1) ──< (N) ImpactEntity
EventEntity (1) ──< (N) JudgmentEntity
EventEntity (1) ──< (1) SummaryEntity
EventEntity (N) >── (1) ContextTemplateEntity (via embedded fields)

SyncQueueEntity (tracks all entity sync operations)
```

---

## Performance Considerations

- **Pagination**: All list queries use LIMIT/OFFSET (default: 35 items per page)
- **Indices**: Foreign keys, status, timestamps indexed for fast queries
- **WAL Mode**: Write-Ahead Logging enables concurrent reads/writes
- **Background Sync**: WorkManager processes queue without blocking UI

---

## Validation Summary

| Entity | Required Fields | Validation Rules |
|--------|----------------|-------------------|
| Event | `id`, `title`, `created_at`, `status` | Title max 200 chars, date order validation |
| ContextTemplate | `id`, `name` | Name unique, duplicate detection |
| Judgment | `id`, `event_id`, `assessment`, `confidence_level`, `submitted_at` | Assessment enum, confidence 0-1 |
| Impact | `id`, `event_id`, `impact_level`, `created_at` | Impact level 1-5 |
| Summary | `id`, `event_id`, `updated_at` | Event_id unique (1:1) |

---

## Next Steps

- **Phase 2**: Implement Room entities, DAOs, database initialization
- **Phase 2**: Implement sync queue processing
- **Phase 2**: Implement API client integration with Room

