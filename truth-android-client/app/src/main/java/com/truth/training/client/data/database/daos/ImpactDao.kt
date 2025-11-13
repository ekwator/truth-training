package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.ImpactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImpactDao {
    @Query("SELECT * FROM impact WHERE event_id = :eventId ORDER BY created_at DESC")
    suspend fun listImpactsForEvent(eventId: Long): List<ImpactEntity>

    @Query("SELECT * FROM impact WHERE event_id = :eventId ORDER BY created_at DESC")
    fun listImpactsForEventFlow(eventId: Long): Flow<List<ImpactEntity>>

    @Query("SELECT * FROM impact WHERE id = :id")
    suspend fun getImpactById(id: Long): ImpactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImpact(impact: ImpactEntity): Long

    @Update
    suspend fun updateImpact(impact: ImpactEntity)

    @Delete
    suspend fun deleteImpact(impact: ImpactEntity)
}

