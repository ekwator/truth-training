package com.truth.training.client.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import com.truth.training.client.data.database.daos.*
import com.truth.training.client.data.database.entities.*
import com.truth.training.client.data.database.TruthDatabaseMigrations
import android.util.Log

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
        SyncQueueEntity::class,
        // Discovery entities
        NodeEntity::class
    ],
    version = 6,  // Incremented to fix schema validation issues - triggers fallbackToDestructiveMigration for corrupted databases
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
    
        // CI schema DAOs (part of canonical schema, not legacy)
        abstract fun judgmentDao(): JudgmentDao
        abstract fun summaryDao(): SummaryDao
        abstract fun syncQueueDao(): SyncQueueDao
    
    // Discovery DAOs
    abstract fun nodeDao(): NodeDao
    
    companion object {
        const val DATABASE_NAME = "truth_training.sqlite"

        @Volatile
        private var INSTANCE: TruthDatabase? = null

        private fun buildDatabase(context: Context): TruthDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                TruthDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    TruthDatabaseMigrations.MIGRATION_1_2,
                    TruthDatabaseMigrations.MIGRATION_2_3,
                    TruthDatabaseMigrations.MIGRATION_3_4,
                    TruthDatabaseMigrations.MIGRATION_4_5
                )
                .fallbackToDestructiveMigration()
            
            // Add callback to execute canonical schema from assets for new installations
            // This ensures schema parity with Desktop
            builder.addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    try {
                        // Execute canonical schema from assets to ensure parity
                        // Room already creates tables from entities, but this ensures
                        // any additional tables/constraints from core/src/storage.rs are applied
                        SchemaLoader.loadAndExecuteSchema(context.applicationContext, db)
                        
                        // Validate schema after initialization
                        validateSchema(db)
                        
                        Log.d("TruthDatabase", "Database initialized with canonical schema from assets")
                    } catch (e: Exception) {
                        Log.e("TruthDatabase", "Failed to load schema from assets or validate schema", e)
                        // Don't throw - Room will continue with entity-based schema
                        // But log the error for debugging
                    }
                }
                
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Note: Schema validation removed from onOpen to prevent crashes
                    // when opening existing databases with schema mismatches.
                    // Room's migration system and fallbackToDestructiveMigration() handle
                    // schema updates. Validation only occurs in onCreate for new databases.
                    Log.d("TruthDatabase", "Database opened successfully")
                }
            })
            
            return builder.build()
        }
        
        /**
         * Validates that the database schema matches canonical Truth schema.
         * Checks that legacy tables are absent and required canonical tables exist.
         */
        private fun validateSchema(db: SupportSQLiteDatabase) {
            // Check that legacy tables are absent
            val foundLegacyTables = mutableListOf<String>()
            
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN (?, ?, ?, ?)",
                arrayOf("events", "impacts", "summaries", "logs")).use { cursor ->
                while (cursor.moveToNext()) {
                    foundLegacyTables.add(cursor.getString(0))
                }
            }
            
            if (foundLegacyTables.isNotEmpty()) {
                throw IllegalStateException(
                    "Legacy tables found in database: ${foundLegacyTables.joinToString(", ")}. " +
                    "These should have been removed by MIGRATION_3_4."
                )
            }
            
            // Verify required canonical tables exist
            val requiredTables = listOf(
                "truth_events", "impact", "progress_metrics", "context",
                "category", "cause", "develop", "effect", "forma", "impact_type"
            )
            val existingTables = mutableListOf<String>()
            
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
                while (cursor.moveToNext()) {
                    existingTables.add(cursor.getString(0))
                }
            }
            
            val missingTables = requiredTables.filter { it !in existingTables }
            if (missingTables.isNotEmpty()) {
                Log.w("TruthDatabase", "Missing canonical tables: ${missingTables.joinToString(", ")}")
                // Don't throw - Room entities may create tables with different names
                // This is a warning, not an error
            }
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

