package com.example.cookingeasy.ui.viewmodel

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val recipeRepository: RecipeRepository = RecipeRepositoryImp()

    private val _listArea = MutableStateFlow<List<Area>>(emptyList())
    private val _listCategory = MutableStateFlow<List<Category>>(emptyList())
    private val _listRecipe = MutableStateFlow<List<Recipe>>(emptyList())
    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    private val _favoriteError = MutableSharedFlow<Recipe>()
    private val _isFavoritesReady = MutableStateFlow(false)

    val lisCategory: StateFlow<List<Category>> = _listCategory
    val listArea: StateFlow<List<Area>> = _listArea
    val listRecipe: StateFlow<List<Recipe>> = _listRecipe

    // ─────────────────────────────────────────────
    // Load data
    // ─────────────────────────────────────────────

    fun getListCategory() {
        viewModelScope.launch {
            try {
                _listCategory.value = recipeRepository.getCategories()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getListArea() {
        viewModelScope.launch {
            try {
                _listArea.value = recipeRepository.getAreas()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getRecipes() {
        viewModelScope.launch {
            try {
                recipeRepository.getRecipesFlow().collect { recipes ->
                    val ids = _favoriteIds.value.toSet()
                    _listRecipe.value = recipes.map { recipe ->
                        recipe.copy(isFavorote = ids.contains(recipe.idMeal.toString()))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ─────────────────────────────────────────────
    // Favorites
    // ─────────────────────────────────────────────

    private suspend fun loadFavoritesSync() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        try {
            val recipes = recipeRepository.getFavoriteRecipes(uid)
            _favoriteIds.value = recipes.map { it.idMeal.toString() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            combine(
                recipeRepository.getRecipesFlow(),
                flow { emit(getFavRecipeIds()) }
            ) { recipes, favorites ->

                val favIds = favorites.map { it }.toSet()
                Log.d("Fav recipe: ", favIds.toString())
                recipes.map {
                    it.copy(isFavorote = favIds.contains(it.idMeal.toString()))
                }

            }.collect { updatedList ->
                _listRecipe.value = updatedList
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
                _favoriteError.emit(recipe)
            }
        }
    }

    suspend fun getFavRecipeIds(): List<String> {
        return recipeRepository.getFavRecipeIds()
    }

    fun caculatorColumn(context: Context): Int {
        val disPlayMetrics = context.resources.displayMetrics
        val screenDisplay = disPlayMetrics.widthPixels / disPlayMetrics.density
        return if (screenDisplay < 500) 2
        else if (screenDisplay in 500.0..<700.0) 3
        else 4
    }
}