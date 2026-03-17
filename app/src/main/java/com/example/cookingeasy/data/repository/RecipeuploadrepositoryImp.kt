package com.example.cookingeasy.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.cookingeasy.data.remote.firebase.RecipeFirestoreDataSource
import com.example.cookingeasy.data.remote.supabase.SupabaseStorageDataSource
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository

class RecipeUploadRepositoryImp(
    contentResolver: ContentResolver
) : IRecipeUploadRepository {

    private val firestoreDataSource = RecipeFirestoreDataSource()
    private val storageDataSource = SupabaseStorageDataSource(contentResolver)

    // ─────────────────────────────────────────────
    // Save Draft
    // ─────────────────────────────────────────────

    override suspend fun saveDraft(
        uid: String,
        mealName: String,
        category: String,
        area: String,
        tags: String,
        youtubeLink: String,
        instructions: String,
        ingredients: List<Map<String, String>>,
        imageUrl: String
    ): Result<String> {

        return runCatching {

            val recipe = RecipeUpload(
                uid = uid,
                mealName = mealName,
                category = category,
                area = area,
                tags = tags,
                youtubeLink = youtubeLink,
                instructions = instructions,
                ingredients = ingredients,
                mealImageUrl = imageUrl,
                videoUrl = "",
                status = "draft"
            )

            firestoreDataSource.saveRecipe(recipe)
        }
    }

    // ─────────────────────────────────────────────
    // Publish
    // ─────────────────────────────────────────────

    override suspend fun publish(
        uid: String,
        mealName: String,
        category: String,
        area: String,
        tags: String,
        youtubeLink: String,
        instructions: String,
        ingredients: List<Map<String, String>>,
        imageUrl: String,
        videoUri: String
    ): Result<String> {

        return runCatching {

            val recipe = RecipeUpload(
                uid = uid,
                mealName = mealName,
                category = category,
                area = area,
                tags = tags,
                youtubeLink = youtubeLink,
                instructions = instructions,
                ingredients = ingredients,
                mealImageUrl = imageUrl,
                videoUrl = videoUri,
                status = "published"
            )
            firestoreDataSource.saveRecipe(recipe)
        }
    }

    override suspend fun getMyRecipes(uid: String): Result<List<RecipeUpload>> {
        return runCatching {
            firestoreDataSource.getRecipesByUid(uid)
        }
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return runCatching {
            firestoreDataSource.deleteRecipe(recipeId)
        }
    }

    override suspend fun getRecipesByUserUUID(uid: String): Result<List<RecipeUpload>> {
        return runCatching {
            firestoreDataSource.getRecipesByUserUUID(uid)
        }
    }

    override suspend fun updateStatus(recipeId: String, status: String): Result<Unit> {
        return runCatching {
            firestoreDataSource.updateRecipeStatus(recipeId, status)
        }
    }
}