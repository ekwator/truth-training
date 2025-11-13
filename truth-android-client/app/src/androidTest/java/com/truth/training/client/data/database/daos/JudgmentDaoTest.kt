package com.truth.training.client.data.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.database.entities.JudgmentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JudgmentDaoTest {
    private lateinit var database: TruthDatabase
    private lateinit var judgmentDao: JudgmentDao
    private lateinit var eventDao: EventDao
    private var eventId: Long = 0L

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()
        judgmentDao = database.judgmentDao()
        eventDao = database.eventDao()

        runBlocking {
            eventId = eventDao.insertEvent(
                EventEntity(
                    description = "Test Event",
                    timestampStart = System.currentTimeMillis(),
                    vector = true
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
        val judgment = createTestJudgment("judg_1")
        judgmentDao.insertJudgment(judgment)

        val retrieved = judgmentDao.getJudgmentById("judg_1")
        assertNotNull(retrieved)
        assertEquals(eventId, retrieved!!.eventId)
        assertEquals("true", retrieved.assessment)
        assertEquals(0.8, retrieved.confidenceLevel, 0.01)
    }

    @Test
    fun listJudgmentsForEvent() = runBlocking {
        repeat(5) { i ->
            judgmentDao.insertJudgment(createTestJudgment("judg_$i", assessment = if (i % 2 == 0) "true" else "false"))
        }

        val judgments = judgmentDao.listJudgmentsForEvent(eventId, limit = 10, offset = 0)
        assertEquals(5, judgments.size)
    }

    @Test
    fun listJudgmentsForEventFlow() = runBlocking {
        repeat(3) { i ->
            judgmentDao.insertJudgment(createTestJudgment("judg_$i"))
        }

        val flow = judgmentDao.listJudgmentsForEventFlow(eventId)
        val judgments = flow.first()
        assertEquals(3, judgments.size)
    }

    @Test
    fun countJudgmentsByAssessment() = runBlocking {
        judgmentDao.insertJudgment(createTestJudgment("judg_1", assessment = "true"))
        judgmentDao.insertJudgment(createTestJudgment("judg_2", assessment = "true"))
        judgmentDao.insertJudgment(createTestJudgment("judg_3", assessment = "false"))
        judgmentDao.insertJudgment(createTestJudgment("judg_4", assessment = "uncertain"))

        val trueCount = judgmentDao.countJudgmentsByAssessment(eventId, "true")
        val falseCount = judgmentDao.countJudgmentsByAssessment(eventId, "false")
        val uncertainCount = judgmentDao.countJudgmentsByAssessment(eventId, "uncertain")

        assertEquals(2, trueCount)
        assertEquals(1, falseCount)
        assertEquals(1, uncertainCount)
    }

    @Test
    fun getAverageConfidence() = runBlocking {
        judgmentDao.insertJudgment(createTestJudgment("judg_1", confidenceLevel = 0.9))
        judgmentDao.insertJudgment(createTestJudgment("judg_2", confidenceLevel = 0.8))
        judgmentDao.insertJudgment(createTestJudgment("judg_3", confidenceLevel = 0.7))

        val avgConfidence = judgmentDao.averageConfidence(eventId)
        assertNotNull(avgConfidence)
        assertEquals(0.8, avgConfidence!!, 0.01)
    }

    @Test
    fun getJudgmentCountForEvent() = runBlocking {
        repeat(7) { i ->
            judgmentDao.insertJudgment(createTestJudgment("judg_$i"))
        }

        val count = judgmentDao.countJudgmentsForEvent(eventId)
        assertEquals(7, count)
    }

    @Test
    fun updateAndDeleteJudgment() = runBlocking {
        val judgment = createTestJudgment("judg_update")
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

