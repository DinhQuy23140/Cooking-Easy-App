package com.example.cookingeasy.data.repository

import com.example.cookingeasy.domain.model.ConversationSummary
import com.example.cookingeasy.domain.model.DirectMessage
import com.example.cookingeasy.domain.model.UserPresence
import com.example.cookingeasy.domain.repository.DirectChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DirectChatRepositoryImp : DirectChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun observeMessages(otherUid: String): Flow<List<DirectMessage>> = callbackFlow {
        val me = auth.currentUser?.uid.orEmpty()
        if (me.isEmpty() || otherUid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val conversationId = buildConversationId(me, otherUid)
        val registration = db.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .orderBy(KEY_CREATED_AT)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents.orEmpty().map { doc ->
                    DirectMessage(
                        id = doc.id,
                        senderId = doc.getString(KEY_SENDER_ID).orEmpty(),
                        receiverId = doc.getString(KEY_RECEIVER_ID).orEmpty(),
                        type = doc.getString(KEY_TYPE).orEmpty().ifEmpty { TYPE_TEXT },
                        text = doc.getString(KEY_TEXT).orEmpty(),
                        imageUrl = doc.getString(KEY_IMAGE_URL).orEmpty(),
                        attachmentUrl = doc.getString(KEY_ATTACHMENT_URL).orEmpty(),
                        attachmentName = doc.getString(KEY_ATTACHMENT_NAME).orEmpty(),
                        attachmentSize = doc.getString(KEY_ATTACHMENT_SIZE).orEmpty(),
                        createdAt = toMillis(doc.get(KEY_CREATED_AT))
                    )
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override fun observeConversations(): Flow<List<ConversationSummary>> = callbackFlow {
        val me = auth.currentUser?.uid.orEmpty()
        if (me.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection(COLLECTION_CONVERSATIONS)
            .whereArrayContains(KEY_PARTICIPANTS, me)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val participants = (doc.get(KEY_PARTICIPANTS) as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val otherUid = participants.firstOrNull { it != me } ?: return@mapNotNull null
                    val namesMap = (doc.get(KEY_PARTICIPANT_NAMES) as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            val key = k as? String ?: return@mapNotNull null
                            key to (v as? String).orEmpty()
                        }?.toMap().orEmpty()
                    val avatarsMap = (doc.get(KEY_PARTICIPANT_AVATARS) as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            val key = k as? String ?: return@mapNotNull null
                            key to (v as? String).orEmpty()
                        }?.toMap().orEmpty()
                    val seenBy = (doc.get(KEY_SEEN_BY) as? List<*>)?.filterIsInstance<String>().orEmpty()
                    ConversationSummary(
                        conversationId = doc.id,
                        otherUserId = otherUid,
                        otherUserName = namesMap[otherUid].orEmpty().ifEmpty { "Chef" },
                        otherUserAvatar = avatarsMap[otherUid].orEmpty(),
                        lastMessage = doc.getString(KEY_LAST_MESSAGE).orEmpty(),
                        lastSenderId = doc.getString(KEY_LAST_SENDER_ID).orEmpty(),
                        updatedAt = toMillis(doc.get(KEY_UPDATED_AT)),
                        unreadCount = if (seenBy.contains(me)) 0 else 1,
                        isSeenByMe = seenBy.contains(me)
                    )
                }.sortedByDescending { it.updatedAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override fun observeUserPresence(uid: String): Flow<UserPresence> = callbackFlow {
        if (uid.isBlank()) {
            trySend(UserPresence())
            close()
            return@callbackFlow
        }
        val registration = db.collection(COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, _ ->
                val lastActiveAt = toMillis(snapshot?.get(KEY_USER_LAST_ACTIVE_AT))
                val rawOnline = snapshot?.getBoolean(KEY_USER_IS_ONLINE) == true
                val isOnline = rawOnline && (System.currentTimeMillis() - lastActiveAt <= PRESENCE_STALE_MS)
                trySend(UserPresence(isOnline = isOnline, lastActiveAt = lastActiveAt))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getUsersPresence(uids: List<String>): Map<String, UserPresence> {
        if (uids.isEmpty()) return emptyMap()
        return runCatching {
            val map = mutableMapOf<String, UserPresence>()
            uids.distinct().chunked(10).forEach { chunk ->
                val snap = db.collection(COLLECTION_USERS)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                snap.documents.forEach { doc ->
                    val uid = doc.id
                    val lastActiveAt = toMillis(doc.get(KEY_USER_LAST_ACTIVE_AT))
                    val rawOnline = doc.getBoolean(KEY_USER_IS_ONLINE) == true
                    map[uid] = UserPresence(
                        isOnline = rawOnline && (System.currentTimeMillis() - lastActiveAt <= PRESENCE_STALE_MS),
                        lastActiveAt = lastActiveAt
                    )
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    override fun observeOtherUserSeen(otherUid: String): Flow<Boolean> = callbackFlow {
        val me = auth.currentUser?.uid.orEmpty()
        if (me.isEmpty() || otherUid.isEmpty()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val conversationId = buildConversationId(me, otherUid)
        val registration = db.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .addSnapshotListener { snapshot, _ ->
                val seenBy = (snapshot?.get(KEY_SEEN_BY) as? List<*>)?.filterIsInstance<String>().orEmpty()
                trySend(seenBy.contains(otherUid))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendTextMessage(otherUid: String, text: String): Result<Unit> = runCatching {
        sendMessageInternal(
            otherUid = otherUid,
            type = TYPE_TEXT,
            text = text,
            imageUrl = "",
            attachmentUrl = "",
            attachmentName = "",
            attachmentSize = ""
        )
    }

    override suspend fun sendImageMessage(otherUid: String, imageUrl: String): Result<Unit> = runCatching {
        sendMessageInternal(
            otherUid = otherUid,
            type = TYPE_IMAGE,
            text = "",
            imageUrl = imageUrl,
            attachmentUrl = "",
            attachmentName = "",
            attachmentSize = ""
        )
    }

    override suspend fun sendAttachmentMessage(
        otherUid: String,
        fileUrl: String,
        fileName: String,
        fileSize: String
    ): Result<Unit> = runCatching {
        sendMessageInternal(
            otherUid = otherUid,
            type = TYPE_ATTACHMENT,
            text = "",
            imageUrl = "",
            attachmentUrl = fileUrl,
            attachmentName = fileName,
            attachmentSize = fileSize
        )
    }

    override suspend fun sendVideoMessage(
        otherUid: String,
        fileUrl: String,
        fileName: String,
        fileSize: String
    ): Result<Unit> = runCatching {
        sendMessageInternal(
            otherUid = otherUid,
            type = TYPE_VIDEO,
            text = "",
            imageUrl = "",
            attachmentUrl = fileUrl,
            attachmentName = fileName,
            attachmentSize = fileSize
        )
    }

    override suspend fun sendVoiceMessage(
        otherUid: String,
        fileUrl: String,
        fileName: String,
        fileSize: String
    ): Result<Unit> = runCatching {
        sendMessageInternal(
            otherUid = otherUid,
            type = TYPE_VOICE,
            text = "",
            imageUrl = "",
            attachmentUrl = fileUrl,
            attachmentName = fileName,
            attachmentSize = fileSize
        )
    }

    private suspend fun sendMessageInternal(
        otherUid: String,
        type: String,
        text: String,
        imageUrl: String,
        attachmentUrl: String,
        attachmentName: String,
        attachmentSize: String
    ) {
        val me = auth.currentUser?.uid ?: error("Not logged in")
        val conversationId = buildConversationId(me, otherUid)
        val meProfile = db.collection(COLLECTION_USERS).document(me).get().await()
        val otherProfile = db.collection(COLLECTION_USERS).document(otherUid).get().await()
        val myName = meProfile.getString("fullName").orEmpty().ifEmpty { meProfile.getString("nickname").orEmpty() }
        val myAvatar = meProfile.getString("avatarUrl").orEmpty()
        val otherName = otherProfile.getString("fullName").orEmpty().ifEmpty { otherProfile.getString("nickname").orEmpty() }
        val otherAvatar = otherProfile.getString("avatarUrl").orEmpty()

        db.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .add(
                mapOf(
                    KEY_SENDER_ID to me,
                    KEY_RECEIVER_ID to otherUid,
                    KEY_TEXT to text,
                    KEY_TYPE to type,
                    KEY_IMAGE_URL to imageUrl,
                    KEY_ATTACHMENT_URL to attachmentUrl,
                    KEY_ATTACHMENT_NAME to attachmentName,
                    KEY_ATTACHMENT_SIZE to attachmentSize,
                    KEY_CREATED_AT to FieldValue.serverTimestamp()
                )
            ).await()

        db.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .set(
                mapOf(
                    KEY_PARTICIPANTS to listOf(me, otherUid),
                    KEY_PARTICIPANT_NAMES to mapOf(me to myName, otherUid to otherName),
                    KEY_PARTICIPANT_AVATARS to mapOf(me to myAvatar, otherUid to otherAvatar),
                    KEY_LAST_MESSAGE to when (type) {
                        TYPE_IMAGE -> "[Image]"
                        TYPE_ATTACHMENT -> "[Attachment]"
                        TYPE_VIDEO -> "[Video]"
                        TYPE_VOICE -> "[Voice]"
                        else -> text
                    },
                    KEY_LAST_SENDER_ID to me,
                    KEY_UPDATED_AT to FieldValue.serverTimestamp(),
                    KEY_SEEN_BY to listOf(me)
                )
            ).await()
    }

    override suspend fun markConversationSeen(otherUid: String): Result<Unit> = runCatching {
        val me = auth.currentUser?.uid ?: error("Not logged in")
        val conversationId = buildConversationId(me, otherUid)
        db.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .set(mapOf(KEY_SEEN_BY to FieldValue.arrayUnion(me)), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    private fun buildConversationId(uidA: String, uidB: String): String {
        return if (uidA <= uidB) "${uidA}_$uidB" else "${uidB}_$uidA"
    }

    private fun toMillis(raw: Any?): Long {
        return when (raw) {
            is com.google.firebase.Timestamp -> raw.toDate().time
            is Long -> raw
            is Number -> raw.toLong()
            else -> 0L
        }
    }

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_CONVERSATIONS = "conversations"
        private const val COLLECTION_MESSAGES = "messages"

        private const val KEY_PARTICIPANTS = "participants"
        private const val KEY_PARTICIPANT_NAMES = "participantNames"
        private const val KEY_PARTICIPANT_AVATARS = "participantAvatars"
        private const val KEY_LAST_MESSAGE = "lastMessage"
        private const val KEY_LAST_SENDER_ID = "lastSenderId"
        private const val KEY_UPDATED_AT = "updatedAt"
        private const val KEY_SEEN_BY = "seenBy"

        private const val KEY_SENDER_ID = "senderId"
        private const val KEY_RECEIVER_ID = "receiverId"
        private const val KEY_TEXT = "text"
        private const val KEY_TYPE = "type"
        private const val KEY_IMAGE_URL = "imageUrl"
        private const val KEY_ATTACHMENT_URL = "attachmentUrl"
        private const val KEY_ATTACHMENT_NAME = "attachmentName"
        private const val KEY_ATTACHMENT_SIZE = "attachmentSize"
        private const val KEY_CREATED_AT = "createdAt"
        private const val TYPE_TEXT = "text"
        private const val TYPE_IMAGE = "image"
        private const val TYPE_ATTACHMENT = "attachment"
        private const val TYPE_VIDEO = "video"
        private const val TYPE_VOICE = "voice"
        private const val KEY_USER_IS_ONLINE = "isOnline"
        private const val KEY_USER_LAST_ACTIVE_AT = "lastActiveAt"
        private const val PRESENCE_STALE_MS = 2 * 60 * 1000L
    }
}
