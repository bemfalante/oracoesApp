package com.example.prayerapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position ASC")
    fun getAllCategories(): LiveData<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Update
    suspend fun updateAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories ORDER BY position ASC")
    suspend fun getAllCategoriesList(): List<Category>
}
