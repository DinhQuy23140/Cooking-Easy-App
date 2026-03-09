package com.example.cookingeasy.common.listener

import com.example.cookingeasy.domain.model.Recipe

interface RecipeListener {
    fun OnClickItem(recipe: Recipe)
    fun OnFavoriteClick(boolean: Boolean)
}