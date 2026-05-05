package com.example.cookingeasy.common.listener

import com.example.cookingeasy.domain.model.RecipeUpload

interface UploadRecipeListener {
    fun onItemClick(recipe: RecipeUpload)
    fun onEdit(recipe: RecipeUpload)
    fun onDelete(recipe: RecipeUpload)
}