package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ResultByCategoryViewModel(): ViewModel() {
    private val recipeRepository = RecipeRepositoryImp()
    private val _recipesByCategory: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val recipesByCategory = _recipesByCategory

    fun getRecipesByCategory(category: String) {
        viewModelScope.launch {
            try {
                val recipes = recipeRepository.filterRecipesByCategory(category)
                _recipesByCategory.value = recipes
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}