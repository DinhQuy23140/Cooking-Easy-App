package com.example.cookingeasy.data.remote.api

import com.example.cookingeasy.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client gọi REST Storage của Supabase.
 * Cấu hình [BuildConfig.SUPABASE_URL] và [BuildConfig.SUPABASE_ANON_KEY] trong `local.properties`.
 */
object SupabaseClient {

    private val authInterceptor = Interceptor { chain ->
        val key = BuildConfig.SUPABASE_ANON_KEY
        val req = chain.request().newBuilder()
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .build()
        chain.proceed(req)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String {
        val raw = BuildConfig.SUPABASE_URL.trim()
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    val api: SupabaseService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseService::class.java)
    }

    fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
}
