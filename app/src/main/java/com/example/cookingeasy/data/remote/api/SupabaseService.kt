package com.example.cookingeasy.data.remote.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Supabase Storage REST — [tài liệu](https://supabase.com/docs/reference/javascript/storage-from-upload)
 */
interface SupabaseService {

    /** Upload file (body = raw bytes + Content-Type). Bucket phải tồn tại trên dashboard. */
    @Headers("x-upsert: true")
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadObject(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @DELETE("storage/v1/object/{bucket}/{path}")
    suspend fun deleteObject(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>
}
