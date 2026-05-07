package com.example.cookingeasy.data.repository

import com.example.cookingeasy.domain.model.HistorySearch

interface UserRepository {
    suspend fun saveUserProfile(
        uid: String,
        fullName: String,
        email: String
    ): Result<Unit>
    suspend fun getUserProfile(uid: String): Result<Map<String, Any>>
    suspend fun updateUserName(
        uid: String,
        fullName: String,
        nickname: String
    ): Result<Unit>
    suspend fun updateAvatar(uid: String, avatarUrl: String): Result<Unit>
    suspend fun updateEmail(uid: String, email: String): Result<Unit>
    suspend fun deleteUserProfile(uid: String): Result<Unit>
    suspend fun isProfileComplete(uid: String): Boolean
    suspend fun updateImgProfile(uid: String, strImg: String): Result<Unit>
    suspend fun getImgUrl(uid: String): String?
    suspend fun saveSearch(userId: String, keyword: String)
    suspend fun getListHistorySearch(userId: String): List<HistorySearch>
    suspend fun deleteHistorySearch(userId: String, docId: String)
    suspend fun clearHistorySearch(userId: String)
    suspend fun followUser(targetUid: String)
    suspend fun unfollowUser(targetUid: String)
    suspend fun isFollowing(targetUid: String): Boolean
    suspend fun getFollowStats(uid: String): Pair<Int, Int>
    fun getUid(): String
}