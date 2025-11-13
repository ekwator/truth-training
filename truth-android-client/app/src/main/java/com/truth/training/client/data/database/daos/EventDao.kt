package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM truth_events ORDER BY timestamp_start DESC LIMIT :limit OFFSET :offset")
    suspend fun listEvents(limit: Int, offset: Int): List<EventEntity>

    @Query("SELECT * FROM truth_events WHERE category_id = :categoryId ORDER BY timestamp_start DESC LIMIT :limit OFFSET :offset")
    suspend fun listEventsByCategory(categoryId: Int, limit: Int, offset: Int): List<EventEntity>

    @Query("SELECT * FROM truth_events WHERE id = :id")
    suspend fun getEventById(id: Long): EventEntity?

    @Query("SELECT * FROM truth_events WHERE id = :id")
    fun getEventByIdFlow(id: Long): Flow<EventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT COUNT(*) FROM truth_events")
    suspend fun getEventCount(): Int

    @Query("SELECT * FROM truth_events ORDER BY timestamp_start DESC LIMIT :limit OFFSET :offset")
    suspend fun listEventsByTimestamp(limit: Int, offset: Int): List<EventEntity>

    @Query("SELECT * FROM truth_events ORDER BY timestamp_start DESC")
    fun getAllEventsFlow(): Flow<List<EventEntity>>
}

