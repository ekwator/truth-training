package com.truth.training.client.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import com.truth.training.client.data.repository.EventRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventCreationWithTemplateTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventRepository: EventRepository

    @Before
    fun setup() {
        database = TruthDatabase.getInstance(ApplicationProvider.getApplicationContext())
        eventRepository = EventRepository(database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createEventPrefillsContext() = runBlocking {
        val context = ContextTemplateEntity(
            name = "Template",
            description = "Template Description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        database.contextTemplateDao().insertTemplate(context)

        val event = eventRepository.createEvent(
            com.truth.training.client.data.network.dto.CreateEventRequest(
                description = "New Training Event",
                categoryId = 1,
                timestampStart = 1_000L
            )
        ).getOrThrow()

        val savedEvent = eventRepository.getEventById(event.id)
        assertNotNull(savedEvent)
        assertEquals(event.description, savedEvent!!.description)
    }
}

