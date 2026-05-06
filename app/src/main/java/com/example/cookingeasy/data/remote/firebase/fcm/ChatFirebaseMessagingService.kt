package com.example.cookingeasy.data.remote.firebase.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cookingeasy.R
import com.example.cookingeasy.call.IncomingCallActivity
import com.example.cookingeasy.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL
import kotlin.random.Random

class ChatFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "Service created")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.e(
            TAG,
            "FCM onMessageReceived: from=${message.from}, messageId=${message.messageId}, sentTime=${message.sentTime}"
        )
        Log.e(TAG, "FCM data payload: ${message.data}")
        Log.e(
            TAG,
            "FCM notification payload: title=${message.notification?.title}, body=${message.notification?.body}"
        )

        val data = message.data
        if (data["type"] == "incoming_call") {
            showIncomingCallNotification(data)
            return
        }
        if (data["type"] == "call_status") {
            showCallStatusNotification(data)
            return
        }

        val fallbackTitle = message.notification?.title ?: getString(R.string.chat_notification_title_default)
        val title = data["otherName"]?.takeIf { it.isNotBlank() } ?: data["title"] ?: fallbackTitle
        val body = data["body"] ?: message.notification?.body ?: getString(R.string.chat_notification_body_default)
        val otherUid = data["otherUid"].orEmpty()
        val otherName = data["otherName"].orEmpty()
        val otherAvatar = data["otherAvatar"].orEmpty()

        showChatNotification(
            title = title,
            body = body,
            otherUid = otherUid,
            otherName = otherName,
            otherAvatar = otherAvatar
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e(TAG, "FCM new token: $token")
        syncFcmToken(token)
    }

    private fun showChatNotification(
        title: String,
        body: String,
        otherUid: String,
        otherName: String,
        otherAvatar: String
    ) {
        ensureChannel()
        val managerCompat = NotificationManagerCompat.from(this)
        if (!managerCompat.areNotificationsEnabled()) {
            Log.e(TAG, "Notification blocked: app notifications disabled in system settings")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.e(TAG, "Notification blocked: POST_NOTIFICATIONS not granted")
                return
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
            putExtra(MainActivity.EXTRA_CHAT_UID, otherUid)
            putExtra(MainActivity.EXTRA_CHAT_NAME, otherName)
            putExtra(MainActivity.EXTRA_CHAT_AVATAR, otherAvatar)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            otherUid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_person)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        loadAvatarBitmap(otherAvatar)?.let { bitmap ->
            notification.largeIcon = bitmap
        }

        val notificationId = Random.nextInt()
        managerCompat.notify(notificationId, notification)
        Log.e(TAG, "Notification posted: id=$notificationId, title=$title, otherUid=$otherUid")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            Log.e(TAG, "Notification channel existing: id=$CHANNEL_ID, importance=${existing.importance}")
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.chat_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.chat_notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun showIncomingCallNotification(data: Map<String, String>) {
        ensureChannel()
        val managerCompat = NotificationManagerCompat.from(this)
        if (!managerCompat.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val callId = data["callId"].orEmpty()
        val callerId = data["callerId"].orEmpty()
        val callerName = data["callerName"].orEmpty().ifBlank { "Incoming call" }
        val callType = data["callType"].orEmpty().ifBlank { "audio" }

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(IncomingCallActivity.EXTRA_CALLER_ID, callerId)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, callerName)
            putExtra(IncomingCallActivity.EXTRA_CALL_TYPE, callType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_person)
            .setContentTitle(callerName)
            .setContentText("Incoming $callType call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        managerCompat.notify(callId.hashCode(), notification)
    }

    private fun syncFcmToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank() || token.isBlank()) return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "fcm_tokens" to FieldValue.arrayUnion(token)
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { Log.e(TAG, "Service synced fcm token for uid=$uid") }
            .addOnFailureListener { e -> Log.e(TAG, "Service failed to sync fcm token", e) }
    }

    private fun showCallStatusNotification(data: Map<String, String>) {
        ensureChannel()
        val managerCompat = NotificationManagerCompat.from(this)
        if (!managerCompat.areNotificationsEnabled()) return

        val callId = data["callId"].orEmpty()
        val status = data["status"].orEmpty().ifBlank { "updated" }
        val text = when (status) {
            "accepted" -> "Receiver accepted the call"
            "rejected" -> "Receiver rejected the call"
            "ended" -> "Call ended"
            else -> "Call status updated"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_person)
            .setContentTitle("Call")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        managerCompat.notify(("call-status-$callId").hashCode(), notification)
    }

    private fun loadAvatarBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null

        return runCatching {
            URL(url).openStream().use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to load avatar for notification", e)
        }.getOrNull()
    }

    companion object {
        private const val CHANNEL_ID = "chat_messages_channel"
        private const val TAG = "ChatFCM"
    }
}
