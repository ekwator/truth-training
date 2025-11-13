package com.truth.training.client.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.EventDao
import com.truth.training.client.data.database.entities.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.minutes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Performance tests for Room database queries against canonical schema.
 */
@RunWith(AndroidJUnit4::class)
class RoomPerformanceTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventDao: EventDao
    private val insertedIds = mutableListOf<Long>()

    data class PerformanceBenchmark(
        val operation: String,
        val averageTime: Long,
        val minTime: Long,
        val maxTime: Long,
        val databaseSize: Int,
        val passed: Boolean
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TruthDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        eventDao = database.eventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private inline fun <T> measureTime(block: () -> T): Pair<T, Long> {
        val start = System.nanoTime()
        val result = block()
        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        return result to durationMs
    }

    private fun medianOf(times: List<Long>): Long = if (times.isEmpty()) 0 else times.sorted()[times.size / 2]

    private fun createTestEvent(description: String, categoryId: Int, timestamp: Long, vector: Boolean): EventEntity =
        EventEntity(
            description = description,
            categoryId = categoryId,
            formaId = null,
            causeId = null,
            developId = null,
            effectId = null,
            vector = vector,
            detected = null,
            corrected = false,
            timestampStart = timestamp,
            timestampEnd = null,
            code = 1,
            collectiveScore = null
        )

    private suspend fun populateDatabase(size: Int) {
        insertedIds.clear()
        database.clearAllTables()
        database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=OFF")
        database.openHelper.writableDatabase.beginTransaction()
        try {
            repeat(size) { index ->
                val event = createTestEvent(
                    description = "Event $index",
                    categoryId = (index % 5) + 1,
                    timestamp = 1_000L + index,
                    vector = index % 2 == 0
                )
                val id = eventDao.insertEvent(event)
                insertedIds.add(id)
            }
            database.openHelper.writableDatabase.setTransactionSuccessful()
        } finally {
            database.openHelper.writableDatabase.endTransaction()
            database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private suspend fun warmUpDatabase() {
        eventDao.listEvents(limit = 1, offset = 0)
        eventDao.getEventCount()
    }

    @Test
    fun benchmarkPaginationQueryWith100Events() = runTest {
        populateDatabase(100)
        warmUpDatabase()

        val times = mutableListOf<Long>()
        eventDao.listEvents(limit = 35, offset = 0)
        repeat(10) {
            val (_, t) = measureTime { eventDao.listEvents(limit = 35, offset = 0) }
            times.add(t)
        }
        val median = medianOf(times)

        val result = PerformanceBenchmark(
            operation = "pagination_query_35_events",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 100,
            passed = median < 50
        )

        assertTrue("Pagination query should be < 50ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun benchmarkPaginationQueryWith1000Events() = runTest {
        populateDatabase(1000)
        warmUpDatabase()

        val times = mutableListOf<Long>()
        eventDao.listEvents(limit = 35, offset = 0)
        repeat(10) {
            val (_, t) = measureTime { eventDao.listEvents(limit = 35, offset = 0) }
            times.add(t)
        }
        val median = medianOf(times)

        val result = PerformanceBenchmark(
            operation = "pagination_query_35_events",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 1000,
            passed = median < 50
        )

        assertTrue("Pagination query should be < 50ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun benchmarkPaginationQueryWith10000Events() = runTest(timeout = 5.minutes) {
        populateDatabase(10000)
        warmUpDatabase()

        val times = mutableListOf<Long>()
        eventDao.listEvents(limit = 35, offset = 0)
        repeat(10) {
            val (_, t) = measureTime { eventDao.listEvents(limit = 35, offset = 0) }
            times.add(t)
        }
        val median = medianOf(times)

        val result = PerformanceBenchmark(
            operation = "pagination_query_35_events",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 10000,
            passed = median < 50
        )

        assertTrue("Pagination query should be < 50ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun benchmarkSingleEntityRetrieval() = runTest {
        populateDatabase(1000)
        warmUpDatabase()

        val targetId = insertedIds[500]
        val times = mutableListOf<Long>()
        eventDao.getEventById(targetId)
        repeat(10) {
            val (_, t) = measureTime { eventDao.getEventById(targetId) }
            times.add(t)
        }
        val median = medianOf(times)

        val result = PerformanceBenchmark(
            operation = "single_entity_retrieval",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 1000,
            passed = median < 10
        )

        assertTrue("Single entity retrieval should be < 10ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun benchmarkBulkInsert100Events() = runTest {
        val events = (0 until 100).map { index ->
            createTestEvent(
                description = "Bulk Event $index",
                categoryId = 1,
                timestamp = 2000L + index,
                vector = true
            )
        }

        val (_, time) = measureTime {
            database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=OFF")
            events.forEach { eventDao.insertEvent(it) }
            database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=ON")
        }

        val result = PerformanceBenchmark(
            operation = "bulk_insert_100_events",
            averageTime = time,
            minTime = time,
            maxTime = time,
            databaseSize = 100,
            passed = time < 100
        )

        assertTrue("Bulk insert should be < 100ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun benchmarkCategoryFilterQuery() = runTest {
        populateDatabase(1000)
        warmUpDatabase()

        val times = mutableListOf<Long>()
        eventDao.listEventsByCategory(categoryId = 1, limit = 50, offset = 0)
        repeat(10) {
            val (_, t) = measureTime { eventDao.listEventsByCategory(categoryId = 1, limit = 50, offset = 0) }
            times.add(t)
        }
        val median = medianOf(times)

        val result = PerformanceBenchmark(
            operation = "category_filter_query",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 1000,
            passed = median < 30
        )

        assertTrue("Category filter query should be < 30ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun benchmarkFlowEmissionLatency() = runTest {
        populateDatabase(100)
        warmUpDatabase()

        eventDao.getAllEventsFlow().first()
        val times = mutableListOf<Long>()
        repeat(10) {
            val (_, t) = measureTime { eventDao.getAllEventsFlow().first() }
            times.add(t)
        }
        val median = medianOf(times)

        val result = PerformanceBenchmark(
            operation = "flow_emission_initial",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 100,
            passed = median < 20
        )

        assertTrue("Flow emission should be < 20ms, but was ${result.averageTime}ms", result.passed)
    }

    @Test
    fun validateDatabaseIndices() = runTest {
        populateDatabase(100)
        warmUpDatabase()
        val smallDbTime = measureTime { eventDao.listEvents(limit = 35, offset = 0) }.second

        populateDatabase(1000)
        warmUpDatabase()
        val largeDbTime = measureTime { eventDao.listEvents(limit = 35, offset = 0) }.second

        assertTrue("Indexed query degraded: ${largeDbTime}ms vs ${smallDbTime}ms", largeDbTime <= smallDbTime * 2)
    }
}

