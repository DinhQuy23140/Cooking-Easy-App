package com.example.cookingeasy.data.repository

import android.util.Log
import com.example.cookingeasy.data.remote.api.ApiServiceProvider
import com.example.cookingeasy.data.remote.dto.AreaResponseDto
import com.example.cookingeasy.data.remote.dto.CategoryResponseDto
import com.example.cookingeasy.data.remote.dto.RecipeResponseDto
import com.example.cookingeasy.data.remote.mapper.AreaMapper
import com.example.cookingeasy.data.remote.mapper.CategoryMapper
import com.example.cookingeasy.data.remote.mapper.RecipeMapper
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class RecipeRepositoryImp : RecipeRepository{

    val recipeService = ApiServiceProvider.recipeService
    override suspend fun getCategories(): List<Category> {
        val response: CategoryResponseDto = recipeService.getCategories()
        return CategoryMapper.mapToCategoryList(response.categories)
    }

    override suspend fun getAreas(): List<Area> {
        val response: AreaResponseDto = recipeService.getArea()
        return AreaMapper.toListAre(response)
    }

    override suspend fun getRecipes(): List<Recipe> {
        val allRecipes = mutableListOf<Recipe>()
        val alphabet = ('a'..'z').toList()

        for (char in alphabet) {
            try {
                val response = recipeService.getRecipes(char.toString())
                response.meals?.let { meals ->
                    allRecipes.addAll(RecipeMapper.toRecipeList(meals))
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching letter $char: ${e.message}")
            }
        }
        return allRecipes
    }

    override fun getRecipesFlow(): Flow<List<Recipe>> = flow {
        val allRecipes = mutableListOf<Recipe>()
        val alphabet = ('a'..'z').toList()

        for (char in alphabet) {
            try {
                val response = recipeService.getRecipes(char.toString())
                response.meals?.let { meals ->
                    allRecipes.addAll(RecipeMapper.toRecipeList(meals))
                    emit(allRecipes.toList()) // ← emit sau mỗi chữ cái
                }
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error fetching letter $char: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun filterRecipesByArea(are: String): List<Recipe> {
        val response: RecipeResponseDto = recipeService.filterRecipesByArea(are)
        return RecipeMapper.toRecipeList(response.meals)
    }

    override suspend fun filterRecipesByCategory(category: String): List<Recipe> {
        val response: RecipeResponseDto = recipeService.filterRecipesByCategory(category)
        return RecipeMapper.toRecipeList(response.meals)
    }

    override suspend fun filterRecipesByIngredient(ingredient: String): List<Recipe> {
        TODO("Not yet implemented")
    }
}