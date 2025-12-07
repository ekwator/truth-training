package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.EffectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EffectDao {
    @Query("SELECT * FROM effect ORDER BY name ASC")
    suspend fun listEffects(): List<EffectEntity>
    
    @Query("SELECT * FROM effect ORDER BY name ASC")
    fun listEffectsFlow(): Flow<List<EffectEntity>>
    
    @Query("SELECT * FROM effect WHERE id = :id")
    suspend fun getEffectById(id: Int): EffectEntity?
    
    @Query("SELECT * FROM effect WHERE name = :name")
    suspend fun getEffectByName(name: String): EffectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffect(effect: EffectEntity)
    
    @Update
    suspend fun updateEffect(effect: EffectEntity)
    
    @Delete
    suspend fun deleteEffect(effect: EffectEntity)
    
    @Query("SELECT COUNT(*) FROM effect")
    suspend fun getEffectCount(): Int
    
    // Clear all effects (used when changing language)
    @Query("DELETE FROM effect")
    suspend fun clearAllEffects()
}

