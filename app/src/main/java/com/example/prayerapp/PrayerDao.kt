package com.example.prayerapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayers WHERE categoryId = :categoryId ORDER BY position ASC")
    fun getPrayersByCategory(categoryId: Int): LiveData<List<Prayer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayer: Prayer)

    @Update
    suspend fun update(prayer: Prayer)

    @Update
    suspend fun updateAll(prayers: List<Prayer>)

    @Delete
    suspend fun delete(prayer: Prayer)

    @Query("DELETE FROM prayers WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Int)
}
