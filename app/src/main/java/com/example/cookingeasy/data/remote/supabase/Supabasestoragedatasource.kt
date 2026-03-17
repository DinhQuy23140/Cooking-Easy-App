package com.example.cookingeasy.data.remote.supabase

import android.content.ContentResolver
import android.net.Uri
import com.example.cookingeasy.BuildConfig
import com.example.cookingeasy.data.remote.api.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class SupabaseStorageDataSource(
    private val contentResolver: ContentResolver
) {

    companion object {
        private val SUPABASE_URL = BuildConfig.SUPABASE_URL
        private val SUPABASE_KEY = BuildConfig.SUPABASE_ANON_KEY

        private const val IMAGE_BUCKET = "recipe-images"
        private const val VIDEO_BUCKET = "recipe-videos"
    }

}