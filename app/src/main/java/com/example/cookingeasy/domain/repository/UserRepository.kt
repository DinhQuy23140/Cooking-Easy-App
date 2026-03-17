package com.example.cookingeasy.data.repository

interface UserRepository {

    // ─── Create ──────────────────────────────────────────────────────

    suspend fun saveUserProfile(
        uid: String,
        fullName: String,
        email: String
    ): Result<Unit>

    // ─── Read ────────────────────────────────────────────────────────

    suspend fun getUserProfile(uid: String): Result<Map<String, Any>>

    // ─── Update ──────────────────────────────────────────────────────

    suspend fun updateUserName(
        uid: String,
        fullName: String,
        nickname: String
    ): Result<Unit>

    suspend fun updateAvatar(uid: String, avatarUrl: String): Result<Unit>

    suspend fun updateEmail(uid: String, email: String): Result<Unit>

    // ─── Delete ──────────────────────────────────────────────────────

    suspend fun deleteUserProfile(uid: String): Result<Unit>

    // ─── Check ───────────────────────────────────────────────────────

    suspend fun isProfileComplete(uid: String): Boolean
}