package com.example.cookingeasy.domain.model

data class DirectMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val type: String,
    val text: String,
    val imageUrl: String,
    val attachmentUrl: String,
    val attachmentName: String,
    val attachmentSize: String,
    val createdAt: Long
)

data class ConversationSummary(
    val conversationId: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserAvatar: String,
    val lastMessage: String,
    val lastSenderId: String,
    val updatedAt: Long,
    val unreadCount: Int,
    val isSeenByMe: Boolean
)

data class UserPresence(
    val isOnline: Boolean = false,
    val lastActiveAt: Long = 0L
)
