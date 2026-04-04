package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoriteViewModel(): ViewModel() {
    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val recipeRepository: RecipeRepository = RecipeRepositoryImp()

    private val _favoriteRecipes: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val favoriteRecipes: StateFlow<List<Recipe>> = _favoriteRecipes

    fun getFavoriteRecipes() {
        viewModelScope.launch {
            _favoriteRecipes.value = recipeRepository.getFavRecipeFirebase()
        }
    }
}