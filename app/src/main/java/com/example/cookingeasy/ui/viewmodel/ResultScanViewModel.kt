package com.example.cookingeasy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ResultScanViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    private val _recipeByIngredients = MutableStateFlow<List<Recipe>>(emptyList())
    val recipeByIngredients: StateFlow<List<Recipe>> = _recipeByIngredients

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun getRecipesByIngredients(ingredients: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recipeRepository.getRecipesFlow()
                    .collect { allRecipes ->
                        _recipeByIngredients.value = allRecipes.filter { recipe ->
                            recipe.containsAnyIgnoreCase(ingredients)
                        }
                    }
            } catch (e: Exception) {
                Log.e("ResultScanViewModel", "Error: ${e.message}")
                _recipeByIngredients.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}