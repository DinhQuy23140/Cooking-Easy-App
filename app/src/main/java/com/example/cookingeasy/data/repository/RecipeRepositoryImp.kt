package com.example.cookingeasy.data.repository

import android.util.Log
import com.example.cookingeasy.data.remote.api.ApiServiceProvider
import com.example.cookingeasy.data.remote.dto.AreaResponseDto
import com.example.cookingeasy.data.remote.dto.CategoryResponseDto
import com.example.cookingeasy.data.remote.dto.RecipeResponseDto
import com.example.cookingeasy.data.remote.firebase.RecipeFirestoreDataSource
import com.example.cookingeasy.data.remote.mapper.AreaMapper
import com.example.cookingeasy.data.remote.mapper.CategoryMapper
import com.example.cookingeasy.data.remote.mapper.RecipeMapper
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.supervisorScope

class RecipeRepositoryImp : RecipeRepository{

    val recipeService = ApiServiceProvider.recipeService
    val remote = RecipeFirestoreDataSource()
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

    override suspend fun filterRecipesBySearch(query: String): List<Recipe> {
        val response = recipeService.filterRecipesBySearch(query)
        return RecipeMapper.toRecipeList(response.meals ?: emptyList())
    }

    override suspend fun getRandomRecipe(): Recipe? {
        val response = recipeService.getRandomRecipe()
        val dto = response.meals?.firstOrNull() ?: return null
        return RecipeMapper.toRecipe(dto)
    }

    override fun getTrendingRecipe(): Flow<List<Recipe>> = flow {
        val trendingRecipes = mutableListOf<Recipe>()
        for (index in 1 .. 10) {
            try {
                val response = recipeService.getRandomRecipe()
                response.meals?.let {
                    trendingRecipes.add(RecipeMapper.toRecipe(response.meals.first()))
                    emit(trendingRecipes.toList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getFavoriteRecipes(uid: String): List<Recipe> = supervisorScope {

        val recipesDeferred = async(Dispatchers.IO) {
            runCatching { getRecipes() }.getOrDefault(emptyList())
        }

        val favoriteIdsDeferred = async(Dispatchers.IO) {
            runCatching { remote.getFavoriteIds(uid) }.getOrDefault(emptyList())
        }

        val recipes = recipesDeferred.await()
        val favoriteIds = favoriteIdsDeferred.await().toSet()

        return@supervisorScope recipes.filter {
            it.idMeal.toString() in favoriteIds
        }
    }

    override suspend fun toggleFavorite(uid: String, recipe: Recipe) {
        val isFav = remote.isFavorite(uid, recipe.idMeal.toString())

        if (isFav) {
            remote.removeFavorite(uid, recipe.idMeal.toString())
        } else {
            remote.addFavorite(uid, recipe)
        }
    }

    override suspend fun isFavorite(uid: String, recipeId: String): Boolean {
        return remote.isFavorite(uid, recipeId)
    }
}