package com.example.cookingeasy.data.remote.firebase.fireStore

import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.model.RecipeUpload
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Inject
import kotlinx.coroutines.tasks.await

class RecipeFirestoreDataSource @Inject constructor(private val db: FirebaseFirestore) {

    private val recipesCollection = db.collection("recipes")

    suspend fun saveRecipe(recipe: RecipeUpload): String {
        val docRef = recipesCollection.document()
        val recipeMap = hashMapOf(
            "recipeId"     to docRef.id,
            "uid"          to recipe.uid,
            "userName"     to recipe.userName,
            "userImg"      to recipe.userImage,
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

    private fun mapDocToRecipeUpload(doc: DocumentSnapshot): RecipeUpload? {
        val parsed = doc.toObject(RecipeUpload::class.java) ?: return null
        val id = doc.getString("recipeId").orEmpty().ifEmpty { doc.id }
        return if (parsed.recipeId.isEmpty() || parsed.recipeId != id) parsed.copy(recipeId = id) else parsed
    }

    suspend fun getRecipesByUid(uid: String): List<RecipeUpload> {
        val snapshot = recipesCollection
            .whereEqualTo("uid", uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { mapDocToRecipeUpload(it) }
            .sortedByDescending { it.createdAt }
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

    /** Không dùng orderBy trên Firestore (tránh bắt buộc composite index); sắp xếp sau khi đọc. */
    suspend fun getRecipesByUserUUID(uid: String): List<RecipeUpload> {
        val snapshot = recipesCollection
            .whereEqualTo("uid", uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { mapDocToRecipeUpload(it) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun getPublishedRecipes(): List<RecipeUpload> {
        val snapshot = recipesCollection
            .whereEqualTo("status", "published")
            .get()
            .await()
        return snapshot.documents.mapNotNull {
            it.toObject(RecipeUpload::class.java)
        }.sortedByDescending { it.createdAt }
    }

    fun getFavoritesCollection(uid: String) =
        db.collection("users")
            .document(uid)
            .collection("favorites")

    suspend fun getFavorites(uid: String): List<Recipe> {
        val snapShot = db.collection("users")
            .document(uid)
            .collection("favorites")
            .get().await()
        return snapShot.documents.map {
            it.toObject(Recipe::class.java) as Recipe
        }
    }

    suspend fun getFavoriteIds(uid: String): List<String> {
        val snapshot = getFavoritesCollection(uid).get().await()
        return snapshot.documents.map { it.id }
    }

    suspend fun addFavorite(uid: String, recipe: Recipe) {
        val doc = getFavoritesCollection(uid).document(recipe.idMeal)

        val data = hashMapOf(
            "idMeal" to recipe.idMeal.toString(),
            "createdAt" to System.currentTimeMillis(),
            "strMeal" to recipe.strMeal,
            "strMealThumb" to recipe.strMealThumb
        )

        doc.set(data).await()
    }

    suspend fun removeFavorite(uid: String, recipeId: String) {
        getFavoritesCollection(uid).document(recipeId).delete().await()
    }

    suspend fun isFavorite(uid: String, recipeId: String): Boolean {
        val doc = getFavoritesCollection(uid).document(recipeId).get().await()
        return doc.exists()
    }
}