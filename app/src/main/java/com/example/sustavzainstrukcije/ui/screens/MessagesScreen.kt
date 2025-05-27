package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sustavzainstrukcije.ui.data.ChatInfo // Importaj novu klasu
import com.example.sustavzainstrukcije.ui.data.Message // [1]
import com.example.sustavzainstrukcije.ui.data.User // [2]
import com.example.sustavzainstrukcije.ui.viewmodels.UserViewModel // Pretpostavka da postoji
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
    // Sada koristimo listu ChatInfo objekata
    val chatInfos = remember { mutableStateListOf<ChatInfo>() }

    DisposableEffect(userId) {
        val databaseChatsRef = Firebase.database.reference.child("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatInfos.clear()
                val tempChatInfos = mutableListOf<ChatInfo>()

                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key
                    var isUserParticipant = false
                    var lastMessage: Message? = null

                    // Iteriraj kroz poruke da pronađeš zadnju i provjeriš sudjelovanje
                    chatSnapshot.child("messages").children.forEach { messageData ->
                        val message = messageData.getValue(Message::class.java)
                        if (message != null) {
                            if (message.senderId == userId || message.receiverId == userId) {
                                isUserParticipant = true
                            }
                            // Pronađi najnoviju poruku
                            if (lastMessage == null || message.timestamp > lastMessage!!.timestamp) {
                                lastMessage = message
                            }
                        }
                    }

                    if (isUserParticipant && chatId != null && lastMessage != null) {
                        // Odredi ID drugog korisnika
                        val otherUserId = if (lastMessage!!.senderId == userId) {
                            lastMessage!!.receiverId
                        } else {
                            lastMessage!!.senderId
                        }

                        if (otherUserId.isNotEmpty()) { // Osiguraj da otherUserId nije prazan
                            tempChatInfos.add(
                                ChatInfo(
                                    chatId = chatId,
                                    otherUserId = otherUserId,
                                    lastMessageTimestamp = lastMessage!!.timestamp,
                                    lastMessageText = lastMessage!!.text
                                )
                            )
                        }
                    }
                }
                // Sortiraj chatove po timestampu zadnje poruke, od najnovije do najstarije
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
        items(chatInfos) { chatInfo -> // Iteriraj kroz chatInfos
            ChatListItem(
                chatInfo = chatInfo, // Proslijedi cijeli ChatInfo objekt
                currentUserId = userId,
                navController = navController
            )
        }
    }
}


@Composable
fun ChatListItem(
    chatInfo: ChatInfo, // Primi ChatInfo objekt
    currentUserId: String,
    navController: NavHostController,
    userViewModel: UserViewModel = viewModel() // UserViewModel za dohvaćanje podataka o korisniku
) {
    // Dohvati podatke o drugom korisniku koristeći njegov ID iz chatInfo
    val otherUserDetails by userViewModel.getUserById(chatInfo.otherUserId).collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxWidth() // Neka zauzme cijelu širinu
            .clickable {
                // Navigiraj na ChatScreen s chatId i otherUserId
                navController.navigate("chat/${chatInfo.chatId}/${chatInfo.otherUserId}")
            }
            .padding(top = 30.dp, bottom = 16.dp).padding(horizontal = 16.dp)
    ) {
        Text(
            // Prikaz imena drugog korisnika ako je dostupno, inače njegov ID
            text = otherUserDetails?.name ?: "Korisnik ${chatInfo.otherUserId.take(6)}...",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        // Opcionalno: Prikaz zadnje poruke
        Text(
            text = chatInfo.lastMessageText.take(40) + if (chatInfo.lastMessageText.length > 40) "..." else "", // Prikaz dijela zadnje poruke
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Opcionalno: Prikaz vremena zadnje poruke
        Text(
            text = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(chatInfo.lastMessageTimestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
    HorizontalDivider()
}

