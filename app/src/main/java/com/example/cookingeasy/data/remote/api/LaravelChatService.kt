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

data class InitiateCallRequest(
    val callerId: String,
    val receiverId: String,
    val type: String
)

data class CallActionRequest(
    val callId: String
)

data class InitiateCallResponse(
    val status: String? = null,
    val callId: String? = null
)

interface LaravelChatService {
    @POST("api/send-message")
    suspend fun sendMessage(@Body request: SendMessageRequest): SendMessageResponse

    @POST("api/call/initiate")
    suspend fun initiateCall(@Body request: InitiateCallRequest): InitiateCallResponse

    @POST("api/call/accept")
    suspend fun acceptCall(@Body request: CallActionRequest): SendMessageResponse

    @POST("api/call/reject")
    suspend fun rejectCall(@Body request: CallActionRequest): SendMessageResponse

    @POST("api/call/end")
    suspend fun endCall(@Body request: CallActionRequest): SendMessageResponse
}
