package com.example.cookingeasy.data.remote.api

object ApiServiceProvider {
    val recipeService: RecipeService by lazy {
        RetrofitClient.getInstance().create(RecipeService::class.java)
    }
}