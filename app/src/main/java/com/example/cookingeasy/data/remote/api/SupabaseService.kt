package com.example.cookingeasy.data.remote.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SupabaseService {

    @Multipart
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadFile(
        @Header("Authorization") bearer: String,
        @Header("apikey") apiKey: String,
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
}