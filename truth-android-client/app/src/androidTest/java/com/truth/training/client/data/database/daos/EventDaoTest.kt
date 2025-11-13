package com.truth.training.client.data.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDaoTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventDao: EventDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
        eventDao = database.eventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetEventById() = runBlocking {
        val id = eventDao.insertEvent(testEvent(description = "Test Event"))
        val retrieved = eventDao.getEventById(id)
        assertNotNull(retrieved)
        assertEquals("Test Event", retrieved!!.description)
    }

    @Test
    fun getEventByIdFlowEmitsInsertedEntity() = runBlocking {
        val id = eventDao.insertEvent(testEvent(description = "Flow Event"))
        val entity = eventDao.getEventByIdFlow(id).first()
        assertEquals("Flow Event", entity?.description)
    }

    @Test
    fun listEventsRespectsPagination() = runBlocking {
        repeat(10) { index ->
            eventDao.insertEvent(
                testEvent(
                    description = "Event $index",
                    timestampStart = 1_000L + index
                )
            )
        }

        val firstPage = eventDao.listEvents(limit = 4, offset = 0)
        val secondPage = eventDao.listEvents(limit = 4, offset = 4)
        assertEquals(4, firstPage.size)
        assertEquals(4, secondPage.size)
    }

    @Test
    fun updateEventPersistsChanges() = runBlocking {
        val id = eventDao.insertEvent(testEvent(description = "Original", detected = null))
        val existing = eventDao.getEventById(id)!!
        val updated = existing.copy(description = "Updated", detected = true)

        eventDao.updateEvent(updated)
        val reloaded = eventDao.getEventById(id)!!
        assertEquals("Updated", reloaded.description)
        assertTrue(reloaded.detected!!)
    }

    @Test
    fun deleteEventRemovesRow() = runBlocking {
        val id = eventDao.insertEvent(testEvent(description = "To delete"))
        val entity = eventDao.getEventById(id)!!
        eventDao.deleteEvent(entity)
        assertNull(eventDao.getEventById(id))
    }

    @Test
    fun countMatchesInsertedRows() = runBlocking {
        repeat(3) {
            eventDao.insertEvent(testEvent(description = "Event $it"))
        }
        assertEquals(3, eventDao.getEventCount())
    }

    @Test
    fun allEventsFlowEmitsLatestSnapshot() = runBlocking {
        repeat(2) {
            eventDao.insertEvent(testEvent(description = "Event $it"))
        }
        val snapshot = eventDao.getAllEventsFlow().first()
        assertEquals(2, snapshot.size)
    }

    private fun testEvent(
        description: String,
        categoryId: Int? = null,
        formaId: Int? = null,
        causeId: Int? = null,
        developId: Int? = null,
        effectId: Int? = null,
        vector: Boolean = true,
        detected: Boolean? = null,
        corrected: Boolean = false,
        timestampStart: Long = System.currentTimeMillis(),
        timestampEnd: Long? = null,
        code: Int = 1,
        collectiveScore: Double? = null
    ): EventEntity = EventEntity(
        description = description,
        categoryId = categoryId,
        formaId = formaId,
        causeId = causeId,
        developId = developId,
        effectId = effectId,
        vector = vector,
        detected = detected,
        corrected = corrected,
        timestampStart = timestampStart,
        timestampEnd = timestampEnd,
        code = code,
        collectiveScore = collectiveScore
    )
}

