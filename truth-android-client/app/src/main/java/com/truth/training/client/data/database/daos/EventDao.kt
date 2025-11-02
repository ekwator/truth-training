package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun listEvents(limit: Int, offset: Int): List<EventEntity>
    
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?
    
    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventByIdFlow(id: String): Flow<EventEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Update
    suspend fun updateEvent(event: EventEntity)
    
    @Delete
    suspend fun deleteEvent(event: EventEntity)
    
    @Query("SELECT COUNT(*) FROM events")
    suspend fun getEventCount(): Int
    
    @Query("SELECT * FROM events WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun listEventsByStatus(status: String, limit: Int, offset: Int): List<EventEntity>
    
    @Query("SELECT * FROM events ORDER BY created_at DESC")
    fun getAllEventsFlow(): Flow<List<EventEntity>>
}

