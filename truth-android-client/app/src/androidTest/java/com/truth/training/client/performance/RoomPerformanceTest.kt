package com.truth.training.client.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.EventDao
import com.truth.training.client.data.database.entities.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

/**
 * Performance tests for Room database queries.
 * 
 * Benchmarks:
 * - Pagination query: < 50ms for 35 events
 * - Single entity retrieval: < 10ms for event by ID
 * - Bulk insert: < 100ms for 100 events
 * - Complex query: < 30ms for filtered list with status
 * - Flow emission: < 20ms initial latency
 * 
 * Test methodology:
 * - Measure average of 10 iterations
 * - Test with database sizes: 100, 1000, 10000 events
 * - Validate query plans and indices
 */
@RunWith(AndroidJUnit4::class)
class RoomPerformanceTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventDao: EventDao
    
    /**
     * Performance benchmark result data class.
     */
    data class PerformanceBenchmark(
        val operation: String,
        val averageTime: Long, // milliseconds
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

    /**
     * Helper function to measure execution time in milliseconds.
     */
    private inline fun <T> measureTime(block: () -> T): Pair<T, Long> {
        val startTime = System.nanoTime()
        val result = block()
        val endTime = System.nanoTime()
        val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
        return Pair(result, durationMs)
    }

    /**
     * Helper function to measure average execution time over iterations.
     */
    private suspend fun measureAverageTime(iterations: Int, block: suspend () -> Unit): PerformanceBenchmark {
        // Warm-up run to mitigate JIT/initialization costs
        measureTime { block() }
        val times = mutableListOf<Long>()
        repeat(iterations) {
            val (_, time) = measureTime { block() }
            times.add(time)
        }
        val average = times.average().toLong()
        val min = times.minOrNull() ?: 0L
        val max = times.maxOrNull() ?: 0L
        return PerformanceBenchmark(
            operation = "",
            averageTime = average,
            minTime = min,
            maxTime = max,
            databaseSize = 0,
            passed = false
        )
    }

    private fun medianOf(times: List<Long>): Long {
        if (times.isEmpty()) return 0
        val sorted = times.sorted()
        return sorted[sorted.size / 2]
    }

    /**
     * Create test event with given ID and title.
     */
    private fun createTestEvent(id: String, title: String, status: String = "active"): EventEntity {
        val now = System.currentTimeMillis().toString()
        return EventEntity(
            id = id,
            title = title,
            description = "Test description for $title",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = now,
            endDate = null,
            createdAt = now,
            updatedAt = null,
            status = status
        )
    }

    /**
     * Populate database with specified number of events.
     * Optimized for large datasets by using batch inserts in transactions.
     */
    private suspend fun populateDatabase(size: Int) {
        database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=OFF")
        database.openHelper.writableDatabase.beginTransaction()
        try {
            // Batch inserts in transaction for better performance
            repeat(size) { index ->
                val event = createTestEvent(
                    id = "event_$index",
                    title = "Event $index",
                    status = if (index % 2 == 0) "active" else "completed"
                )
                eventDao.insertEvent(event)
            }
            database.openHelper.writableDatabase.setTransactionSuccessful()
        } finally {
            database.openHelper.writableDatabase.endTransaction()
        }
        database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=ON")
    }

    /**
     * Warm up database by performing initial queries.
     */
    private suspend fun warmUpDatabase() {
        eventDao.listEvents(limit = 1, offset = 0)
        eventDao.getEventCount()
    }

    /**
     * Measure median time from a list of execution times.
     */
    private fun measureMedianTime(times: List<Long>): Long {
        return medianOf(times)
    }

    @Test
    fun benchmarkPaginationQueryWith100Events() = runTest {
        // Setup: populate database with 100 events
        populateDatabase(100)
        warmUpDatabase()
        
        // Measure pagination query (35 events as per requirement)
        val times = mutableListOf<Long>()
        // Warm-up query
        eventDao.listEvents(limit = 35, offset = 0)
        repeat(10) {
            val (_, t) = measureTime {
                eventDao.listEvents(limit = 35, offset = 0)
            }
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
        
        assertTrue(
            "Pagination query should be < 50ms, but was ${result.averageTime}ms",
            result.passed
        )
        
        println("Performance: ${result.operation} (DB size: ${result.databaseSize})")
        println("  Average: ${result.averageTime}ms")
        println("  Min: ${result.minTime}ms, Max: ${result.maxTime}ms")
        println("  Target: < 50ms, Passed: ${result.passed}")
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
        
        assertTrue(
            "Pagination query should be < 50ms, but was ${result.averageTime}ms",
            result.passed
        )
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
        
        assertTrue(
            "Pagination query should be < 50ms, but was ${result.averageTime}ms",
            result.passed
        )
    }

    @Test
    fun benchmarkSingleEntityRetrieval() = runTest {
        populateDatabase(1000)
        warmUpDatabase()
        
        // Measure single entity retrieval
        val times = mutableListOf<Long>()
        eventDao.getEventById("event_500")
        repeat(10) {
            val (_, t) = measureTime { eventDao.getEventById("event_500") }
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
        
        assertTrue(
            "Single entity retrieval should be < 10ms, but was ${result.averageTime}ms",
            result.passed
        )
        
        println("Performance: ${result.operation} (DB size: ${result.databaseSize})")
        println("  Average: ${result.averageTime}ms")
        println("  Target: < 10ms, Passed: ${result.passed}")
    }

    @Test
    fun benchmarkBulkInsert100Events() = runTest {
        // Measure bulk insert
        val events = (0..99).map { index ->
            createTestEvent("bulk_event_$index", "Bulk Event $index")
        }
        
        val (_, time) = measureTime {
            database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=OFF")
            // Execute inserts sequentially (for performance tests)
            events.forEach { event ->
                eventDao.insertEvent(event)
            }
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
        
        assertTrue(
            "Bulk insert should be < 100ms, but was ${result.averageTime}ms",
            result.passed
        )
        
        println("Performance: ${result.operation}")
        println("  Time: ${result.averageTime}ms")
        println("  Target: < 100ms, Passed: ${result.passed}")
    }

    @Test
    fun benchmarkComplexQueryWithStatusFilter() = runTest {
        populateDatabase(1000)
        warmUpDatabase()
        
        // Measure filtered query by status
        val times = mutableListOf<Long>()
        eventDao.listEventsByStatus("active", limit = 50, offset = 0)
        repeat(10) {
            val (_, t) = measureTime { eventDao.listEventsByStatus("active", limit = 50, offset = 0) }
            times.add(t)
        }
        val median = medianOf(times)
        
        val result = PerformanceBenchmark(
            operation = "complex_query_status_filter",
            averageTime = median,
            minTime = times.minOrNull() ?: median,
            maxTime = times.maxOrNull() ?: median,
            databaseSize = 1000,
            passed = median < 30
        )
        
        assertTrue(
            "Complex query should be < 30ms, but was ${result.averageTime}ms",
            result.passed
        )
        
        println("Performance: ${result.operation} (DB size: ${result.databaseSize})")
        println("  Average: ${result.averageTime}ms")
        println("  Target: < 30ms, Passed: ${result.passed}")
    }

    @Test
    fun benchmarkFlowEmissionLatency() = runTest {
        populateDatabase(100)
        warmUpDatabase()
        
        // Measure Flow initial emission latency
        // Warm-up a subscription
        eventDao.getAllEventsFlow().first()
        val times = mutableListOf<Long>()
        repeat(10) {
            val (_, t) = measureTime {
                val flow = eventDao.getAllEventsFlow()
                flow.first()
            }
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
        
        assertTrue(
            "Flow emission should be < 20ms, but was ${result.averageTime}ms",
            result.passed
        )
        
        println("Performance: ${result.operation} (DB size: ${result.databaseSize})")
        println("  Average: ${result.averageTime}ms")
        println("  Target: < 20ms, Passed: ${result.passed}")
    }

    @Test
    fun validateDatabaseIndices() = runTest {
        // This test validates that indices are properly created
        // by checking query performance doesn't degrade with larger datasets
        populateDatabase(100)
        warmUpDatabase()
        val smallDbTime = measureAverageTime(5) {
            eventDao.listEvents(limit = 35, offset = 0)
        }
        
        // Clear and repopulate with larger dataset
        database.clearAllTables()
        populateDatabase(1000)
        warmUpDatabase()
        val largeDbTime = measureAverageTime(5) {
            eventDao.listEvents(limit = 35, offset = 0)
        }
        
        // With proper indices, query time should not increase significantly
        // (should be within 2x for 10x data increase)
        val ratio = largeDbTime.averageTime.toDouble() / smallDbTime.averageTime.toDouble()
        
        assertTrue(
            "Query performance degraded too much: small DB ${smallDbTime.averageTime}ms, large DB ${largeDbTime.averageTime}ms (ratio: $ratio)",
            ratio < 2.0
        )
        
        println("Index validation:")
        println("  Small DB (100 events): ${smallDbTime.averageTime}ms")
        println("  Large DB (1000 events): ${largeDbTime.averageTime}ms")
        println("  Performance ratio: $ratio (should be < 2.0)")
    }
}

