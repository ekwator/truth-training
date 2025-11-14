package com.truth.training.client.performance

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.truth.training.client.MainActivity
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.testing.TestDataSeeder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UIResponseTimeTest {
    data class UIBenchmark(
        val operation: String,
        val averageTime: Long,
        val minTime: Long,
        val maxTime: Long
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() = runBlocking {
        TruthDatabase.closeInstance()
        context.deleteDatabase(TruthDatabase.DATABASE_NAME)
        val db = TruthDatabase.getInstance(context)
        TestDataSeeder.seedKnowledgeBase(db)
    }

    @After
    fun tearDown() {
        if (this::scenario.isInitialized) {
            scenario.close()
        }
        TruthDatabase.closeInstance()
    }

    private fun waitForIdle(scenario: ActivityScenario<MainActivity>) {
        instrumentation.waitForIdleSync()
        repeat(50) {
            var hasFocus = false
            scenario.onActivity { activity ->
                hasFocus = activity.window?.decorView?.hasWindowFocus() == true
            }
            if (hasFocus) return
            Thread.sleep(100)
        }
        throw AssertionError("Window never gained focus within timeout")
    }

    private fun measure(operation: String, iterations: Int, block: () -> Long): UIBenchmark {
        val measurements = mutableListOf<Long>()
        repeat(iterations) {
            measurements += block()
        }
        val avg = measurements.average().toLong()
        val min = measurements.minOrNull() ?: 0L
        val max = measurements.maxOrNull() ?: 0L
        return UIBenchmark(operation, avg, min, max)
    }

    private fun launchColdStart(): Long {
        val start = SystemClock.elapsedRealtime()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForIdle(scenario)
        }
        return SystemClock.elapsedRealtime() - start
    }

    private fun prepareEvents(count: Int) = runBlocking {
        val db = TruthDatabase.getInstance(context)
        val eventDao = db.eventDao()
        db.clearAllTables()
        TestDataSeeder.seedKnowledgeBase(db)
        val now = System.currentTimeMillis()
        repeat(count) { index ->
            eventDao.insertEvent(
                EventEntity(
                    description = "Event $index",
                    categoryId = (index % 5) + 1,
                    formaId = null,
                    causeId = null,
                    developId = null,
                    effectId = null,
                    vector = index % 2 == 0,
                    detected = null,
                    corrected = false,
                    timestampStart = now + index,
                    timestampEnd = null,
                    code = 1,
                    collectiveScore = null
                )
            )
        }
    }

    @Test
    fun benchmarkScreenRenderingColdStart() {
        val result = measure("screen_rendering_cold_start", 3) { launchColdStart() }
        assertTrue("Cold start should be < 5000ms, was ${result.averageTime}ms", result.averageTime < 5_000)
    }

    @Test
    fun benchmarkScreenRenderingWarmStart() {
        launchColdStart() // Warm-up
        val result = measure("screen_rendering_warm_start", 3) { launchColdStart() }
        assertTrue("Warm start should be < 3500ms, was ${result.averageTime}ms", result.averageTime < 3_500)
    }

    @Test
    fun benchmarkDataLoadingWithLargeDataset() {
        prepareEvents(200)
        val result = measure("data_loading_large_dataset", 3) { launchColdStart() }
        assertTrue("Data loading should be < 5500ms, was ${result.averageTime}ms", result.averageTime < 5_500)
    }

    @Test
    fun benchmarkUserInteractionButtonClick() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForIdle(scenario)
        val result = measure("user_interaction_tap", 10) {
            val start = SystemClock.elapsedRealtime()
            val centerX = device.displayWidth / 2
            val centerY = device.displayHeight / 2
            device.click(centerX, centerY)
            instrumentation.waitForIdleSync()
            SystemClock.elapsedRealtime() - start
        }
        assertTrue("Tap interaction should be < 1000ms, was ${result.averageTime}ms", result.averageTime < 1_000)
    }

    @Test
    fun benchmarkNavigationTransition() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForIdle(scenario)
        val result = measure("navigation_recreate", 5) {
            val start = SystemClock.elapsedRealtime()
            scenario.recreate()
            waitForIdle(scenario)
            SystemClock.elapsedRealtime() - start
        }
        assertTrue("Recreation should be < 2000ms, was ${result.averageTime}ms", result.averageTime < 2_000)
    }
}

