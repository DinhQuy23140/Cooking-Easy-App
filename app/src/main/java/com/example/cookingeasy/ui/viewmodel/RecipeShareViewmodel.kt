package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.cookingeasy.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow

class RecipeShareViewmodel : ViewModel() {
    private val _selecetRecipe: MutableStateFlow<Recipe?> = MutableStateFlow<Recipe?>(null)
    val selectRecipe = _selecetRecipe

    fun selectedRecipe(recipe: Recipe) {
        _selecetRecipe.value = recipe
    }
}