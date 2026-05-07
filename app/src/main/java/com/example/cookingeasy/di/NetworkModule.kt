package com.example.cookingeasy.di

import com.example.cookingeasy.data.remote.api.RecipeService
import com.example.cookingeasy.data.remote.api.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return RetrofitClient.getInstance()
    }
    @Provides
    @Singleton
    fun provideRecipeService(retrofit: Retrofit): RecipeService = retrofit.create(RecipeService::class.java)
}