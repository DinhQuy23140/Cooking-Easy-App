package com.example.cookingeasy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ResultByTagViewModel(): ViewModel() {

    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val recipeRepository = RecipeRepositoryImp()
    private val _recipesByArea: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val recipeByArea: StateFlow<List<Recipe>> = _recipesByArea

    fun getRecipesByArea(tag: String) {
        viewModelScope.launch {
            try {
                combine(
                    flow { emit(recipeRepository.filterRecipesByArea(tag))},
                    flow { emit(getFavRecipeIds()) }
                ) { recipes, favorites ->

                    val favIds = favorites.map { it }.toSet()
                    Log.d("Fav recipe: ", favIds.toString())
                    recipes.map {
                        it.copy(isFavorote = favIds.contains(it.idMeal.toString()))
                    }

                }.collect { updatedList ->
                    _recipesByArea.value = updatedList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                recipeRepository.toggleFavorite(uid, recipe)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Toggle favorite failed: ${e.message}")
            }
        }
    }

    suspend fun getFavRecipeIds(): List<String> {
        return recipeRepository.getFavRecipeIds()
    }
}