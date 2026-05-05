package com.example.cookingeasy.data.repository

import com.example.cookingeasy.domain.model.HistorySearch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserRepositoryImp() : UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")


    override suspend fun saveUserProfile(
        uid: String,
        fullName: String,
        email: String
    ): Result<Unit> {
        return try {
            val userMap = hashMapOf(
                "uid"       to uid,
                "fullName"  to fullName,
                "nickname"  to fullName,
                "email"     to email,
                "avatarUrl" to "",
                "createdAt" to System.currentTimeMillis()
            )
            // SetOptions.merge() — không ghi đè nếu document đã tồn tại
            usersCollection.document(uid)
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getUserProfile(uid: String): Result<Map<String, Any>> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (snapshot.exists()) {
                Result.success(snapshot.data ?: emptyMap())
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun updateUserName(
        uid: String,
        fullName: String,
        nickname: String
    ): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                mapOf(
                    "fullName" to fullName,
                    "nickname" to nickname
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvatar(uid: String, avatarUrl: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("avatarUrl", avatarUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmail(uid: String, email: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("email", email)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun deleteUserProfile(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isProfileComplete(uid: String): Boolean {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val fullName = snapshot.getString("fullName") ?: ""
            fullName.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateImgProfile(uid: String, strImg: String): Result<Unit>{
        return try{
            usersCollection.document(uid)
                .update("avatarUrl", strImg)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getImgUrl(uid: String): String? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.getString("avatarUrl")
    }

    override suspend fun saveSearch(userId: String, keyword: String) {
        val docId = normalizeKeyword(keyword)

        val data = mapOf(
            "keyword" to keyword.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        usersCollection
            .document(userId)
            .collection("search_history")
            .document(docId)
            .set(data)
            .await()
    }
    fun normalizeKeyword(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .replace(" ", "_")
    }
    override suspend fun getListHistorySearch(userId: String): List<HistorySearch> {
        val snapshot = usersCollection.document(userId)
            .collection("search_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
        return snapshot.documents.map { doc -> doc.toHistorySearch(userId) }
    }

    override suspend fun deleteHistorySearch(userId: String, docId: String) {
        usersCollection
            .document(userId)
            .collection("search_history")
            .document(docId)
            .delete()
            .await()
    }

    override suspend fun clearHistorySearch(userId: String) {
        val snapshot = usersCollection
            .document(userId)
            .collection("search_history")
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }

    override fun getUid(): String {
        return firebaseAuth.currentUser?.uid ?: ""
    }

    private fun DocumentSnapshot.toHistorySearch(userId: String): HistorySearch {
        val keyword = getString("keyword") ?: ""
        val timeLabel = formatHistoryTime(this)
        return HistorySearch(
            id = id,
            userId = userId,
            keyword = keyword,
            timestamp = timeLabel
        )
    }

    private fun formatHistoryTime(doc: DocumentSnapshot): String {
        val ms = when (val v = doc.get("timestamp")) {
            null -> return ""
            is Long -> v
            is Number -> v.toLong()
            is com.google.firebase.Timestamp -> v.toDate().time
            else -> return ""
        }
        return SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date(ms))
    }
}