package com.example.cookingeasy.domain.repository

import com.example.cookingeasy.domain.model.ChatMessage
import com.example.cookingeasy.domain.model.Recipe

interface ChatRepository {
    suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessage>
    ): Pair<String, List<Recipe>>
}