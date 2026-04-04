package com.example.cookingeasy.domain.repository
import android.net.Uri
import com.example.cookingeasy.domain.model.RecipeUpload

interface IRecipeUploadRepository {

    suspend fun saveDraft(
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
    ): Result<String>

    suspend fun publish(
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
    ): Result<String>

    suspend fun getMyRecipes(uid: String): Result<List<RecipeUpload>>
    suspend fun deleteRecipe(recipeId: String): Result<Unit>
    suspend fun getRecipesByUserUUID(uid: String): Result<List<RecipeUpload>>
    suspend fun updateStatus(recipeId: String, status: String): Result<Unit>
}