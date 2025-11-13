package com.truth.training.client.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truth.training.client.data.database.entities.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE event_id = :eventId")
    suspend fun getSummaryForEvent(eventId: Long): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE event_id = :eventId")
    fun getSummaryForEventFlow(eventId: Long): Flow<SummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: SummaryEntity)
}

