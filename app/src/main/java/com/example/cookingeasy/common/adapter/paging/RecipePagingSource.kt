package com.example.cookingeasy.common.adapter.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.RecipeRepository
import java.util.Collections

class RecipePagingSource(private val recipeRepository: RecipeRepository) : PagingSource<Int, Recipe>() {

    override fun getRefreshKey(state: PagingState<Int, Recipe>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Recipe> {
        try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val listRecipe: List<Recipe> = recipeRepository.getRecipes()
            val fromIndex = page * pageSize
            val toIndex = Math.min(fromIndex + pageSize, listRecipe.size)
            if (fromIndex > listRecipe.size) {
                return LoadResult.Page(Collections.emptyList(), null, null)
            }
            val pageData: List<Recipe> = listRecipe.subList(fromIndex, toIndex)
            return LoadResult.Page(
                pageData,
                if (page === 0) null else page - 1,
                if (toIndex >= listRecipe.size) null else page + 1
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

}