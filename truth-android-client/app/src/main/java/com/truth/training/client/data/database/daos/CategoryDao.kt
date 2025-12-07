package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY name ASC")
    suspend fun listCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM category ORDER BY name ASC")
    fun listCategoriesFlow(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?
    
    @Query("SELECT * FROM category WHERE name = :name")
    suspend fun getCategoryByName(name: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("SELECT COUNT(*) FROM category")
    suspend fun getCategoryCount(): Int
    
    // Clear all categories (used when changing language)
    @Query("DELETE FROM category")
    suspend fun clearAllCategories()
}

