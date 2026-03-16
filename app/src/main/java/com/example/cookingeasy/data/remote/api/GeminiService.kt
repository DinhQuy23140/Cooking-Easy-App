package com.example.cookingeasy.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,   // ← @Path trước
        @Query("key") apiKey: String,   // ← @Query sau
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("system_instruction") val systemInstruction: GeminiContent? = null,
    @SerializedName("generation_config") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    @SerializedName("max_output_tokens") val maxOutputTokens: Int = 512
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val error: GeminiError?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiError(
    val message: String?,
    val code: Int?
)