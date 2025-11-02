package com.truth.training.client.data.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.JudgmentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Unit tests for JudgmentDao.
 */
@RunWith(AndroidJUnit4::class)
class JudgmentDaoTest {
    private lateinit var database: TruthDatabase
    private lateinit var judgmentDao: JudgmentDao
    private lateinit var eventDao: EventDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()
        judgmentDao = database.judgmentDao()
        eventDao = database.eventDao()
        
        // Insert test event
        runBlocking {
            eventDao.insertEvent(
                com.truth.training.client.data.database.entities.EventEntity(
                    id = "event_1",
                    title = "Test Event",
                    description = null,
                    categoryId = null,
                    formaId = null,
                    causeId = null,
                    developId = null,
                    effectId = null,
                    startDate = null,
                    endDate = null,
                    createdAt = "2024-01-01T00:00:00Z",
                    updatedAt = null,
                    status = "active"
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetJudgmentById() = runBlocking {
        val judgment = createTestJudgment("judg_1", "event_1")
        judgmentDao.insertJudgment(judgment)
        
        val retrieved = judgmentDao.getJudgmentById("judg_1")
        assertNotNull(retrieved)
        assertEquals("event_1", retrieved!!.eventId)
        assertEquals("true", retrieved.assessment)
        assertEquals(0.8, retrieved.confidenceLevel, 0.01)
    }

    @Test
    fun listJudgmentsForEvent() = runBlocking {
        repeat(5) { i ->
            judgmentDao.insertJudgment(
                createTestJudgment("judg_$i", "event_1", assessment = if (i % 2 == 0) "true" else "false")
            )
        }
        
        val judgments = judgmentDao.listJudgmentsForEvent("event_1", limit = 10, offset = 0)
        assertEquals(5, judgments.size)
    }

    @Test
    fun listJudgmentsForEventFlow() = runBlocking {
        repeat(3) { i ->
            judgmentDao.insertJudgment(createTestJudgment("judg_$i", "event_1"))
        }
        
        val flow = judgmentDao.listJudgmentsForEventFlow("event_1")
        val judgments = flow.first()
        assertEquals(3, judgments.size)
    }

    @Test
    fun countJudgmentsByAssessment() = runBlocking {
        judgmentDao.insertJudgment(createTestJudgment("judg_1", "event_1", "true"))
        judgmentDao.insertJudgment(createTestJudgment("judg_2", "event_1", "true"))
        judgmentDao.insertJudgment(createTestJudgment("judg_3", "event_1", "false"))
        judgmentDao.insertJudgment(createTestJudgment("judg_4", "event_1", "uncertain"))
        
        val trueCount = judgmentDao.countJudgmentsByAssessment("event_1", "true")
        val falseCount = judgmentDao.countJudgmentsByAssessment("event_1", "false")
        val uncertainCount = judgmentDao.countJudgmentsByAssessment("event_1", "uncertain")
        
        assertEquals(2, trueCount)
        assertEquals(1, falseCount)
        assertEquals(1, uncertainCount)
    }

    @Test
    fun getAverageConfidence() = runBlocking {
        judgmentDao.insertJudgment(createTestJudgment("judg_1", "event_1", confidenceLevel = 0.9))
        judgmentDao.insertJudgment(createTestJudgment("judg_2", "event_1", confidenceLevel = 0.8))
        judgmentDao.insertJudgment(createTestJudgment("judg_3", "event_1", confidenceLevel = 0.7))
        
        val avgConfidence = judgmentDao.getAverageConfidence("event_1")
        assertNotNull(avgConfidence)
        assertEquals(0.8, avgConfidence!!, 0.01) // (0.9 + 0.8 + 0.7) / 3 = 0.8
    }

    @Test
    fun getJudgmentCountForEvent() = runBlocking {
        repeat(7) { i ->
            judgmentDao.insertJudgment(createTestJudgment("judg_$i", "event_1"))
        }
        
        val count = judgmentDao.getJudgmentCountForEvent("event_1")
        assertEquals(7, count)
    }

    @Test
    fun updateAndDeleteJudgment() = runBlocking {
        val judgment = createTestJudgment("judg_update", "event_1")
        judgmentDao.insertJudgment(judgment)
        
        val updated = judgment.copy(assessment = "false", confidenceLevel = 0.5)
        judgmentDao.updateJudgment(updated)
        
        val retrieved = judgmentDao.getJudgmentById("judg_update")
        assertEquals("false", retrieved!!.assessment)
        assertEquals(0.5, retrieved.confidenceLevel, 0.01)
        
        judgmentDao.deleteJudgment(retrieved)
        val deleted = judgmentDao.getJudgmentById("judg_update")
        assertNull(deleted)
    }

    private fun createTestJudgment(
        id: String = "judg_test",
        eventId: String = "event_1",
        assessment: String = "true",
        confidenceLevel: Double = 0.8,
        reasoning: String? = "Test reasoning"
    ): JudgmentEntity {
        return JudgmentEntity(
            id = id,
            eventId = eventId,
            assessment = assessment,
            confidenceLevel = confidenceLevel,
            reasoning = reasoning,
            submittedAt = "2024-01-01T00:00:00Z"
        )
    }
}

