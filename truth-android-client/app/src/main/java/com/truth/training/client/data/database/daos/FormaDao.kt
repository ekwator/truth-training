package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.FormaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormaDao {
    @Query("SELECT * FROM forma ORDER BY name ASC")
    suspend fun listFormas(): List<FormaEntity>
    
    @Query("SELECT * FROM forma ORDER BY name ASC")
    fun listFormasFlow(): Flow<List<FormaEntity>>
    
    @Query("SELECT * FROM forma WHERE id = :id")
    suspend fun getFormaById(id: Int): FormaEntity?
    
    @Query("SELECT * FROM forma WHERE name = :name")
    suspend fun getFormaByName(name: String): FormaEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForma(forma: FormaEntity)
    
    @Update
    suspend fun updateForma(forma: FormaEntity)
    
    @Delete
    suspend fun deleteForma(forma: FormaEntity)
    
    @Query("SELECT COUNT(*) FROM forma")
    suspend fun getFormaCount(): Int
    
    // Clear all formas (used when changing language)
    @Query("DELETE FROM forma")
    suspend fun clearAllFormas()
}

