package com.truth.training.client.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.data.repository.JudgmentRepository
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.network.dto.CreateJudgmentRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration test: Scenario 3 - Judgment submission and consensus calculation.
 * Validates consensus mechanisms and truth convergence.
 */
@RunWith(AndroidJUnit4::class)
class JudgmentConsensusTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventRepository: EventRepository
    private lateinit var judgmentRepository: JudgmentRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TruthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepository = EventRepository(database, null)
        judgmentRepository = JudgmentRepository(database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun submitJudgmentsAndCalculateConsensusStatistics() = runBlocking {
        // Step 1: Create event
        val eventResult = eventRepository.createEvent(
            CreateEventRequest("Test Event", "Description", null, null, null, null, null, null, null)
        )
        assertTrue(eventResult.isSuccess)
        val event = eventResult.getOrNull()!!
        val eventId = event.id

        // Step 2: Submit multiple judgments
        val judgment1 = CreateJudgmentRequest(eventId, "true", 0.9, "Strong evidence")
        val judgment2 = CreateJudgmentRequest(eventId, "true", 0.8, "Moderate evidence")
        val judgment3 = CreateJudgmentRequest(eventId, "false", 0.7, "Conflicting evidence")
        val judgment4 = CreateJudgmentRequest(eventId, "uncertain", 0.5, "Insufficient data")

        val result1 = judgmentRepository.submitJudgment(judgment1)
        val result2 = judgmentRepository.submitJudgment(judgment2)
        val result3 = judgmentRepository.submitJudgment(judgment3)
        val result4 = judgmentRepository.submitJudgment(judgment4)

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)
        assertTrue(result4.isSuccess)

        // Step 3: Get statistics
        val statsResult = judgmentRepository.getJudgmentStats(eventId)
        assertTrue(statsResult.isSuccess)
        val stats = statsResult.getOrNull()!!

        // Step 4: Verify consensus calculation
        assertEquals(2, stats.trueCount) // 2 "true" judgments
        assertEquals(1, stats.falseCount) // 1 "false" judgment
        assertEquals(1, stats.uncertainCount) // 1 "uncertain" judgment
        
        // Average confidence: (0.9 + 0.8 + 0.7 + 0.5) / 4 = 0.725
        val expectedAvg = (0.9 + 0.8 + 0.7 + 0.5) / 4.0
        assertEquals(expectedAvg, stats.avgConfidence, 0.01)

        // Step 5: Verify judgments are saved
        val judgments = judgmentRepository.listJudgmentsForEvent(eventId, 100, 0)
        assertEquals(4, judgments.size)
    }
}

