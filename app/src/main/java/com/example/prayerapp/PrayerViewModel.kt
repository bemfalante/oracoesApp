package com.example.prayerapp

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class PrayerViewModel(private val prayerDao: PrayerDao, private val categoryDao: CategoryDao, private val stoicDao: StoicDao) : ViewModel() {

    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    fun getPrayersByCategory(categoryId: Int): LiveData<List<Prayer>> = prayerDao.getPrayersByCategory(categoryId)

    // Category operations
    fun insertCategory(category: Category) = viewModelScope.launch {
        categoryDao.insert(category)
    }

    suspend fun insertCategoryWithId(category: Category): Long {
        return categoryDao.insert(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        categoryDao.update(category)
    }

    fun updateAllCategories(categories: List<Category>) = viewModelScope.launch {
        categoryDao.updateAll(categories)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        categoryDao.delete(category)
        prayerDao.deleteByCategoryId(category.id)
    }

    suspend fun getAllCategoriesList(): List<Category> = categoryDao.getAllCategoriesList()

    // Prayer operations
    fun insert(prayer: Prayer) = viewModelScope.launch {
        prayerDao.insert(prayer)
    }

    fun update(prayer: Prayer) = viewModelScope.launch {
        prayerDao.update(prayer)
    }

    fun updateAll(prayers: List<Prayer>) = viewModelScope.launch {
        prayerDao.updateAll(prayers)
    }

    fun delete(prayer: Prayer) = viewModelScope.launch {
        prayerDao.delete(prayer)
    }

    // Stoic operations
    fun getStoicMeditation(month: Int, day: Int) = stoicDao.getMeditation(month, day)
    fun getStoicMeditationsByMonth(month: Int) = stoicDao.getMeditationsByMonth(month)
    fun updateStoicMeditation(meditation: StoicMeditation) = viewModelScope.launch {
        stoicDao.update(meditation)
    }
    suspend fun getStoicCount() = stoicDao.getCount()
    fun insertStoicMeditations(meditations: List<StoicMeditation>) = viewModelScope.launch {
        stoicDao.insertAll(meditations)
    }
}

class PrayerViewModelFactory(private val prayerDao: PrayerDao, private val categoryDao: CategoryDao, private val stoicDao: StoicDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrayerViewModel(prayerDao, categoryDao, stoicDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
