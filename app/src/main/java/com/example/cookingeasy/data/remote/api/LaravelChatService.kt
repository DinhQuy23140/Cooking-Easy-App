package com.example.cookingeasy.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

data class SendMessageRequest(
    val sender_id: String,
    val receiver_id: String,
    val content: String
)

data class SendMessageResponse(
    val status: String? = null
)

interface LaravelChatService {
    @POST("api/send-message")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendMessageResponse
}
