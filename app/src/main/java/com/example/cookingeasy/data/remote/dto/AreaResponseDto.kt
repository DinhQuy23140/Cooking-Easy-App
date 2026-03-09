package com.example.cookingeasy.data.remote.dto

data class AreaResponseDto(
    val meals: List<AreaDto>
)

data class AreaDto(
    val strArea: String
)