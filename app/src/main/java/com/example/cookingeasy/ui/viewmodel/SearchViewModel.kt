package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.HistorySearch
import com.example.cookingeasy.domain.model.Recipe
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SearchViewModel(): ViewModel() {
    private val recipeRepository = RecipeRepositoryImp()
    private val _searchResult: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val searchResult: StateFlow<List<Recipe>> = _searchResult
    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun createTimestamp(dateStr: String): Timestamp {
        val format = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        val date = format.parse(dateStr)
        return Timestamp(date!!)
    }

    val mockHistory = mutableListOf<HistorySearch>(
        HistorySearch("1", "user_001", "Phở bò", "10:30 23/04/2026"),
        HistorySearch("2", "user_001", "Bún chả", "12:00 23/04/2026"),
        HistorySearch("3", "user_002", "Cơm tấm", "18:45 22/04/2026"),
        HistorySearch("4", "user_003", "Trà sữa", "09:15 21/04/2026")
    )

    fun searchRecipes(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = recipeRepository.filterRecipesBySearch(query)
                _searchResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun mockData(): List<HistorySearch> {
        return mockHistory
    }
}