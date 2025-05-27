package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.ChatInfo
import com.example.sustavzainstrukcije.ui.data.Message
import com.example.sustavzainstrukcije.ui.viewmodels.UserViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(userId: String, navController: NavHostController) {
    val chatInfos = remember { mutableStateListOf<ChatInfo>() }

    DisposableEffect(userId) {
        val databaseChatsRef = Firebase.database.reference.child("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempChatInfos = mutableListOf<ChatInfo>()

                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key
                    var isUserParticipant = false
                    var lastMessage: Message? = null
                    var unreadMessagesCount = 0

                    chatSnapshot.child("messages").children.forEach { messageData ->
                        val message = messageData.getValue(Message::class.java)
                        if (message != null) {
                            if (message.senderId == userId || message.receiverId == userId) {
                                isUserParticipant = true
                            }
                            if (lastMessage == null || message.timestamp > lastMessage!!.timestamp) {
                                lastMessage = message
                            }
                            // Provjeri je li poruka nepročitana za trenutnog korisnika
                            if (message.receiverId == userId && message.readBy[userId] != true) {
                                unreadMessagesCount++
                            }
                        }
                    }

                    if (isUserParticipant && chatId != null && lastMessage != null) {
                        val otherUserId = if (lastMessage!!.senderId == userId) {
                            lastMessage!!.receiverId
                        } else {
                            lastMessage!!.senderId
                        }

                        if (otherUserId.isNotEmpty()) {
                            tempChatInfos.add(
                                ChatInfo(
                                    chatId = chatId,
                                    otherUserId = otherUserId,
                                    lastMessageTimestamp = lastMessage!!.timestamp,
                                    lastMessageText = lastMessage!!.text,
                                    unreadCount = unreadMessagesCount
                                )
                            )
                        }
                    }
                }
                chatInfos.clear()
                chatInfos.addAll(tempChatInfos.sortedByDescending { it.lastMessageTimestamp })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("MessagesScreen", "loadChats:onCancelled", databaseError.toException())
            }
        }
        databaseChatsRef.addValueEventListener(listener)

        onDispose {
            databaseChatsRef.removeEventListener(listener)
        }
    }

    LazyColumn {
        items(chatInfos) { chatInfo ->
            ChatListItem(
                chatInfo = chatInfo,
                currentUserId = userId,
                navController = navController
            )
        }
    }
}



@Composable
fun ChatListItem(
    chatInfo: ChatInfo,
    currentUserId: String,
    navController: NavHostController,
    userViewModel: UserViewModel = viewModel()
) {
    val otherUserDetails by userViewModel.getUserById(chatInfo.otherUserId).collectAsState(initial = null)

    val titleFontWeight = if (chatInfo.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
    val lastMessageFontWeight = if (chatInfo.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
    val lastMessageColor = if (chatInfo.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("chat/${chatInfo.chatId}/${chatInfo.otherUserId}")
            }
            .padding(top = 20.dp).padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = otherUserDetails?.name ?: "Korisnik ${chatInfo.otherUserId.take(6)}...",
                fontWeight = titleFontWeight,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = chatInfo.lastMessageText.take(30) + if (chatInfo.lastMessageText.length > 30) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = lastMessageFontWeight,
                color = lastMessageColor
            )
            Text(
                text = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(chatInfo.lastMessageTimestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (chatInfo.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chatInfo.unreadCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    HorizontalDivider()
}

