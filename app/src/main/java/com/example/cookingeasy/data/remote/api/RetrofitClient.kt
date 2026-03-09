package com.example.cookingeasy.data.remote.api

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private const val URL = "https://www.themealdb.com/api/json/v1/1/"

    fun getInstance(): Retrofit {

        if (retrofit == null) {

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()

                    Log.d(
                        "HTTP_URL",
                        "URL: ${request.url} METHOD: ${request.method}"
                    )

                    chain.proceed(request)
                }
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!
    }
}