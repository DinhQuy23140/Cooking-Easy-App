package com.example.cookingeasy.data.remote.dto

import com.example.cookingeasy.domain.model.Category

data class CategoryResponseDto(
    val categories: List<CategoryDto>
)

data class CategoryDto(
    val idCategory: String ="",
    val strCategory: String = "",
    val strCategoryThumb: String = "",
    val strCategoryDescription: String = ""
)