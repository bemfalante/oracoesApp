package com.example.prayerapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface StoicDao {
    @Query("SELECT * FROM stoic_meditations WHERE month = :month AND day = :day LIMIT 1")
    fun getMeditation(month: Int, day: Int): LiveData<StoicMeditation?>

    @Query("SELECT * FROM stoic_meditations WHERE month = :month")
    fun getMeditationsByMonth(month: Int): LiveData<List<StoicMeditation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meditations: List<StoicMeditation>)

    @Update
    suspend fun update(meditation: StoicMeditation)

    @Query("SELECT COUNT(*) FROM stoic_meditations")
    suspend fun getCount(): Int
}
