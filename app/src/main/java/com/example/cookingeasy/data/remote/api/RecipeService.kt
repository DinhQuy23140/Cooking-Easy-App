package com.example.cookingeasy.data.remote.api

import com.example.cookingeasy.data.remote.dto.AreaResponseDto
import com.example.cookingeasy.data.remote.dto.CategoryResponseDto
import com.example.cookingeasy.data.remote.dto.RecipeResponseDto
import com.example.cookingeasy.domain.model.Category
import retrofit2.http.GET


interface RecipeService {
    @GET("categories.php")
    suspend fun getCategories(): CategoryResponseDto

    @GET("list.php?a=list")
    suspend fun getArea() : AreaResponseDto

    @GET("search.php?f=a")
    suspend fun getRecipes(): RecipeResponseDto
}