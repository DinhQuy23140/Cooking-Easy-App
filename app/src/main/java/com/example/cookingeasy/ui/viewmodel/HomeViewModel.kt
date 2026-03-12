package com.example.cookingeasy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel() : ViewModel(){
    private val recipeRepository: RecipeRepository = RecipeRepositoryImp()
    private val _listArea: MutableStateFlow<List<Area>> = MutableStateFlow<List<Area>>(emptyList())
    private val _listCategory: MutableStateFlow<List<Category>> =
        MutableStateFlow<List<Category>>(emptyList())
    private val _listRecipe: MutableStateFlow<List<Recipe>> =
        MutableStateFlow<List<Recipe>>(emptyList())
    val lisCategory: StateFlow<List<Category>> = _listCategory
    val listArea: StateFlow<List<Area>> = _listArea
    val listRecipe: StateFlow<List<Recipe>> = _listRecipe

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
                    _listRecipe.value = recipes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}