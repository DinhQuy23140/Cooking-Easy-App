package com.example.cookingeasy.domain.repository

import com.example.cookingeasy.domain.model.ConversationSummary
import com.example.cookingeasy.domain.model.DirectMessage
import com.example.cookingeasy.domain.model.UserPresence
import kotlinx.coroutines.flow.Flow

interface DirectChatRepository {
    fun observeMessages(otherUid: String): Flow<List<DirectMessage>>
    fun observeOtherUserSeen(otherUid: String): Flow<Boolean>
    fun observeUserPresence(uid: String): Flow<UserPresence>
    fun observeConversations(): Flow<List<ConversationSummary>>
    suspend fun getUsersPresence(uids: List<String>): Map<String, UserPresence>
    suspend fun sendTextMessage(otherUid: String, text: String): Result<Unit>
    suspend fun sendImageMessage(otherUid: String, imageUrl: String): Result<Unit>
    suspend fun sendAttachmentMessage(otherUid: String, fileUrl: String, fileName: String, fileSize: String): Result<Unit>
    suspend fun sendVideoMessage(otherUid: String, fileUrl: String, fileName: String, fileSize: String): Result<Unit>
    suspend fun sendVoiceMessage(otherUid: String, fileUrl: String, fileName: String, fileSize: String): Result<Unit>
    suspend fun markConversationSeen(otherUid: String): Result<Unit>
}
