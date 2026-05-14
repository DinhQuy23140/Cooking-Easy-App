package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingeasy.common.adapter.ActiveUserUi
import com.example.cookingeasy.common.adapter.ChatConversation
import com.example.cookingeasy.data.repository.DirectChatRepositoryImp
import com.example.cookingeasy.domain.repository.DirectChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class ListChatViewModel @Inject constructor(
    private val repository: DirectChatRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _activeUsers = MutableStateFlow<List<ActiveUserUi>>(emptyList())
    val activeUsers: StateFlow<List<ActiveUserUi>> = _activeUsers.asStateFlow()

    fun start() {
        viewModelScope.launch {
            _loading.value = true
            repository.observeConversations().collect { list ->
                val presenceMap = repository.getUsersPresence(list.map { it.otherUserId })
                _conversations.value = list.map {
                    val isActive = presenceMap[it.otherUserId]?.isOnline == true
                    ChatConversation(
                        id = it.conversationId,
                        peerUid = it.otherUserId,
                        displayName = it.otherUserName,
                        snippet = it.lastMessage,
                        timeLabel = formatTime(it.updatedAt),
                        unreadCount = it.unreadCount,
                        isOnline = isActive,
                        isGroup = false,
                        avatarUrl = it.otherUserAvatar
                    )
                }
                _activeUsers.value = list
                    .filter { presenceMap[it.otherUserId]?.isOnline == true }
                    .distinctBy { it.otherUserId }
                    .take(12)
                    .map {
                        ActiveUserUi(
                            uid = it.otherUserId,
                            name = it.otherUserName,
                            avatarUrl = it.otherUserAvatar
                        )
                    }
                _loading.value = false
            }
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    }

    companion object {
        private const val ACTIVE_WINDOW_MS = 5 * 60 * 1000L
    }
}
