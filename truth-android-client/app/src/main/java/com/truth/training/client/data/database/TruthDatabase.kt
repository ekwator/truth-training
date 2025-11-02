package com.truth.training.client.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.truth.training.client.data.database.daos.*
import com.truth.training.client.data.database.entities.*

/**
 * Room database for Truth Training Android v1.0.0.
 * Matches Desktop SQLite schema with embedded context fields.
 * 
 * Note: No TypeConverters needed - all date fields are stored as ISO 8601 strings directly.
 */
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
    
    companion object {
        const val DATABASE_NAME = "truth_training.db"
    }
}

