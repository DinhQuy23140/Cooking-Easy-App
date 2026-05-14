package com.example.cookingeasy.domain.model

data class RecipeComment(
    val id: String = "",
    val recipeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userNickname: String = "",
    val userAvatarUrl: String = "",
    val content: String = "",
    val createdAt: Long = 0L
)
