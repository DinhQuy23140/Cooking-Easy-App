package com.example.cookingeasy.domain.model

import com.google.firebase.firestore.PropertyName
import java.util.UUID

data class RecipeUpload(
    @PropertyName("recipeId")
    val recipeId: String = UUID.randomUUID().toString(), // ← auto generate
    @PropertyName("uid")
    val uid: String = "",
    @PropertyName("userName")
    val userName: String = "",
    @PropertyName("userImg")
    val userImage: String = "",
    val mealName: String = "",
    @PropertyName("category")
    val category: String = "",
    @PropertyName("area")
    val area: String = "",
    @PropertyName("tags")
    val tags: String = "",
    @PropertyName("youtubeLink")
    val youtubeLink: String = "",
    @PropertyName("instructions")
    val instructions: String = "",
    @PropertyName("ingredients")
    val ingredients: List<Map<String, String>> = emptyList(),
    @PropertyName("mealImageUrl")
    val mealImageUrl: String = "",
    @PropertyName("videoUrl")
    val videoUrl: String = "",
    @PropertyName("status")
    val status: String = "draft",   // "draft" | "published"
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)