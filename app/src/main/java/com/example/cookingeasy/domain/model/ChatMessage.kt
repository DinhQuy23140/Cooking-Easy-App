package com.example.cookingeasy.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: ChatRole,
    val recipes: List<Recipe> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

enum class ChatRole { USER, BOT }
