package com.truth.training.client.data.database

import android.content.Context
import android.util.Log
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
 * Note: Currently seeds English data only (Android app is EN-only).
 * Russian data can be added in the future if localization is implemented.
 */
object KnowledgeBaseSeeder {
    private const val TAG = "KnowledgeBaseSeeder"
    
    /**
     * Seeds knowledge base tables with initial data.
     * Should be called after database schema is created but before first use.
     * 
     * @param database TruthDatabase instance
     * @param locale Optional locale ("ru" or "en"), defaults to "en"
     */
    suspend fun seedKnowledgeBase(
        database: TruthDatabase,
        locale: String = "en"
    ) = withContext(Dispatchers.IO) {
        try {
            val categoryDao = database.categoryDao()
            val formaDao = database.formaDao()
            val causeDao = database.causeDao()
            val developDao = database.developDao()
            val effectDao = database.effectDao()
            val contextTemplateDao = database.contextTemplateDao()
            val impactTypeDao = database.impactTypeDao()
            
            // Check if data already exists
            // Use a one-time check - if categories exist, skip seeding
            val categoryCount = categoryDao.getCategoryCount()
            if (categoryCount > 0) {
                Log.d(TAG, "Knowledge base already seeded (found $categoryCount categories), skipping")
                return@withContext
            }
            
            Log.d(TAG, "Seeding knowledge base with locale: $locale")
            
            if (locale == "ru") {
                seedRussian(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
            } else {
                seedEnglish(database, categoryDao, formaDao, causeDao, developDao, effectDao, contextTemplateDao, impactTypeDao)
            }
            
            Log.d(TAG, "Knowledge base seeding completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed knowledge base", e)
            // Don't throw - allow app to continue even if seeding fails
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

