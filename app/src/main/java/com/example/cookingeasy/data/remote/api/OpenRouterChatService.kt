package com.example.cookingeasy.data.remote.api

import com.example.cookingeasy.domain.model.OpenRouterChatRequest
import com.example.cookingeasy.domain.model.OpenRouterChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterChatService {
    @POST("chat/completions")
    suspend fun chat(
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}