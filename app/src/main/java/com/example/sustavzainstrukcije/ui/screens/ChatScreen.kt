package com.example.sustavzainstrukcije.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.data.Message // Vaša Message data klasa [1]
import com.example.sustavzainstrukcije.ui.viewmodels.ChatViewModel // Vaš ChatViewModel [4]
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
    chatId: String,       // ID chata, generiran simetrično
    otherUserId: String,  // ID drugog korisnika u chatu
    navController: NavController, // NavController ostaje
    userViewModel: UserViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()   // ChatViewModel
) {
    val currentUser = Firebase.auth.currentUser // Trenutno prijavljeni korisnik
    // Opcionalno: dohvati podatke o drugom korisniku ako želiš prikazati ime
    val otherUserDetails by userViewModel.getUserById(otherUserId).collectAsState(initial = null)

    var newMessage by remember { mutableStateOf("") }
    val messages by chatViewModel.getMessages(chatId)
        .collectAsState(initial = emptyList())

    val listState = rememberLazyListState() // Za automatsko scrollanje na dno
    val coroutineScope = rememberCoroutineScope()

    // Automatsko scrollanje na dno kada se dodaju nove poruke
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 4.dp, bottom = 40.dp)) {
        // Opcionalno: Prikaz imena drugog korisnika na vrhu ekrana
        TopAppBar(title = { Text(otherUserDetails?.name ?: "Chat") })

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message -> // Nije potrebno sortirati ovdje ako getMessages već vraća sortiranu listu
                MessageBubble(
                    text = message.text,
                    isUser = message.senderId == currentUser?.uid,
                    senderId = message.senderId,
                    timestamp = message.timestamp
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
                placeholder = { Text("Napiši poruku...") },
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
                            )
                        )
                        newMessage = ""
                    }
                },
                enabled = newMessage.isNotBlank() && currentUser != null
            ) {
                Text("Pošalji")
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean, senderId: String, timestamp: Long) {
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
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
         Text(
             text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)),
             style = MaterialTheme.typography.bodySmall,
             color = Color.Gray,
             modifier = Modifier.padding(horizontal = 4.dp)
         )
    }
}

