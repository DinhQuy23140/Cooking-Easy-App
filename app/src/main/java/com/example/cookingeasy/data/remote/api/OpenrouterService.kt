package com.example.cookingeasy.data.remote.api

import android.graphics.Bitmap
import com.example.cookingeasy.BuildConfig
import com.example.cookingeasy.domain.model.Ingredient
import com.example.cookingeasy.domain.model.ScanResult

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.util.Log

val apiKey = BuildConfig.OpenRouter_KEY
// OpenrouterService.kt
class OpenrouterService {
    // Thay GeminiService bằng OpenRouter
    private val client = OkHttpClient()

    private val freeVisionModels = listOf(
        "google/gemma-3-4b-it:free",
        "meta-llama/llama-3.2-11b-vision-instruct:free",
        "google/gemini-2.0-flash-lite",
        "moonshotai/kimi-vl-a3b-thinking:free"
    )

    suspend fun scanIngredients(bitmap: Bitmap): Result<ScanResult> {
        val base64 = bitmapToBase64(bitmap)

        for (model in freeVisionModels) {
            Log.d("OpenRouter", "Trying model: $model")
            val result = tryScanWithModel(base64, model)

            if (result.isSuccess) return result

            val error = result.exceptionOrNull()?.message ?: ""
            val isRateLimit = error.contains("429") || error.contains("rate") || error.contains("Provider returned error")

            if (isRateLimit) {
                Log.w("OpenRouter", "Model $model rate limited, trying next...")
                continue // thử model tiếp theo
            }

            return result // lỗi khác thì dừng luôn
        }

        return Result.failure(Exception("Tất cả model đang bận, vui lòng thử lại sau"))
    }

    private suspend fun tryScanWithModel(base64: String, model: String): Result<ScanResult> {
        val json = """
        {
          "model": "$model",
          "messages": [{
            "role": "user",
            "content": [
              {
                "type": "image_url",
                "image_url": { "url": "data:image/jpeg;base64,$base64" }
              },
              {
                "type": "text",
                "text": "Analyze this food image. Return ONLY JSON: {\"dish_name\": \"...\", \"ingredients\": [{\"name\": \"...\", \"amount\": \"...\", \"unit\": \"...\", \"calories\": 0}]}"
              }
            ]
          }]
        }
    """.trimIndent()

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${BuildConfig.OpenRouter_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: throw Exception("Empty response")
            Log.d("OpenRouter", "[$model] Code: ${response.code} Body: $body")

            val jsonObject = JSONObject(body)

            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error")
                throw Exception(error.getString("message"))
            }

            val content = jsonObject
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val cleanJson = content
                .trim()
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
                .let { str ->
                    val start = str.indexOf("{")
                    val end = str.lastIndexOf("}")
                    if (start != -1 && end != -1) str.substring(start, end + 1)
                    else str
                }

            val result = Gson().fromJson(cleanJson, ScanResultDto::class.java)
            Result.success(result.toDomain())

        } catch (e: Exception) {
            Log.e("OpenRouter", "[$model] Error: ${e.message}")
            Result.failure(e)
        }
    }

//    suspend fun scanIngredients(bitmap: Bitmap): Result<ScanResult> {
//        val base64 = bitmapToBase64(bitmap)
//
//        val json = """
//        {
//          "model": "openrouter/free",
//          "messages": [{
//            "role": "user",
//            "content": [
//              {
//                "type": "image_url",
//                "image_url": { "url": "data:image/jpeg;base64,$base64" }
//              },
//              {
//                "type": "text",
//                "text": "Analyze this food image. Return ONLY JSON: {\"dish_name\": \"...\", \"ingredients\": [{\"name\": \"...\", \"amount\": \"...\", \"unit\": \"...\", \"calories\": 0}]}"
//              }
//            ]
//          }]
//        }
//    """.trimIndent()
//
//        val request = Request.Builder()
//            .url("https://openrouter.ai/api/v1/chat/completions")
//            .addHeader("Authorization", "Bearer ${BuildConfig.OpenRouter_KEY}")
//            .addHeader("Content-Type", "application/json")
//            .post(json.toRequestBody("application/json".toMediaType()))
//            .build()
//
//        return try {
//            val response = withContext(Dispatchers.IO) {
//                client.newCall(request).execute()
//            }
//            val body = response.body?.string() ?: throw Exception("Empty response")
//
//            // Log response để debug
//            Log.d("OpenRouter", "Response code: ${response.code}")
//            Log.d("OpenRouter", "Response body: $body")
//
//            val jsonObject = JSONObject(body)
//
//            // Kiểm tra có lỗi không
//            if (jsonObject.has("error")) {
//                val error = jsonObject.getJSONObject("error")
//                throw Exception("API Error: ${error.getString("message")}")
//            }
//
//            val content = jsonObject
//                .getJSONArray("choices")
//                .getJSONObject(0)
//                .getJSONObject("message")
//                .getString("content")
//                .replace("```json", "").replace("```", "").trim()
//
//            val result = Gson().fromJson(content, ScanResultDto::class.java)
//
//            Log.d("Result Scan: ", result.ingredients.toString())
//            Result.success(result.toDomain())
//        } catch (e: Exception) {
//            Log.e("ScanError", "Error scan: ${e.message}")
//            Result.failure(e)
//        }
//    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }


//    private val model = GenerativeModel(
//        modelName = "gemini-2.0-flash",
//        apiKey = BuildConfig.GEMINI_API_KEY
//    )
//
//    suspend fun scanIngredients(bitmap: Bitmap): Result<ScanResult> {
//        repeat(3) { attempt ->
//            try {
//                val prompt = content {
//                    image(bitmap)
//                    text("""
//                    You are a food ingredient analyzer.
//                    Look at this image and identify all visible food ingredients.
//                    Return ONLY a valid JSON, no explanation, no markdown:
//                    {
//                      "dish_name": "name of the dish or food",
//                      "ingredients": [
//                        {
//                          "name": "ingredient name",
//                          "amount": "estimated amount or null",
//                          "unit": "g/ml/tbsp/etc or null",
//                          "calories": estimated calories as integer or null
//                        }
//                      ]
//                    }
//                    If multiple dishes are visible, include all ingredients combined.
//                """.trimIndent())
//                }
//
//                val response = model.generateContent(prompt)
//                val json = response.text
//                    ?.replace("```json", "")
//                    ?.replace("```", "")
//                    ?.trim()
//                    ?: throw Exception("Empty response")
//
//                val result = Gson().fromJson(json, ScanResultDto::class.java)
//                Result.success(result.toDomain())
//            } catch (e: Exception) {
//                val isQuotaError = e.message?.contains("quota", ignoreCase = true) == true
//                        || e.message?.contains("429") == true
//
//                if (isQuotaError && attempt < 2) {
//                    delay(15_000L) // chờ 15 giây rồi retry
//                    return@repeat
//                }
//                return Result.failure(e)
//            }
//        }
//        return Result.failure(Exception("Quota exceeded, please try again later"))
//    }
}

// DTO để parse JSON
private data class ScanResultDto(
    @SerializedName("dish_name") val dishName: String,
    @SerializedName("ingredients") val ingredients: List<IngredientDto>
) {
    fun toDomain() = ScanResult(
        dishName = dishName,
        ingredients = ingredients.map { it.toDomain() }
    )
}

private data class IngredientDto(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: String?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("calories") val calories: Int?
) {
    fun toDomain() = Ingredient(name, amount, unit, calories)
}