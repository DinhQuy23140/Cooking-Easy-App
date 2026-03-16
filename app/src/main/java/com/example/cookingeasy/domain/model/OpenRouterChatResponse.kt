package com.example.cookingeasy.domain.model

import com.google.gson.annotations.SerializedName

data class OpenRouterChatResponse(
    val id: String?,
    val choices: List<ChatChoice>?,
    val error: OpenRouterError?
)

data class ChatChoice(
    val message: OpenRouterMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class OpenRouterError(
    val message: String?,
    val code: Int?
)
