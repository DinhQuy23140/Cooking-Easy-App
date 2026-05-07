package com.example.cookingeasy.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.cookingeasy.data.remote.firebase.fireStore.RecipeFirestoreDataSource
import com.example.cookingeasy.data.remote.supabase.SupabaseStorageDataSource
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.IRecipeUploadRepository
import jakarta.inject.Inject

class RecipeUploadRepositoryImp @Inject constructor(
    private val firestoreDataSource: RecipeFirestoreDataSource
) : IRecipeUploadRepository {

    override suspend fun saveDraft(
        uid: String,
        userName: String,
        userImg: String,
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
                userName = userName,
                userImage = userImg,
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
        userName: String,
        userImg: String,
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
                userName = userName,
                userImage = userImg,
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

    override suspend fun getFavoriteCount(uid: String): Result<Int> {
        return runCatching {
            firestoreDataSource.getFavoriteIds(uid).size
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