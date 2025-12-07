package com.truth.training.client.data.database

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.truth.training.client.data.database.daos.*
import com.truth.training.client.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds knowledge base tables with initial data (similar to Desktop UI seed_knowledge_base).
 * 
 * This ensures that category, forma, cause, develop, effect, impact_type, and context
 * tables are populated with default values on first database initialization.
 * 
 * CRITICAL: ID values MUST be identical across all languages to maintain referential integrity.
 * The knowledge base schema is simple - records are not deleted or added, only names change.
 * When changing language, tables are cleared and re-seeded with the same IDs but different names.
 * This ensures that existing events maintain their foreign key relationships.
 * 
 * All seeding operations are wrapped in transactions to ensure atomicity and data integrity.
 */
object KnowledgeBaseSeeder {
    private const val TAG = "KnowledgeBaseSeeder"
    
    /**
     * Seeds knowledge base tables with initial data.
     * Should be called after database schema is created but before first use.
     * 
     * @param database TruthDatabase instance
     * @param locale Optional locale ("ru" or "en"), defaults to "en"
     * @param forceReseed If true, clears existing data before seeding. Defaults to false.
     */
    suspend fun seedKnowledgeBase(
        database: TruthDatabase,
        locale: String = "en",
        forceReseed: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            val categoryDao = database.categoryDao()
            val formaDao = database.formaDao()
            val causeDao = database.causeDao()
            val developDao = database.developDao()
            val effectDao = database.effectDao()
            val contextTemplateDao = database.contextTemplateDao()
            val impactTypeDao = database.impactTypeDao()
            
            // Check if data already exists (unless force reseed is requested)
            if (!forceReseed) {
                val categoryCount = categoryDao.getCategoryCount()
                if (categoryCount > 0) {
                    Log.d(TAG, "Knowledge base already seeded (found $categoryCount categories), skipping")
                    return@withContext
                }
            }
            
            // Use transaction to ensure atomicity of clear and seed operations
            // This guarantees data integrity and consistent IDs across language changes
            // CRITICAL: Use temporary tables to preserve event data during knowledge base re-seeding
            // When deleting knowledge base records, foreign keys with SET_NULL will nullify
            // context fields in truth_events. By using temporary tables, we preserve the data
            // and restore it after re-seeding, maintaining referential integrity.
            database.withTransaction {
                if (forceReseed) {
                    // Use temporary tables to preserve event data during knowledge base re-seeding
                    Log.d(TAG, "Force reseed requested, using temporary tables to preserve event data")
                    reseedKnowledgeBaseWithTemporaryTables(
                        database = database,
                        categoryDao = categoryDao,
                        formaDao = formaDao,
                        causeDao = causeDao,
                        developDao = developDao,
                        effectDao = effectDao,
                        contextTemplateDao = contextTemplateDao,
                        impactTypeDao = impactTypeDao,
                        locale = locale
                    )
                } else {
                    Log.d(TAG, "Seeding knowledge base with locale: $locale")
                    
                    if (locale == "ru") {
                        seedRussian(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
                    } else {
                        seedEnglish(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
                    }
                }
            }
            
            Log.d(TAG, "Knowledge base seeding completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed knowledge base", e)
            // Don't throw - allow app to continue even if seeding fails
        }
    }
    
    /**
     * Clears all knowledge base tables (categories, formas, causes, develops, effects, impact types, context templates).
     * Used when changing language to ensure clean state before reseeding.
     * 
     * @param database TruthDatabase instance
     */
    suspend fun clearKnowledgeBase(database: TruthDatabase) = withContext(Dispatchers.IO) {
        try {
            val categoryDao = database.categoryDao()
            val formaDao = database.formaDao()
            val causeDao = database.causeDao()
            val developDao = database.developDao()
            val effectDao = database.effectDao()
            val contextTemplateDao = database.contextTemplateDao()
            val impactTypeDao = database.impactTypeDao()
            
            clearKnowledgeBase(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear knowledge base", e)
            throw e
        }
    }
    
    private suspend fun clearKnowledgeBase(
        database: TruthDatabase,
        categoryDao: CategoryDao,
        formaDao: FormaDao,
        causeDao: CauseDao,
        developDao: DevelopDao,
        effectDao: EffectDao,
        contextTemplateDao: ContextTemplateDao,
        impactTypeDao: ImpactTypeDao
    ) {
        Log.d(TAG, "Clearing all knowledge base tables")
        
        // Clear in order: templates first (they reference other tables), then other tables
        contextTemplateDao.clearAllTemplates()
        categoryDao.clearAllCategories()
        formaDao.clearAllFormas()
        causeDao.clearAllCauses()
        developDao.clearAllDevelops()
        effectDao.clearAllEffects()
        impactTypeDao.clearAllImpactTypes()
        
        Log.d(TAG, "Knowledge base tables cleared successfully")
    }
    
    /**
     * Re-seeds knowledge base using temporary tables to preserve event data.
     * 
     * CRITICAL: When deleting knowledge base records, foreign keys with SET_NULL will nullify
     * context fields in truth_events. This method uses a move operation (copy + delete) to
     * preserve data integrity:
     * 1. Creates empty temporary tables for truth_events, impact, progress_metrics
     * 2. Moves (copies and deletes) data from main tables to temporary tables
     * 3. Deletes knowledge base records (which would nullify FK fields, but data is already moved)
     * 4. Inserts new knowledge base records with same IDs but different names
     * 5. Restores data from temporary tables back to main tables (FK relationships are preserved)
     * 6. Drops temporary tables
     * 
     * This ensures that context fields in events remain intact after language change.
     * All operations are performed within a single transaction for atomicity.
     */
    private suspend fun reseedKnowledgeBaseWithTemporaryTables(
        database: TruthDatabase,
        categoryDao: CategoryDao,
        formaDao: FormaDao,
        causeDao: CauseDao,
        developDao: DevelopDao,
        effectDao: EffectDao,
        contextTemplateDao: ContextTemplateDao,
        impactTypeDao: ImpactTypeDao,
        locale: String
    ) {
        val db = database.openHelper.writableDatabase
        
        try {
            Log.d(TAG, "Step 1: Creating empty temporary tables")
            // Create empty temporary tables with same structure as main tables
            db.execSQL("""
                CREATE TEMP TABLE temp_truth_events (
                    id INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    category_id INTEGER,
                    forma_id INTEGER,
                    cause_id INTEGER,
                    develop_id INTEGER,
                    effect_id INTEGER,
                    vector INTEGER NOT NULL,
                    detected INTEGER,
                    corrected INTEGER NOT NULL,
                    timestamp_start INTEGER NOT NULL,
                    timestamp_end INTEGER,
                    code INTEGER NOT NULL,
                    collective_score REAL
                )
            """.trimIndent())
            
            db.execSQL("""
                CREATE TEMP TABLE temp_impact (
                    id INTEGER PRIMARY KEY,
                    event_id INTEGER NOT NULL,
                    type_id INTEGER NOT NULL,
                    value INTEGER NOT NULL,
                    notes TEXT,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            
            db.execSQL("""
                CREATE TEMP TABLE temp_progress_metrics (
                    id INTEGER PRIMARY KEY,
                    timestamp INTEGER NOT NULL,
                    total_events INTEGER NOT NULL,
                    total_events_group INTEGER NOT NULL,
                    total_positive_impact REAL NOT NULL,
                    total_positive_impact_group REAL NOT NULL,
                    total_negative_impact REAL NOT NULL,
                    total_negative_impact_group REAL NOT NULL,
                    trend REAL NOT NULL,
                    trend_group REAL NOT NULL
                )
            """.trimIndent())
            
            Log.d(TAG, "Step 2: Moving data from main tables to temporary tables")
            // Move data: copy to temp, then delete from main (move operation)
            // This preserves data before knowledge base deletion nullifies FK fields
            db.execSQL("""
                INSERT INTO temp_truth_events 
                (id, description, category_id, forma_id, cause_id, develop_id, effect_id, 
                 vector, detected, corrected, timestamp_start, timestamp_end, code, collective_score)
                SELECT 
                    id, description, category_id, forma_id, cause_id, develop_id, effect_id,
                    vector, detected, corrected, timestamp_start, timestamp_end, code, collective_score
                FROM truth_events
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO temp_impact 
                (id, event_id, type_id, value, notes, created_at)
                SELECT 
                    id, event_id, type_id, value, notes, created_at
                FROM impact
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO temp_progress_metrics 
                (id, timestamp, total_events, total_events_group, 
                 total_positive_impact, total_positive_impact_group,
                 total_negative_impact, total_negative_impact_group,
                 trend, trend_group)
                SELECT 
                    id, timestamp, total_events, total_events_group,
                    total_positive_impact, total_positive_impact_group,
                    total_negative_impact, total_negative_impact_group,
                    trend, trend_group
                FROM progress_metrics
            """.trimIndent())
            
            // Delete data from main tables (completing the move operation)
            db.execSQL("DELETE FROM truth_events")
            db.execSQL("DELETE FROM impact")
            db.execSQL("DELETE FROM progress_metrics")
            
            Log.d(TAG, "Step 3: Clearing knowledge base tables")
            // Clear knowledge base tables
            // Note: FK fields in truth_events are already NULL (data was moved), so no nullification occurs
            clearKnowledgeBase(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
            
            Log.d(TAG, "Step 4: Seeding knowledge base with locale: $locale")
            // Seed knowledge base with new locale (same IDs, different names)
            if (locale == "ru") {
                seedRussian(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
            } else {
                seedEnglish(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
            }
            
            Log.d(TAG, "Step 5: Restoring data from temporary tables to main tables")
            // Restore data from temporary tables back to main tables
            // FK relationships are preserved because IDs in knowledge_base are the same
            db.execSQL("""
                INSERT INTO truth_events 
                (id, description, category_id, forma_id, cause_id, develop_id, effect_id, 
                 vector, detected, corrected, timestamp_start, timestamp_end, code, collective_score)
                SELECT 
                    id, description, category_id, forma_id, cause_id, develop_id, effect_id,
                    vector, detected, corrected, timestamp_start, timestamp_end, code, collective_score
                FROM temp_truth_events
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO impact 
                (id, event_id, type_id, value, notes, created_at)
                SELECT 
                    id, event_id, type_id, value, notes, created_at
                FROM temp_impact
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO progress_metrics 
                (id, timestamp, total_events, total_events_group, 
                 total_positive_impact, total_positive_impact_group,
                 total_negative_impact, total_negative_impact_group,
                 trend, trend_group)
                SELECT 
                    id, timestamp, total_events, total_events_group,
                    total_positive_impact, total_positive_impact_group,
                    total_negative_impact, total_negative_impact_group,
                    trend, trend_group
                FROM temp_progress_metrics
            """.trimIndent())
            
            Log.d(TAG, "Step 6: Dropping temporary tables")
            // Drop temporary tables (they are automatically dropped when connection closes, but explicit is better)
            db.execSQL("DROP TABLE IF EXISTS temp_truth_events")
            db.execSQL("DROP TABLE IF EXISTS temp_impact")
            db.execSQL("DROP TABLE IF EXISTS temp_progress_metrics")
            
            Log.d(TAG, "Knowledge base re-seeding with temporary tables completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-seed knowledge base with temporary tables", e)
            // Try to clean up temporary tables on error
            try {
                db.execSQL("DROP TABLE IF EXISTS temp_truth_events")
                db.execSQL("DROP TABLE IF EXISTS temp_impact")
                db.execSQL("DROP TABLE IF EXISTS temp_progress_metrics")
            } catch (cleanupError: Exception) {
                Log.e(TAG, "Failed to clean up temporary tables", cleanupError)
            }
            throw e
        }
    }
    
    private suspend fun seedEnglish(
        database: TruthDatabase,
        categoryDao: CategoryDao,
        formaDao: FormaDao,
        causeDao: CauseDao,
        developDao: DevelopDao,
        effectDao: EffectDao,
        contextTemplateDao: ContextTemplateDao,
        impactTypeDao: ImpactTypeDao
    ) {
        // Categories
        val categories = listOf(
            CategoryEntity(1, "Social", "Communication, reputation, trust"),
            CategoryEntity(2, "Financial", "Money, property, contracts"),
            CategoryEntity(3, "Political", "State, treaties, international relations"),
            CategoryEntity(4, "Legal", "Law, compliance, courts"),
            CategoryEntity(5, "Personal", "Self-assessment, inner decisions"),
            CategoryEntity(6, "Organizational", "Teams, companies, processes"),
            CategoryEntity(7, "Media", "Information, press, platforms"),
            CategoryEntity(8, "Technological", "IT systems, data, security")
        )
        categories.forEach { categoryDao.insertCategory(it) }
        
        // Causes
        val causes = listOf(
            CauseEntity(1, "Fear", false, "Avoidance of punishment or blame"),
            CauseEntity(2, "Benefit", false, "Material/personal interest"),
            CauseEntity(3, "Mercy", true, "Compassion, care for others"),
            CauseEntity(4, "Ignorance", false, "Lack of knowledge, mistakes"),
            CauseEntity(5, "Duty", true, "Obligation, responsibility"),
            CauseEntity(6, "Curiosity", true, "Search for truth, inquiry"),
            CauseEntity(7, "Pressure", false, "Coercion, conformism"),
            CauseEntity(8, "Care", true, "Protecting another's good")
        )
        causes.forEach { causeDao.insertCause(it) }
        
        // Develops
        val develops = listOf(
            DevelopEntity(1, "Concealment", false, "Intentional omission/withholding"),
            DevelopEntity(2, "Manipulation", false, "Distortion, pressure, context switch"),
            DevelopEntity(3, "Transparency", true, "Openness, factual availability"),
            DevelopEntity(4, "Verification", true, "Cross-checking sources"),
            DevelopEntity(5, "Exaggeration", false, "Overstatement, false salience"),
            DevelopEntity(6, "Confession", true, "Owning mistakes, remediation")
        )
        develops.forEach { developDao.insertDevelop(it) }
        
        // Effects
        val effects = listOf(
            EffectEntity(1, "Distrust", false, "Erodes trust and ties"),
            EffectEntity(2, "Trust", true, "Strengthens cooperation"),
            EffectEntity(3, "Conflict", false, "Escalation, confrontation"),
            EffectEntity(4, "Reconciliation", true, "Reduced tension, alignment"),
            EffectEntity(5, "Sanctions", false, "Legal/reputational penalties"),
            EffectEntity(6, "Learning", true, "Competence growth, insights"),
            EffectEntity(7, "Reputation Loss", false, "Status decrease"),
            EffectEntity(8, "Reputation Gain", true, "Status increase")
        )
        effects.forEach { effectDao.insertEffect(it) }
        
        // Formas
        val formas = listOf(
            FormaEntity(1, "Deception", false, "Conscious distortion of reality"),
            FormaEntity(2, "Truth", true, "Conformance to facts and checks"),
            FormaEntity(3, "Self-deception", false, "Distortion to reassure oneself"),
            FormaEntity(4, "Half-truth", false, "Partial truth with distortions"),
            FormaEntity(5, "Silence", false, "Withholding significant info"),
            FormaEntity(6, "Openness", true, "Proactive disclosure of facts")
        )
        formas.forEach { formaDao.insertForma(it) }
        
        // Impact Types
        val impactTypes = listOf(
            ImpactTypeEntity(1, "Reputation", "Social capital, trust"),
            ImpactTypeEntity(2, "Finance", "Money, assets, liabilities"),
            ImpactTypeEntity(3, "Emotions", "Stress, confidence, motivation"),
            ImpactTypeEntity(4, "Law", "Legal risks, sanctions"),
            ImpactTypeEntity(5, "Health", "Physical/mental condition"),
            ImpactTypeEntity(6, "Time", "Time losses/gains")
        )
        impactTypes.forEach { impactTypeDao.insertImpactType(it) }
        
        // Context Templates
        val contexts = listOf(
            ContextTemplateEntity(
                id = 1,
                name = "Interpersonal: openness",
                categoryId = 1,
                formaId = 2,
                causeId = 5,
                developId = 3,
                effectId = 2,
                description = "Honest dialogue, strengthening trust"
            ),
            ContextTemplateEntity(
                id = 2,
                name = "Interpersonal: concealment",
                categoryId = 1,
                formaId = 1,
                causeId = 1,
                developId = 1,
                effectId = 1,
                description = "Withholding a significant fact, trust erosion"
            ),
            ContextTemplateEntity(
                id = 3,
                name = "Finance: fraud",
                categoryId = 2,
                formaId = 1,
                causeId = 2,
                developId = 2,
                effectId = 5,
                description = "Deception for profit, legal consequences"
            ),
            ContextTemplateEntity(
                id = 4,
                name = "Finance: transparent reporting",
                categoryId = 2,
                formaId = 2,
                causeId = 5,
                developId = 4,
                effectId = 8,
                description = "Verifiable facts, reputation growth"
            ),
            ContextTemplateEntity(
                id = 5,
                name = "Politics: treaty breach",
                categoryId = 3,
                formaId = 1,
                causeId = 2,
                developId = 1,
                effectId = 1,
                description = "Hidden violations, loss of trust"
            ),
            ContextTemplateEntity(
                id = 6,
                name = "Politics: treaty compliance",
                categoryId = 3,
                formaId = 2,
                causeId = 5,
                developId = 4,
                effectId = 2,
                description = "Confirmed execution of obligations"
            ),
            ContextTemplateEntity(
                id = 7,
                name = "Organization: admitting a mistake",
                categoryId = 6,
                formaId = 2,
                causeId = 5,
                developId = 6,
                effectId = 6,
                description = "Admission and correction improve learning"
            ),
            ContextTemplateEntity(
                id = 8,
                name = "Media: disinformation",
                categoryId = 7,
                formaId = 1,
                causeId = 7,
                developId = 2,
                effectId = 3,
                description = "Manipulations leading to conflict"
            )
        )
        contexts.forEach { contextTemplateDao.insertTemplate(it) }
    }
    
    private suspend fun seedRussian(
        database: TruthDatabase,
        categoryDao: CategoryDao,
        formaDao: FormaDao,
        causeDao: CauseDao,
        developDao: DevelopDao,
        effectDao: EffectDao,
        contextTemplateDao: ContextTemplateDao,
        impactTypeDao: ImpactTypeDao
    ) {
        // Categories (Russian)
        val categories = listOf(
            CategoryEntity(1, "Социальный", "Общение, репутация, доверие"),
            CategoryEntity(2, "Финансовый", "Деньги, собственность, договоры"),
            CategoryEntity(3, "Политический", "Государство, договоры, международные отношения"),
            CategoryEntity(4, "Правовой", "Закон, соблюдение норм, суд"),
            CategoryEntity(5, "Личный", "Самооценка, внутренние решения"),
            CategoryEntity(6, "Организационный", "Команды, компании, процессы"),
            CategoryEntity(7, "Медиа", "Информация, СМИ, платформы"),
            CategoryEntity(8, "Технологический", "ИТ-системы, данные, безопасность")
        )
        categories.forEach { categoryDao.insertCategory(it) }
        
        // Causes (Russian)
        val causes = listOf(
            CauseEntity(1, "Страх", false, "Избежание наказания, осуждения"),
            CauseEntity(2, "Выгода", false, "Материальный/личный интерес"),
            CauseEntity(3, "Милосердие", true, "Сострадание, забота о другом"),
            CauseEntity(4, "Неведение", false, "Отсутствие знаний, ошибки"),
            CauseEntity(5, "Долг", true, "Обязанность, ответственность"),
            CauseEntity(6, "Любопытство", true, "Поиск истины, исследование"),
            CauseEntity(7, "Давление", false, "Принуждение, конформизм"),
            CauseEntity(8, "Забота", true, "Охрана блага другого")
        )
        causes.forEach { causeDao.insertCause(it) }
        
        // Develops (Russian)
        val develops = listOf(
            DevelopEntity(1, "Сокрытие", false, "Умышленное недосказание/умолчание"),
            DevelopEntity(2, "Манипуляция", false, "Искажение, давление, подмена контекста"),
            DevelopEntity(3, "Прозрачность", true, "Открытость, доступность фактов"),
            DevelopEntity(4, "Проверка", true, "Верификация, сопоставление источников"),
            DevelopEntity(5, "Преувеличение", false, "Гипербола, ложная значимость"),
            DevelopEntity(6, "Признание", true, "Принятие ответственности, исправление")
        )
        develops.forEach { developDao.insertDevelop(it) }
        
        // Effects (Russian)
        val effects = listOf(
            EffectEntity(1, "Недоверие", false, "Подрыв доверия, разрыв связей"),
            EffectEntity(2, "Доверие", true, "Укрепление отношений, кооперация"),
            EffectEntity(3, "Конфликт", false, "Эскалация, противостояние"),
            EffectEntity(4, "Примирение", true, "Снижение напряжения, согласие"),
            EffectEntity(5, "Санкции", false, "Юридические/репутационные последствия"),
            EffectEntity(6, "Обучение", true, "Рост компетентности, выводы"),
            EffectEntity(7, "Потеря репутации", false, "Снижение статуса"),
            EffectEntity(8, "Рост репутации", true, "Укрепление статуса")
        )
        effects.forEach { effectDao.insertEffect(it) }
        
        // Formas (Russian)
        val formas = listOf(
            FormaEntity(1, "Обман", false, "Сознательное искажение реальности"),
            FormaEntity(2, "Правда", true, "Соответствие фактам и проверкам"),
            FormaEntity(3, "Самообман", false, "Искажение для успокоения себя"),
            FormaEntity(4, "Полуправда", false, "Частичное искажение с верными фрагментами"),
            FormaEntity(5, "Умолчание", false, "Сокрытие значимой информации"),
            FormaEntity(6, "Открытость", true, "Проактивное раскрытие фактов")
        )
        formas.forEach { formaDao.insertForma(it) }
        
        // Impact Types (Russian)
        val impactTypes = listOf(
            ImpactTypeEntity(1, "Репутация", "Социальный капитал, доверие"),
            ImpactTypeEntity(2, "Финансы", "Деньги, активы, обязательства"),
            ImpactTypeEntity(3, "Эмоции", "Стресс, уверенность, мотивация"),
            ImpactTypeEntity(4, "Право", "Юридические риски, санкции"),
            ImpactTypeEntity(5, "Здоровье", "Физическое/психическое состояние"),
            ImpactTypeEntity(6, "Время", "Потери/выигрыш времени")
        )
        impactTypes.forEach { impactTypeDao.insertImpactType(it) }
        
        // Context Templates (Russian)
        val contexts = listOf(
            ContextTemplateEntity(
                id = 1,
                name = "Межличностные отношения: открытость",
                categoryId = 1,
                formaId = 2,
                causeId = 5,
                developId = 3,
                effectId = 2,
                description = "Честный диалог, укрепление доверия"
            ),
            ContextTemplateEntity(
                id = 2,
                name = "Межличностные отношения: сокрытие",
                categoryId = 1,
                formaId = 1,
                causeId = 1,
                developId = 1,
                effectId = 1,
                description = "Умолчание значимого факта, эрозия доверия"
            ),
            ContextTemplateEntity(
                id = 3,
                name = "Финансы: мошенничество",
                categoryId = 2,
                formaId = 1,
                causeId = 2,
                developId = 2,
                effectId = 5,
                description = "Обман с целью выгоды, юридические последствия"
            ),
            ContextTemplateEntity(
                id = 4,
                name = "Финансы: прозрачная отчётность",
                categoryId = 2,
                formaId = 2,
                causeId = 5,
                developId = 4,
                effectId = 8,
                description = "Проверяемость фактов, рост репутации"
            ),
            ContextTemplateEntity(
                id = 5,
                name = "Политика: нарушение договора",
                categoryId = 3,
                formaId = 1,
                causeId = 2,
                developId = 1,
                effectId = 1,
                description = "Сокрытие нарушений, падение доверия"
            ),
            ContextTemplateEntity(
                id = 6,
                name = "Политика: соблюдение договора",
                categoryId = 3,
                formaId = 2,
                causeId = 5,
                developId = 4,
                effectId = 2,
                description = "Подтверждённое выполнение обязательств"
            ),
            ContextTemplateEntity(
                id = 7,
                name = "Организация: признание ошибки",
                categoryId = 6,
                formaId = 2,
                causeId = 5,
                developId = 6,
                effectId = 6,
                description = "Признание и исправление повышают обучаемость"
            ),
            ContextTemplateEntity(
                id = 8,
                name = "Медиа: дезинформация",
                categoryId = 7,
                formaId = 1,
                causeId = 7,
                developId = 2,
                effectId = 3,
                description = "Манипуляции, приводящие к конфликтам"
            )
        )
        contexts.forEach { contextTemplateDao.insertTemplate(it) }
    }
}

