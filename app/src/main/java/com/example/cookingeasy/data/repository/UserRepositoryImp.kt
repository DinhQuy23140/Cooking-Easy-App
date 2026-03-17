package com.example.cookingeasy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepositoryImp() : UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // ─── Create ──────────────────────────────────────────────────────

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

    // ─── Read ────────────────────────────────────────────────────────

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

    // ─── Update ──────────────────────────────────────────────────────

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

    // ─── Delete ──────────────────────────────────────────────────────

    override suspend fun deleteUserProfile(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Check ───────────────────────────────────────────────────────

    override suspend fun isProfileComplete(uid: String): Boolean {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val fullName = snapshot.getString("fullName") ?: ""
            fullName.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}