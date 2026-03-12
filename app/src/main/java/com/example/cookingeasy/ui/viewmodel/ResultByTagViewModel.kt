package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResultByTagViewModel(): ViewModel() {
    private val recipeRepository = RecipeRepositoryImp()
    private val _recipesByArea: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val recipeByArea: StateFlow<List<Recipe>> = _recipesByArea

    fun getRecipesByArea(tag: String) {
        viewModelScope.launch {
            try {
                _recipesByArea.value = recipeRepository.filterRecipesByArea(tag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}