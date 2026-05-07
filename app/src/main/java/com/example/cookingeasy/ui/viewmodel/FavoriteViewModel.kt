package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FavoriteViewModel @Inject constructor(private val recipeRepository: RecipeRepository): ViewModel() {
    private val _favoriteRecipes: MutableStateFlow<List<Recipe>> = MutableStateFlow(emptyList())
    val favoriteRecipes: StateFlow<List<Recipe>> = _favoriteRecipes

    fun getFavoriteRecipes() {
        viewModelScope.launch {
            _favoriteRecipes.value = recipeRepository.getFavRecipeFirebase()
        }
    }
}