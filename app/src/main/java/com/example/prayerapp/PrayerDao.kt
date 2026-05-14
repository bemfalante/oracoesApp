package com.example.prayerapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayers WHERE category = :category ORDER BY position ASC")
    fun getPrayersByCategory(category: String): LiveData<List<Prayer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayer: Prayer)

    @Update
    suspend fun update(prayer: Prayer)

    @Update
    suspend fun updateAll(prayers: List<Prayer>)

    @Delete
    suspend fun delete(prayer: Prayer)
}
