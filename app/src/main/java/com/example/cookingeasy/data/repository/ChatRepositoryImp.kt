package com.example.cookingeasy.data.repository

import android.util.Log
import com.example.cookingeasy.BuildConfig
import com.example.cookingeasy.data.remote.api.OpenRouterChatService
import com.example.cookingeasy.domain.model.ChatMessage
import com.example.cookingeasy.domain.model.ChatRole
import com.example.cookingeasy.domain.model.OpenRouterChatRequest
import com.example.cookingeasy.domain.model.OpenRouterMessage
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepositoryImp() : ChatRepository {

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"
        private const val API_KEY = BuildConfig.OpenRouter_KEY

        private val MODELS = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "mistralai/mistral-small-3.1-24b-instruct:free",
            "google/gemma-3-12b-it:free"
        )
        private const val SYSTEM_PROMPT = """You are Chef AI inside CookingEasy app. You MUST respond ONLY with a JSON object, nothing else.

        STRICT RULES:
        - NEVER write plain text
        - ALWAYS respond with exactly this JSON format: {"keyword": "...", "reply": "..."}
        - keyword: food/ingredient word for recipe search, or "" if no food mentioned
        - reply: 1-2 sentence friendly response
        
        EXAMPLES (copy this exact format):
        Input: hello
        Output: {"keyword": "", "reply": "Hi! What would you like to cook today? 🍳"}
        
        Input: I want chicken
        Output: {"keyword": "chicken", "reply": "Great choice! Here are some chicken recipes for you!"}
        
        Input: pasta with tomato
        Output: {"keyword": "pasta", "reply": "Delicious! Here are some pasta recipes!"}
        
        Input: something spicy
        Output: {"keyword": "spicy", "reply": "Here are some spicy recipes to try!"}
        
        REMEMBER: Output ONLY the JSON object. No explanation, no markdown, no plain text."""
    }

    private val service: OpenRouterChatService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://cookingeasy.app")
                    .addHeader("X-Title", "CookingEasy")
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // ← tăng timeout vì model free chậm
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterChatService::class.java)
    }

    private val recipeRepository = RecipeRepositoryImp()

    override suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessage>
    ): Pair<String, List<Recipe>> {

        val messages = buildMessages(userMessage, history)
        var lastException: Exception? = null

        for (model in MODELS) {
            try {
                Log.d("ChatBot", "Trying model: $model")

                val request = OpenRouterChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = 512,
                    temperature = 0.7f
                )

                val response = service.chat(request)
                Log.d("ChatBot", "Response: ${Gson().toJson(response)}")

                val raw = response.choices?.firstOrNull()?.message?.content
                    ?: throw Exception(response.error?.message ?: "No response")

                Log.d("ChatBot", "Raw: $raw")
                return parseResponse(raw)

            } catch (e: Exception) {
                if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("ChatBot", "HTTP ${e.code()} | model: $model | $errorBody")
                    if (e.code() == 429) delay(3000) else delay(500)
                } else {
                    Log.e("ChatBot", "model: $model | error: ${e.message}")
                    delay(500)
                }
                lastException = e
            }
        }
        throw lastException ?: Exception("All models failed")
    }

    private fun buildMessages(
        userMessage: String,
        history: List<ChatMessage>
    ): List<OpenRouterMessage> {
        val messages = mutableListOf<OpenRouterMessage>()
        messages.add(OpenRouterMessage(role = "user", content = SYSTEM_PROMPT.trimIndent()))
        messages.add(OpenRouterMessage(
            role = "assistant",
            content = """{"keyword":"","reply":"Understood! I am Chef AI, ready to help."}"""
        ))
        history.takeLast(8).forEach { msg ->
            if (!msg.isLoading && msg.content.isNotEmpty()) {
                messages.add(OpenRouterMessage(
                    role = if (msg.role == ChatRole.USER) "user" else "assistant",
                    content = msg.content
                ))
            }
        }
        messages.add(OpenRouterMessage(role = "user", content = userMessage))
        return messages
    }

    private suspend fun parseResponse(raw: String): Pair<String, List<Recipe>> {
        return try {
            val clean = raw.replace("```json", "").replace("```", "").trim()
            val start = clean.indexOf("{")
            val end = clean.lastIndexOf("}")

            // ✅ Nếu không có JSON → dùng raw text làm reply, không search recipe
            if (start == -1 || end == -1) {
                Log.w("ChatBot", "No JSON found, using raw as reply: $raw")
                return Pair(raw.take(200), emptyList())
            }

            val json = JSONObject(clean.substring(start, end + 1))
            val keyword = json.optString("keyword", "").trim()
            val reply = json.optString("reply", "Here are some recipes for you!")

            Log.d("ChatBot", "keyword: $keyword | reply: $reply")

            val recipes = if (keyword.length >= 2) {
                try {
                    recipeRepository.filterRecipesBySearch(keyword).take(5)
                } catch (e: Exception) {
                    Log.e("ChatBot", "Recipe search failed: ${e.message}")
                    emptyList()
                }
            } else emptyList()

            Pair(reply, recipes)

        } catch (e: Exception) {
            Log.e("ChatBot", "parseResponse failed: ${e.message} | raw: $raw")
            Pair(raw.take(200), emptyList()) // ← fallback hiển thị raw text
        }
    }
}

