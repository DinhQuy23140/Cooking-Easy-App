package com.example.cookingeasy.data.remote.firebase

import android.util.Log
import com.example.cookingeasy.domain.model.RecipeUpload
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RecipeFirestoreDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val recipesCollection = db.collection("recipes")

    // ─── Save recipe ─────────────────────────────────────────────────

    suspend fun saveRecipe(recipe: RecipeUpload): String {
        val docRef = recipesCollection.document()
        val recipeMap = hashMapOf(
            "recipeId"     to docRef.id,
            "uid"          to recipe.uid,
            "mealName"     to recipe.mealName,
            "category"     to recipe.category,
            "area"         to recipe.area,
            "tags"         to recipe.tags,
            "youtubeLink"  to recipe.youtubeLink,
            "instructions" to recipe.instructions,
            "ingredients"  to recipe.ingredients,
            "mealImageUrl" to recipe.mealImageUrl,
            "videoUrl"     to recipe.videoUrl,
            "status"       to recipe.status,
            "createdAt"    to recipe.createdAt,
            "updatedAt"    to recipe.updatedAt
        )
        docRef.set(recipeMap).await()
        return docRef.id
    }

    // ─── Get recipes by uid ──────────────────────────────────────────

    suspend fun getRecipesByUid(uid: String): List<RecipeUpload> {
        val snapshot = recipesCollection
            .whereEqualTo("uid", uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull {
            it.toObject(RecipeUpload::class.java)
        }
    }

    // ─── Delete recipe ───────────────────────────────────────────────

    suspend fun deleteRecipe(recipeId: String) {
        recipesCollection
            .document(recipeId)
            .delete()
            .await()
    }

    // ─── Update status ───────────────────────────────────────────────

    suspend fun updateRecipeStatus(recipeId: String, status: String) {
        recipesCollection
            .document(recipeId)
            .update(
                mapOf(
                    "status"    to status,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    suspend fun getRecipesByUserUUID(uid: String): List<RecipeUpload> {
        val snapshot = recipesCollection
            .whereEqualTo("uid", uid)  // ← filter theo uid người đăng
            .orderBy("createdAt", Query.Direction.DESCENDING) // ← mới nhất trước
            .get()
            .await()
        return snapshot.documents.mapNotNull {
            it.toObject(RecipeUpload::class.java)
        }
    }
}