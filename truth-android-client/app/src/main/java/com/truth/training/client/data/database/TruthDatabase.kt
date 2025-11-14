package com.truth.training.client.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.truth.training.client.data.database.daos.*
import com.truth.training.client.data.database.entities.*
import com.truth.training.client.data.database.TruthDatabaseMigrations

/**
 * Room database for Truth Training Android v1.0.0.
 * Matches Desktop SQLite schema with embedded context fields.
 * 
 * Full schema includes:
 * - Knowledge base tables: category, cause, develop, effect, forma, context (templates), impact_type
 * - Base tables: truth_events (with embedded context fields), impact, progress_metrics
 * - Legacy tables: events, impacts, summaries, judgments, logs (for backward compatibility)
 * 
 * Note: No TypeConverters needed - all date fields are stored as ISO 8601 strings directly.
 * Boolean fields (quality) are automatically converted to INTEGER (0/1) by Room.
 */
@Database(
    entities = [
        // Knowledge base entities
        CategoryEntity::class,
        CauseEntity::class,
        DevelopEntity::class,
        EffectEntity::class,
        FormaEntity::class,
        ContextTemplateEntity::class,
        ImpactTypeEntity::class,
        // Base entities
        EventEntity::class,
        ImpactEntity::class,
        ProgressMetricsEntity::class,
        // Legacy entities (for backward compatibility)
        JudgmentEntity::class,
        SummaryEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,  // Incremented to add knowledge base entities
    exportSchema = true  // Schema export enabled for migration validation
)
abstract class TruthDatabase : RoomDatabase() {
    // Knowledge base DAOs
    abstract fun categoryDao(): CategoryDao
    abstract fun causeDao(): CauseDao
    abstract fun developDao(): DevelopDao
    abstract fun effectDao(): EffectDao
    abstract fun formaDao(): FormaDao
    abstract fun contextTemplateDao(): ContextTemplateDao
    abstract fun impactTypeDao(): ImpactTypeDao
    
    // Base DAOs
    abstract fun eventDao(): EventDao
    abstract fun impactDao(): ImpactDao
    abstract fun progressMetricsDao(): ProgressMetricsDao
    
    // Legacy DAOs
    abstract fun judgmentDao(): JudgmentDao
    abstract fun summaryDao(): SummaryDao
    abstract fun syncQueueDao(): SyncQueueDao
    
    companion object {
        const val DATABASE_NAME = "truth_training.sqlite"

        @Volatile
        private var INSTANCE: TruthDatabase? = null

        private fun buildDatabase(context: Context): TruthDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TruthDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(TruthDatabaseMigrations.MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun getInstance(context: Context): TruthDatabase {
            val current = INSTANCE
            if (current != null && current.isOpen) {
                return current
            }
            return synchronized(this) {
                val existing = INSTANCE
                if (existing != null && existing.isOpen) {
                    existing
                } else {
                    existing?.close()
                    val db = buildDatabase(context)
                    INSTANCE = db
                    db
                }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}

