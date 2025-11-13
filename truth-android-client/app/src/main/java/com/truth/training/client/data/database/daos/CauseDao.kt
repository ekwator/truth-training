package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.CauseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CauseDao {
    @Query("SELECT * FROM cause ORDER BY name ASC")
    suspend fun listCauses(): List<CauseEntity>
    
    @Query("SELECT * FROM cause ORDER BY name ASC")
    fun listCausesFlow(): Flow<List<CauseEntity>>
    
    @Query("SELECT * FROM cause WHERE id = :id")
    suspend fun getCauseById(id: Int): CauseEntity?
    
    @Query("SELECT * FROM cause WHERE name = :name")
    suspend fun getCauseByName(name: String): CauseEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCause(cause: CauseEntity)
    
    @Update
    suspend fun updateCause(cause: CauseEntity)
    
    @Delete
    suspend fun deleteCause(cause: CauseEntity)
    
    @Query("SELECT COUNT(*) FROM cause")
    suspend fun getCauseCount(): Int
}

