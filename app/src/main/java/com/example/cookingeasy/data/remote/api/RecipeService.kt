package com.example.cookingeasy.data.remote.api

import com.example.cookingeasy.data.remote.dto.AreaResponseDto
import com.example.cookingeasy.data.remote.dto.CategoryResponseDto
import com.example.cookingeasy.data.remote.dto.RecipeResponseDto
import com.example.cookingeasy.domain.model.Category
import retrofit2.http.GET
import retrofit2.http.Query


interface RecipeService {
    @GET("categories.php")
    suspend fun getCategories(): CategoryResponseDto

    @GET("list.php?a=list")
    suspend fun getArea() : AreaResponseDto

    @GET("search.php")
    suspend fun getRecipes(
        @Query("f") letter: String
    ): RecipeResponseDto

    @GET("filter.php")
    suspend fun filterRecipesByArea(
        @Query("a") area: String
    ): RecipeResponseDto

    @GET("filter.php")
    suspend fun filterRecipesByCategory(
        @Query("c") category: String
    ): RecipeResponseDto

    @GET("filter.php")
    suspend fun filterRecipesByIngredient(
        @Query("i") ingredient: String
    ): RecipeResponseDto

    @GET("search.php")
    suspend fun filterRecipesBySearch(
        @Query("s") query: String
    ): RecipeResponseDto

    @GET("random.php")
    suspend fun getRandomRecipe(): RecipeResponseDto
}