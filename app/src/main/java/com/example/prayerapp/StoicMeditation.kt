package com.example.prayerapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stoic_meditations")
data class StoicMeditation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int, // 1-12
    val day: Int,   // 1-31
    val title: String,
    val quote: String,
    val source: String,
    val commentary: String,
    val isRead: Boolean = false
)
