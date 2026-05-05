package com.example.cookingeasy.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.stream.Collector
import java.util.stream.Collectors
import kotlin.ranges.contains

class ExploreViewModel(): ViewModel() {
    private val _recipeRepository = RecipeRepositoryImp()
    private val authRepository: AuthRepositoryImp = AuthRepositoryImp()
    private val _randomRecipe: MutableStateFlow<Recipe?> = MutableStateFlow(null)
    private val _categories: MutableStateFlow<List<Category>> = MutableStateFlow(emptyList())
    private val _areas: MutableStateFlow<List<Area>> = MutableStateFlow(emptyList())
    private val _trendingRecipes: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())

    val randomRecipe: StateFlow<Recipe?> = _randomRecipe
    val categories: StateFlow<List<Category>> = _categories
    val areas: StateFlow<List<Area>> = _areas
    val trendingRecipes: StateFlow<List<Recipe>> = _trendingRecipes

    fun getRandomRecipe() {
        viewModelScope.launch { loadRandomRecipe() }
    }

    /** Dùng khi UI cần chờ xong (ví dụ nút refresh + progress chồng icon). */
    suspend fun loadRandomRecipe() {
        try {
            val r = _recipeRepository.getRandomRecipe() ?: return
            _randomRecipe.value = r
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCategories() {
        viewModelScope.launch {
            try {
                _categories.value = _recipeRepository.getCategories()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAreas() {
        viewModelScope.launch {
            try {
                _areas.value = _recipeRepository.getAreas()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getTrending() {
        viewModelScope.launch {
            combine(
                _recipeRepository.getTrendingRecipe(),
                flow { emit(getFavoriteRecipesIds()) }
            ) { trending, favorites ->

                val favIds = favorites.map { it }.toSet()

                trending.map {
                    it.copy(isFavorote = favIds.contains(it.idMeal.toString()))
                }

            }.collect { updatedList ->
                _trendingRecipes.value = updatedList
            }
        }
    }

    suspend fun getFavoriteRecipesIds(): List<String> {
        return _recipeRepository.getFavRecipeIds()
    }

    fun caculatorColumn(context: Context): Int {
        val disPlayMetrics = context.resources.displayMetrics
        val screenDisplay = disPlayMetrics.widthPixels / disPlayMetrics.density
        return if (screenDisplay < 500) 2
        else if (screenDisplay in 500.0..<700.0) 3
        else 4
    }

}