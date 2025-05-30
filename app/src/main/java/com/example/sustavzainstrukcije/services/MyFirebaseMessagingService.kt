package com.example.sustavzainstrukcije.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
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
            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Nova poruka"
            val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
            val chatId = remoteMessage.data["chatId"]
            val otherUserId = remoteMessage.data["otherUserId"]

            Log.d(TAG, "Extracted data - chatId: $chatId, otherUserId: $otherUserId")
            sendNotification(title, body, chatId, otherUserId)
        }
    }

    private fun sendNotification(title: String, messageBody: String, chatId: String?, otherUserId: String?) {

        Log.d(TAG, "Creating notification with chatId: $chatId, otherUserId: $otherUserId")

        val intent = Intent(this, MainActivity::class.java).apply {
            // KLJUČNA PROMJENA: Dodaj jedinstvenu ACTION
            action = "NOTIFICATION_CHAT_ACTION_${System.currentTimeMillis()}"
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            if (chatId != null && otherUserId != null) {
                putExtra("chatId", chatId)
                putExtra("otherUserId", otherUserId)
                putExtra("navigateTo", "ChatScreen")
                Log.d(TAG, "Added extras to intent: chatId=$chatId, otherUserId=$otherUserId")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nove Poruke",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build()) // Koristi jedinstveni ID
    }

}
