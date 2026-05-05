package com.example.cookingeasy.data.remote.firebase.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cookingeasy.R
import com.example.cookingeasy.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
        val title = data["title"] ?: message.notification?.title ?: getString(R.string.chat_notification_title_default)
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
        // TODO: send token to your backend / Firestore for targeted push.
    }

    private fun showChatNotification(
        title: String,
        body: String,
        otherUid: String,
        otherName: String,
        otherAvatar: String
    ) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.chat_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.chat_notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "chat_messages_channel"
        private const val TAG = "ChatFCM"
    }
}
