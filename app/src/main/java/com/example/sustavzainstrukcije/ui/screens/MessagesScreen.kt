package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Composable
fun MessagesScreen(userId: String, navController: NavHostController) {
    val chatIds = remember { mutableStateListOf<String>() }

    // DisposableEffect se koristi za dodavanje i uklanjanje listenera
    DisposableEffect(userId) {
        val databaseChatsRef = Firebase.database.reference.child("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatIds.clear()
                // Iteriraj kroz svaki chat
                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key // ID chata
                    if (chatId != null) {
                        var userInChat = false
                        //Provjera da li je korisnik sudionik chata
                        chatSnapshot.child("messages").children.forEach { messageSnapshot ->
                            val message = messageSnapshot.getValue(Message::class.java)
                            if (message != null && (message.senderId == userId || message.receiverId == userId)) {
                                userInChat = true
                            }
                        }
                        if (userInChat && !chatIds.contains(chatId)) {
                            chatIds.add(chatId)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("com.example.sustavzainstrukcije.ui.screens.MessagesScreen", "loadChats:onCancelled", databaseError.toException())
            }
        }
        databaseChatsRef.addValueEventListener(listener)

        onDispose {
            databaseChatsRef.removeEventListener(listener)
        }
    }

    LazyColumn {
        items(chatIds) { chatId ->
            ChatListItem(chatId = chatId, userId = userId, navController = navController)
        }
    }
}

@Composable
fun ChatListItem(chatId: String, userId: String, navController: NavHostController) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .clickable {
                navController.navigate("chat/$chatId/$userId")
            }
    ) {
        Text(
            text = "Chat ID: ${chatId.take(8)}...", // Prikaz prvih 8 znakova ID-a chata
            fontWeight = FontWeight.Bold
        )
        // Ovdje možeš dodati prikaz zadnje poruke ili informacije o drugom sudioniku
    }
    HorizontalDivider()
}
