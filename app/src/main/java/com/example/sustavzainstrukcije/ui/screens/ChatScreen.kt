package com.example.sustavzainstrukcije.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.services.MyFirebaseMessagingService
import com.example.sustavzainstrukcije.ui.data.Message
import com.example.sustavzainstrukcije.ui.viewmodels.ChatViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.UserViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    otherUserId: String,
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current

    DisposableEffect(chatId) {
        MyFirebaseMessagingService.currentActiveChatId = chatId

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(otherUserId.hashCode())

        MyFirebaseMessagingService.clearMessagesForUser(otherUserId)

        onDispose {
            MyFirebaseMessagingService.currentActiveChatId = null
            Log.d("ChatScreen", "Cleared active chat")
        }
    }

    val currentUser = Firebase.auth.currentUser
    val otherUserDetails by userViewModel.getUserById(otherUserId).collectAsState(initial = null)

    var newMessage by remember { mutableStateOf("") }
    val messages by chatViewModel.getMessages(chatId)
        .collectAsState(initial = emptyList())

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentUser?.uid?.let { currentUserId ->
            if (currentUserId.isNotEmpty() && chatId.isNotEmpty()) {
                Log.d("ChatScreen", "Marking messages as read on resume for chat: $chatId")
                chatViewModel.markMessagesAsRead(chatId, currentUserId)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 40.dp)
    ) {
        TopAppBar(title = { Text(otherUserDetails?.name ?: "Chat s ${otherUserId.take(6)}") })

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = messages,
                key = { message -> "${message.timestamp}_${message.senderId}_${message.text.hashCode()}" }
            ) { message ->
                val lastSentMessageByCurrentUser = messages.lastOrNull { it.senderId == currentUser?.uid }
                val isTheVeryLastMessageByCurrentUserAndRead = lastSentMessageByCurrentUser == message && message.readBy[message.receiverId] == true

                MessageBubble(
                    message = message,
                    isUser = message.senderId == currentUser?.uid,
                    showSeenIndicator = isTheVeryLastMessageByCurrentUserAndRead
                )
            }
        }

        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a message...") },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newMessage.isNotBlank() && currentUser != null) {
                        Log.d("ChatScreen", "Sending message. Current user: ${currentUser.uid}, Other user: $otherUserId, Chat ID: $chatId")
                        chatViewModel.sendMessage(
                            chatId = chatId,
                            message = Message(
                                text = newMessage.trim(),
                                senderId = currentUser.uid,
                                receiverId = otherUserId,
                                timestamp = System.currentTimeMillis()
                            ),
                        )
                        newMessage = ""
                    }
                },
                enabled = newMessage.isNotBlank() && currentUser != null
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isUser: Boolean, showSeenIndicator: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp).let {
                if (isUser) it.copy(bottomEnd = CornerSize(0.dp))
                else it.copy(bottomStart = CornerSize(0.dp))
            },
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (isUser && showSeenIndicator) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Seen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
