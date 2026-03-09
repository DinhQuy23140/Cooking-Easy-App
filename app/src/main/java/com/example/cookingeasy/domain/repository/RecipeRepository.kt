package com.example.cookingeasy.domain.repository

import com.example.cookingeasy.data.remote.dto.CategoryResponseDto
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe

interface RecipeRepository {
    suspend fun getCategories(): List<Category>

    suspend fun getAreas(): List<Area>

    suspend fun getRecipes(): List<Recipe>
}