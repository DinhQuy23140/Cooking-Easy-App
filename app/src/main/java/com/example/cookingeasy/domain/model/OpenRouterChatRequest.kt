package com.example.cookingeasy.domain.model

import com.google.gson.annotations.SerializedName

data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 500,
    val temperature: Float = 0.7f
)

data class OpenRouterMessage(
    val role: String,
    val content: String
)