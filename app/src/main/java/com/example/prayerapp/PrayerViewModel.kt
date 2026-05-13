package com.example.prayerapp

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class PrayerViewModel(private val dao: PrayerDao) : ViewModel() {

    val allPrayers: LiveData<List<Prayer>> = dao.getAllPrayers()

    fun insert(prayer: Prayer) = viewModelScope.launch {
        dao.insert(prayer)
    }

    fun update(prayer: Prayer) = viewModelScope.launch {
        dao.update(prayer)
    }

    fun delete(prayer: Prayer) = viewModelScope.launch {
        dao.delete(prayer)
    }
}

class PrayerViewModelFactory(private val dao: PrayerDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrayerViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
