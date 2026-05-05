package com.example.cookingeasy.data.remote.supabase

import android.content.ContentResolver
import android.net.Uri
import com.example.cookingeasy.BuildConfig
import com.example.cookingeasy.data.remote.api.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/**
 * Upload / xóa file trên Supabase Storage (bucket public → URL có segment `public`).
 */
class SupabaseStorageDataSource(
    private val contentResolver: ContentResolver
) {

    suspend fun uploadRecipeImage(
        uri: Uri,
        remoteFolder: String = "uploads",
        fileName: String? = null
    ): Result<String> = uploadToBucket(
        uri = uri,
        bucket = Buckets.RECIPE_IMAGES,
        remoteFolder = remoteFolder,
        fileName = fileName,
        defaultMime = "image/jpeg"
    )

    suspend fun uploadRecipeVideo(
        uri: Uri,
        remoteFolder: String = "uploads",
        fileName: String? = null
    ): Result<String> = uploadToBucket(
        uri = uri,
        bucket = Buckets.RECIPE_VIDEOS,
        remoteFolder = remoteFolder,
        fileName = fileName,
        defaultMime = "video/mp4"
    )

    suspend fun deleteRecipeImage(objectPath: String): Result<Unit> =
        deleteFromBucket(Buckets.RECIPE_IMAGES, objectPath)

    suspend fun deleteRecipeVideo(objectPath: String): Result<Unit> =
        deleteFromBucket(Buckets.RECIPE_VIDEOS, objectPath)

    /** URL public để Glide / ExoPlayer (bucket phải public hoặc dùng signed URL riêng). */
    fun publicObjectUrl(bucket: String, objectPath: String): String {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val encodedPath = objectPath.split("/").joinToString("/") { Uri.encode(it) }
        return "$base/storage/v1/object/public/$bucket/$encodedPath"
    }

    fun isConfigured(): Boolean = SupabaseClient.isConfigured()

    private suspend fun uploadToBucket(
        uri: Uri,
        bucket: String,
        remoteFolder: String,
        fileName: String?,
        defaultMime: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(SupabaseClient.isConfigured()) {
                "Thiếu SUPABASE_URL hoặc SUPABASE_ANON_KEY trong local.properties"
            }
            val ext = guessExtension(uri, defaultMime)
            val name = fileName ?: "${UUID.randomUUID()}.$ext"
            val folder = remoteFolder.trim('/')
            val path = if (folder.isEmpty()) name else "$folder/$name"

            val mime = contentResolver.getType(uri) ?: defaultMime
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Không đọc được file từ URI")

            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val response = SupabaseClient.api.uploadObject(bucket, path, body)
            if (!response.isSuccessful) {
                val err = response.errorBody()?.string().orEmpty()
                error("Upload thất bại ${response.code()}: $err")
            }
            publicObjectUrl(bucket, path)
        }
    }

    private suspend fun deleteFromBucket(bucket: String, objectPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(SupabaseClient.isConfigured()) {
                    "Thiếu SUPABASE_URL hoặc SUPABASE_ANON_KEY trong local.properties"
                }
                val response = SupabaseClient.api.deleteObject(bucket, objectPath.trimStart('/'))
                if (!response.isSuccessful && response.code() != 404) {
                    val err = response.errorBody()?.string().orEmpty()
                    error("Xóa thất bại ${response.code()}: $err")
                }
            }
        }

    private fun guessExtension(uri: Uri, defaultMime: String): String {
        val name = uri.lastPathSegment ?: return when {
            defaultMime.startsWith("video") -> "mp4"
            else -> "jpg"
        }
        val dot = name.lastIndexOf('.')
        return if (dot >= 0 && dot < name.length - 1) name.substring(dot + 1).lowercase() else when {
            defaultMime.startsWith("video") -> "mp4"
            else -> "jpg"
        }
    }

    object Buckets {
        const val RECIPE_IMAGES = "recipe-images"
        const val RECIPE_VIDEOS = "recipe-videos"
    }
}
