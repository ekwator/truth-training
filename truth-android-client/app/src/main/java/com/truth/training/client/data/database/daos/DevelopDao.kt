package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.DevelopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DevelopDao {
    @Query("SELECT * FROM develop ORDER BY name ASC")
    suspend fun listDevelops(): List<DevelopEntity>
    
    @Query("SELECT * FROM develop ORDER BY name ASC")
    fun listDevelopsFlow(): Flow<List<DevelopEntity>>
    
    @Query("SELECT * FROM develop WHERE id = :id")
    suspend fun getDevelopById(id: Int): DevelopEntity?
    
    @Query("SELECT * FROM develop WHERE name = :name")
    suspend fun getDevelopByName(name: String): DevelopEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevelop(develop: DevelopEntity)
    
    @Update
    suspend fun updateDevelop(develop: DevelopEntity)
    
    @Delete
    suspend fun deleteDevelop(develop: DevelopEntity)
    
    @Query("SELECT COUNT(*) FROM develop")
    suspend fun getDevelopCount(): Int
}

