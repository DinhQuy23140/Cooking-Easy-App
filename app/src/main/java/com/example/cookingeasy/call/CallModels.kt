package com.example.cookingeasy.call

data class CallSession(
    val callId: String,
    val callerId: String,
    val receiverId: String,
    val type: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class SignalingMessage(
    val id: String = "",
    val type: String,
    val senderId: String,
    val targetId: String = "",
    val sdp: String = "",
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val seq: Long = 0L,
    val createdAt: Long = 0L
)
