package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.ImpactTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImpactTypeDao {
    @Query("SELECT * FROM impact_type ORDER BY name ASC")
    suspend fun listImpactTypes(): List<ImpactTypeEntity>
    
    @Query("SELECT * FROM impact_type ORDER BY name ASC")
    fun listImpactTypesFlow(): Flow<List<ImpactTypeEntity>>
    
    @Query("SELECT * FROM impact_type WHERE id = :id")
    suspend fun getImpactTypeById(id: Int): ImpactTypeEntity?
    
    @Query("SELECT * FROM impact_type WHERE name = :name")
    suspend fun getImpactTypeByName(name: String): ImpactTypeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImpactType(impactType: ImpactTypeEntity)
    
    @Update
    suspend fun updateImpactType(impactType: ImpactTypeEntity)
    
    @Delete
    suspend fun deleteImpactType(impactType: ImpactTypeEntity)
    
    @Query("SELECT COUNT(*) FROM impact_type")
    suspend fun getImpactTypeCount(): Int
}

