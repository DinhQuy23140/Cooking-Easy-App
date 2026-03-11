package com.example.cookingeasy.domain.model

// Ingredient.kt
data class Ingredient(
    val name: String,
    val amount: String?,
    val unit: String?,
    val calories: Int?
)

data class ScanResult(
    val dishName: String,
    val ingredients: List<Ingredient>
)
