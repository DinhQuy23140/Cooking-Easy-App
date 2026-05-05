package com.example.cookingeasy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.ContentResolver
import android.net.Uri
import com.example.cookingeasy.common.adapter.MessageContentType
import com.example.cookingeasy.common.adapter.MessageSendStatus
import com.example.cookingeasy.common.adapter.MessageUiModel
import com.example.cookingeasy.data.repository.DirectChatRepositoryImp
import com.example.cookingeasy.data.remote.supabase.SupabaseStorageDataSource
import com.example.cookingeasy.domain.repository.DirectChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatDetailViewModel(
    private val otherUid: String,
    private val otherName: String,
    private val otherAvatarUrl: String,
    private val repository: DirectChatRepository,
    private val storageDataSource: SupabaseStorageDataSource,
    private val contentResolver: ContentResolver
) : ViewModel() {

    class Factory(
        private val otherUid: String,
        private val otherName: String,
        private val otherAvatarUrl: String,
        private val contentResolver: ContentResolver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatDetailViewModel(
                otherUid = otherUid,
                otherName = otherName,
                otherAvatarUrl = otherAvatarUrl,
                repository = DirectChatRepositoryImp(),
                storageDataSource = SupabaseStorageDataSource(contentResolver),
                contentResolver = contentResolver
            ) as T
        }
    }

    data class UiState(
        val loading: Boolean = false,
        val otherName: String = "",
        val isOtherActive: Boolean = false,
        val otherLastActiveAt: Long = 0L,
        val messages: List<MessageUiModel> = emptyList(),
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(otherName = otherName))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val myAvatarUrl: String = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString().orEmpty()
    private var latestMessages = emptyList<com.example.cookingeasy.domain.model.DirectMessage>()
    private var otherHasSeenConversation = false
    private var otherIsOnline = false
    private var otherLastActiveAt = 0L

    fun start() {
        if (otherUid.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Chat user is unavailable")
            return
        }
        viewModelScope.launch {
            repository.markConversationSeen(otherUid)
        }
        viewModelScope.launch {
            repository.observeOtherUserSeen(otherUid).collect { seen ->
                otherHasSeenConversation = seen
                emitUi(latestMessages)
            }
        }
        viewModelScope.launch {
            repository.observeUserPresence(otherUid).collect { presence ->
                otherIsOnline = presence.isOnline
                otherLastActiveAt = presence.lastActiveAt
                emitUi(latestMessages)
            }
        }
        viewModelScope.launch {
            repository.observeMessages(otherUid).collect { list ->
                latestMessages = list
                emitUi(list)
                repository.markConversationSeen(otherUid)
            }
        }
    }

    private fun emitUi(list: List<com.example.cookingeasy.domain.model.DirectMessage>) {
        val mapped = list.map { msg ->
            val isMine = msg.senderId != otherUid
            MessageUiModel(
                id = msg.id,
                isMine = isMine,
                avatarUrl = if (isMine) myAvatarUrl else otherAvatarUrl,
                contentType = when (msg.type) {
                    "image" -> MessageContentType.IMAGE
                    "attachment", "video", "voice" -> MessageContentType.ATTACHMENT
                    else -> MessageContentType.TEXT
                },
                text = msg.text,
                imageUrl = msg.imageUrl,
                attachmentName = when (msg.type) {
                    "video" -> msg.attachmentName.ifEmpty { "Video message" }
                    "voice" -> msg.attachmentName.ifEmpty { "Voice message" }
                    else -> msg.attachmentName
                },
                attachmentUrl = msg.attachmentUrl,
                attachmentSize = msg.attachmentSize,
                timeLabel = formatTime(msg.createdAt),
                sendStatus = if (isMine) {
                    if (otherHasSeenConversation) MessageSendStatus.SEEN else MessageSendStatus.SENT
                } else {
                    MessageSendStatus.SEEN
                }
            )
        }
        _uiState.value = _uiState.value.copy(
            messages = mapped,
            isOtherActive = otherIsOnline,
            otherLastActiveAt = otherLastActiveAt,
            error = null
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            repository.sendTextMessage(otherUid, text)
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Failed to send message") }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            storageDataSource.uploadChatImage(uri)
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.message ?: "Failed to upload image"
                    )
                }
                .onSuccess { imageUrl ->
                    repository.sendImageMessage(otherUid, imageUrl)
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                error = it.message ?: "Failed to send image message"
                            )
                        }
                    _uiState.value = _uiState.value.copy(loading = false)
                }
        }
    }

    fun sendAttachment(uri: Uri) {
        uploadAndSendAttachment(
            uri = uri,
            upload = { storageDataSource.uploadChatAttachment(uri) },
            send = { url, name, size -> repository.sendAttachmentMessage(otherUid, url, name, size) },
            failUpload = "Failed to upload attachment",
            failSend = "Failed to send attachment"
        )
    }

    fun sendVideo(uri: Uri) {
        uploadAndSendAttachment(
            uri = uri,
            upload = { storageDataSource.uploadChatVideo(uri) },
            send = { url, name, size -> repository.sendVideoMessage(otherUid, url, name, size) },
            failUpload = "Failed to upload video",
            failSend = "Failed to send video"
        )
    }

    fun sendVoice(uri: Uri) {
        uploadAndSendAttachment(
            uri = uri,
            upload = { storageDataSource.uploadChatVoice(uri) },
            send = { url, name, size -> repository.sendVoiceMessage(otherUid, url, name, size) },
            failUpload = "Failed to upload voice",
            failSend = "Failed to send voice"
        )
    }

    private fun uploadAndSendAttachment(
        uri: Uri,
        upload: suspend () -> Result<String>,
        send: suspend (String, String, String) -> Result<Unit>,
        failUpload: String,
        failSend: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val fileName = extractFileName(uri)
            val fileSize = extractFileSize(uri)
            upload()
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.message ?: failUpload
                    )
                }
                .onSuccess { url ->
                    send(url, fileName, fileSize)
                        .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: failSend) }
                    _uiState.value = _uiState.value.copy(loading = false)
                }
        }
    }

    private fun extractFileName(uri: Uri): String {
        val raw = uri.lastPathSegment.orEmpty()
        return if (raw.contains('/')) raw.substringAfterLast('/') else raw.ifEmpty { "file" }
    }

    private fun extractFileSize(uri: Uri): String {
        val size = runCatching { contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1L }.getOrDefault(-1L)
        if (size <= 0L) return ""
        return humanSize(size)
    }

    private fun humanSize(bytes: Long): String {
        val kb = bytes / 1024.0
        if (kb < 1024) return "${DecimalFormat("0.#").format(kb)} KB"
        val mb = kb / 1024.0
        return "${DecimalFormat("0.#").format(mb)} MB"
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "Now"
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    }

    companion object {
    }
}
