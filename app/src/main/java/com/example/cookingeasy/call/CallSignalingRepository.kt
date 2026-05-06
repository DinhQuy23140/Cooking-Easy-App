package com.example.cookingeasy.call

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CallSignalingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun listenCall(callId: String): Flow<CallSession?> = callbackFlow {
        if (callId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection(CALLS).document(callId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                trySend(
                    CallSession(
                        callId = snapshot.getString("callId").orEmpty().ifEmpty { snapshot.id },
                        callerId = snapshot.getString("callerId").orEmpty(),
                        receiverId = snapshot.getString("receiverId").orEmpty(),
                        type = snapshot.getString("type").orEmpty(),
                        status = snapshot.getString("status").orEmpty(),
                        createdAt = (snapshot.getLong("createdAt") ?: 0L),
                        updatedAt = (snapshot.getLong("updatedAt") ?: 0L),
                    )
                )
            }

        awaitClose { registration.remove() }
    }

    fun listenSignaling(callId: String): Flow<List<SignalingMessage>> = callbackFlow {
        if (callId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection(CALLS)
            .document(callId)
            .collection(SIGNALING)
            .orderBy("createdAt")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents.orEmpty().map { doc ->
                    SignalingMessage(
                        id = doc.id,
                        type = doc.getString("type").orEmpty(),
                        senderId = doc.getString("senderId").orEmpty(),
                        targetId = doc.getString("targetId").orEmpty(),
                        sdp = doc.getString("sdp").orEmpty(),
                        candidate = doc.getString("candidate").orEmpty(),
                        sdpMid = doc.getString("sdpMid").orEmpty(),
                        sdpMLineIndex = (doc.getLong("sdpMLineIndex") ?: 0L).toInt(),
                        seq = doc.getLong("seq") ?: 0L,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }
                trySend(list)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendOffer(callId: String, senderId: String, targetId: String, sdp: String) {
        addSignaling(callId, mapOf(
            "type" to "offer",
            "senderId" to senderId,
            "targetId" to targetId,
            "sdp" to sdp
        ))
    }

    suspend fun sendAnswer(callId: String, senderId: String, targetId: String, sdp: String) {
        addSignaling(callId, mapOf(
            "type" to "answer",
            "senderId" to senderId,
            "targetId" to targetId,
            "sdp" to sdp
        ))
    }

    suspend fun sendCandidate(
        callId: String,
        senderId: String,
        targetId: String,
        candidate: String,
        sdpMid: String,
        sdpMLineIndex: Int
    ) {
        addSignaling(callId, mapOf(
            "type" to "candidate",
            "senderId" to senderId,
            "targetId" to targetId,
            "candidate" to candidate,
            "sdpMid" to sdpMid,
            "sdpMLineIndex" to sdpMLineIndex
        ))
    }

    private suspend fun addSignaling(callId: String, payload: Map<String, Any>) {
        if (callId.isBlank()) return

        db.collection(CALLS)
            .document(callId)
            .collection(SIGNALING)
            .add(payload + mapOf("createdAt" to System.currentTimeMillis()))
            .await()
    }

    companion object {
        private const val CALLS = "calls"
        private const val SIGNALING = "signaling"
    }
}
