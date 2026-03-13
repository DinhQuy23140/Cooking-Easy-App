package com.example.cookingeasy.domain.repository

import com.example.cookingeasy.data.remote.dto.CategoryResponseDto
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    suspend fun getCategories(): List<Category>

    suspend fun getAreas(): List<Area>

    suspend fun getRecipes(): List<Recipe>
    fun getRecipesFlow(): Flow<List<Recipe>>

    suspend fun filterRecipesByArea(are: String): List<Recipe>

    suspend fun filterRecipesByCategory(category: String): List<Recipe>

    suspend fun filterRecipesByIngredient(ingredient: String): List<Recipe>

    suspend fun filterRecipesBySearch(query: String): List<Recipe>
}