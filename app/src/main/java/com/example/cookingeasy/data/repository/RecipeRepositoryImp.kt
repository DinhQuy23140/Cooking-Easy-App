package com.example.cookingeasy.data.repository

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
        val response: RecipeResponseDto = recipeService.getRecipes()
        return RecipeMapper.toRecipeList(response.meals)
    }
}