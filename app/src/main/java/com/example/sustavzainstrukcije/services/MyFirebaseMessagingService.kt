package com.example.sustavzainstrukcije.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sustavzainstrukcije.MainActivity
import com.example.sustavzainstrukcije.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "new_message_channel"
        var currentActiveChatId: String? = null
        private val messageCache = mutableMapOf<String, MutableList<String>>()

        fun clearMessagesForUser(userId: String) {
            messageCache.remove(userId)
            Log.d(TAG, "Cleared message cache for user: $userId")
        }
    }


    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null && token != null) {

            val userDocRef = Firebase.firestore.collection("users").document(userId)
            userDocRef.update("fcmToken", token)
                .addOnSuccessListener { Log.d(TAG, "FCM token updated for user $userId") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")

        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "Nova poruka"
            val body = remoteMessage.data["body"] ?: ""
            val chatId = remoteMessage.data["chatId"]
            val otherUserId = remoteMessage.data["otherUserId"]
            val senderName = remoteMessage.data["senderName"] ?: "Netko"

            Log.d(TAG, "Extracted data - chatId: $chatId, otherUserId: $otherUserId")
            Log.d(TAG, "Current active chat: $currentActiveChatId")

            if (currentActiveChatId == chatId) {
                Log.d(TAG, "User is currently in this chat, not showing notification")
                return@let
            }

            if (otherUserId != null) {
                val userMessages = messageCache.getOrPut(otherUserId) { mutableListOf() }
                userMessages.add(body)

                if (userMessages.size > 5) {
                    userMessages.removeAt(0)
                }

                sendGroupedNotification(senderName, userMessages, chatId, otherUserId)
            }
        }
    }

    private fun sendGroupedNotification(
        senderName: String,
        messages: List<String>,
        chatId: String?,
        otherUserId: String?
    ) {
        Log.d(TAG, "Creating grouped notification for $senderName with ${messages.size} messages")

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "NOTIFICATION_CHAT_ACTION_${System.currentTimeMillis()}"
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            if (chatId != null && otherUserId != null) {
                putExtra("chatId", chatId)
                putExtra("otherUserId", otherUserId)
                putExtra("navigateTo", "ChatScreen")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            otherUserId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationId = otherUserId.hashCode()

        val inboxStyle = NotificationCompat.InboxStyle()

        messages.takeLast(5).forEach { message ->
            inboxStyle.addLine(message)
        }

        val messageCount = messages.size
        val bigTitle = if (messageCount > 1) {
            "$messageCount ${if (messageCount < 5) "poruke" else "poruka"} od $senderName"
        } else {
            "Nova poruka od $senderName"
        }

        inboxStyle.setBigContentTitle(bigTitle)

        if (messageCount > 1) {
            inboxStyle.setSummaryText("Kliknite za otvaranje chata")
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(bigTitle)
            .setContentText(messages.last())
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(System.currentTimeMillis())
            .setNumber(messageCount)
            .setGroup("chat_group_$otherUserId")
            .setOnlyAlertOnce(messageCount == 1)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nove Poruke",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Grouped notification sent with $messageCount messages")
    }


    private fun sendNotification(title: String, messageBody: String, chatId: String?, otherUserId: String?) {
        Log.d(TAG, "Creating notification with chatId: $chatId, otherUserId: $otherUserId")

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "NOTIFICATION_CHAT_ACTION_${System.currentTimeMillis()}"
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            if (chatId != null && otherUserId != null) {
                putExtra("chatId", chatId)
                putExtra("otherUserId", otherUserId)
                putExtra("navigateTo", "ChatScreen")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            otherUserId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val groupKey = "chat_group_$otherUserId"
        val notificationId = otherUserId.hashCode()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setWhen(System.currentTimeMillis())

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nove Poruke",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "Notification sent with ID: $notificationId, Group: $groupKey")
    }


}
