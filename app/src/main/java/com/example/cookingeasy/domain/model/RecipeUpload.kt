package com.example.cookingeasy.domain.model

import java.util.UUID

data class RecipeUpload(
    val recipeId: String = UUID.randomUUID().toString(), // ← auto generate
    val uid: String = "",
    val mealName: String = "",
    val category: String = "",
    val area: String = "",
    val tags: String = "",
    val youtubeLink: String = "",
    val instructions: String = "",
    val ingredients: List<Map<String, String>> = emptyList(),
    val mealImageUrl: String = "",
    val videoUrl: String = "",
    val status: String = "draft",   // "draft" | "published"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)