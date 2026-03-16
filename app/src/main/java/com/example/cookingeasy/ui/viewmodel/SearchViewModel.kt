package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(): ViewModel() {
    private val recipeRepository = RecipeRepositoryImp()
    private val _searchResult: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val searchResult: StateFlow<List<Recipe>> = _searchResult
    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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
}