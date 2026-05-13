package com.example.prayerapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayers ORDER BY id DESC")
    fun getAllPrayers(): LiveData<List<Prayer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayer: Prayer)

    @Update
    suspend fun update(prayer: Prayer)

    @Delete
    suspend fun delete(prayer: Prayer)
}
