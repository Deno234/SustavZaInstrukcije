package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sustavzainstrukcije.ui.viewmodels.AuthViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.ChatViewModel
import com.example.sustavzainstrukcije.ui.viewmodels.InstructorsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.sustavzainstrukcije.ui.data.Message

@Composable
fun StudentChatScreen(
    instructorId: String,
    navController: NavController,
    instructorsViewModel: InstructorsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val instructor by instructorsViewModel.checkedInstructor.collectAsState()
    val user by authViewModel.userData.collectAsState()

    val chatViewModel: ChatViewModel = viewModel()
    val currentUser = Firebase.auth.currentUser
    var newMessage by remember { mutableStateOf("") }
    val messages by chatViewModel.getMessages("${currentUser?.uid}_$instructorId")
        .collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        instructorsViewModel.fetchCheckedInstructor(instructorId)
        authViewModel.fetchCurrentUserData()
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            items(messages.sortedBy { it.timestamp }) { message ->
                MessageBubble(
                    text = message.text,
                    isUser = message.senderId == currentUser?.uid
                )
            }
        }

        Row(Modifier.padding(24.dp)) {
            TextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                Log.d("StudentChatScreen", "Current user: ${currentUser?.uid}")
                chatViewModel.sendMessage(
                    instructorId = instructorId,
                    chatId = "${currentUser?.uid}_$instructorId",
                    message = Message(
                        newMessage,
                        currentUser?.uid ?: "",
                        System.currentTimeMillis()
                    )
                )
                newMessage = ""
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp).let {
            if (isUser) it.copy(bottomEnd = CornerSize(0.dp))
            else it.copy(bottomStart = CornerSize(0.dp))
        },
        color = if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}