package com.example.cookingeasy.data.remote.api

import com.example.cookingeasy.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LaravelClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String {
        val raw = BuildConfig.LARAVEL_BASE_URL.trim()
        val normalized = raw
            .replace("http://127.0.0.1", "http://10.0.2.2")
            .replace("http://localhost", "http://10.0.2.2")

        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    val api: LaravelChatService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LaravelChatService::class.java)
    }

    fun isConfigured(): Boolean = BuildConfig.LARAVEL_BASE_URL.isNotBlank()
}
