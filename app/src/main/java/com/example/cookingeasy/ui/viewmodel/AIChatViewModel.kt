package com.example.cookingeasy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.data.repository.ChatRepositoryImp
import com.example.cookingeasy.domain.model.ChatMessage
import com.example.cookingeasy.domain.model.ChatRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AIChatViewModel : ViewModel() {

    private val chatRepository = ChatRepositoryImp()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        viewModelScope.launch {

            // 1. Thêm tin nhắn user
            appendMessage(ChatMessage(content = userInput, role = ChatRole.USER))

            // 2. Loading
            val loadingId = UUID.randomUUID().toString()
            appendMessage(ChatMessage(id = loadingId, content = "", role = ChatRole.BOT, isLoading = true))
            _isTyping.value = true

            try {
                // ✅ Truyền history KHÔNG bao gồm 2 item cuối (user + loading)
                val historyWithoutLast = _messages.value.dropLast(2)

                val (reply, recipes) = chatRepository.sendMessage(
                    userMessage = userInput,
                    history = historyWithoutLast // ← fix
                )

                replaceMessage(
                    id = loadingId,
                    newMessage = ChatMessage(
                        content = reply,
                        role = ChatRole.BOT,
                        recipes = recipes
                    )
                )

            } catch (e: Exception) {
                Log.e("ChatBot", "ViewModel error: ${e.message}", e)
                val errorMsg = when {
                    e.message?.contains("quota") == true ->
                        "I've reached my daily limit. Please try again tomorrow! 🙏"
                    e.message?.contains("network") == true ->
                        "No internet connection. Please check your network!"
                    else ->
                        "Sorry, something went wrong. Please try again!"
                }
                replaceMessage(
                    id = loadingId,
                    newMessage = ChatMessage(content = errorMsg, role = ChatRole.BOT)
                )
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    private fun appendMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun replaceMessage(id: String, newMessage: ChatMessage) {
        _messages.value = _messages.value.map {
            if (it.id == id) newMessage else it
        }
    }
}