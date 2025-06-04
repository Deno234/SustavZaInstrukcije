package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.sustavzainstrukcije.ui.data.Message
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn


class ChatViewModel(private val db: DatabaseReference = Firebase.database.reference) : ViewModel() {

    private val messagesCache = mutableMapOf<String, Flow<List<Message>>>()
    private val markedChats = mutableSetOf<String>()

    fun sendMessage(chatId: String, message: Message) {
        Log.d("ChatViewModel", "Sending message to chat: $chatId")
        val chatRef = db.child("chats").child(chatId)
        val messagesRef = chatRef.child("messages")

        val messageToSend = message.copy(
            readBy = mapOf(message.senderId to true)
        )

        messagesRef.push().setValue(messageToSend)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Message sent successfully")
            }
            .addOnFailureListener {
                Log.e("ChatViewModel", "Failed to send message", it)
            }
    }


    fun getMessages(chatId: String): Flow<List<Message>> {
        return messagesCache.getOrPut(chatId) {
            Log.d("ChatViewModel", "Creating new message flow for chatId: $chatId")
            callbackFlow {
                val messages = mutableListOf<Message>()
                val query = db.child("chats/$chatId/messages").orderByChild("timestamp")

                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("ChatViewModel", "Initial load for chat: $chatId, count: ${snapshot.childrenCount}")
                        messages.clear()
                        snapshot.children.forEach { child ->
                            child.getValue(Message::class.java)?.let { message ->
                                messages.add(message)
                            }
                        }
                        trySend(messages.sortedBy { it.timestamp })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                })

                val childListener = query.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        snapshot.getValue(Message::class.java)?.let { newMessage ->
                            if (!messages.any { it.timestamp == newMessage.timestamp && it.senderId == newMessage.senderId }) {
                                Log.d("ChatViewModel", "New message added to chat: $chatId")
                                messages.add(newMessage)
                                trySend(messages.sortedBy { it.timestamp })
                            }
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        snapshot.getValue(Message::class.java)?.let { updatedMessage ->
                            val index = messages.indexOfFirst {
                                it.timestamp == updatedMessage.timestamp && it.senderId == updatedMessage.senderId
                            }
                            if (index != -1) {
                                messages[index] = updatedMessage
                                trySend(messages.sortedBy { it.timestamp })
                            }
                        }
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        snapshot.getValue(Message::class.java)?.let { removedMessage ->
                            messages.removeAll {
                                it.timestamp == removedMessage.timestamp && it.senderId == removedMessage.senderId
                            }
                            trySend(messages.sortedBy { it.timestamp })
                        }
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                })

                awaitClose {
                    Log.d("ChatViewModel", "Removing listeners for chatId: $chatId")
                    query.removeEventListener(childListener)
                }
            }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
        }
    }


    fun markMessagesAsRead(chatId: String, readerUserId: String) {
        val cacheKey = "${chatId}_${readerUserId}"

        if (markedChats.contains(cacheKey)) {
            Log.d("ChatViewModel", "Messages already marked as read for $cacheKey")
            return
        }

        Log.d("ChatViewModel", "Marking messages as read for $cacheKey")
        markedChats.add(cacheKey)

        val messagesRef = db.child("chats").child(chatId).child("messages")

        messagesRef.orderByChild("receiverId").equalTo(readerUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { messageSnapshot ->
                        val message = messageSnapshot.getValue(Message::class.java)
                        val messageKey = messageSnapshot.key

                        if (message != null && messageKey != null && message.receiverId == readerUserId) {
                            val isAlreadyReadByReader = message.readBy[readerUserId] == true
                            if (!isAlreadyReadByReader) {
                                messagesRef.child(messageKey).child("readBy").child(readerUserId).setValue(true)
                                    .addOnSuccessListener {
                                        Log.d("ChatViewModel", "Message $messageKey marked as read by $readerUserId")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ChatViewModel", "Failed to mark message $messageKey as read", e)
                                    }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error marking messages as read", error.toException())
                }
            })
    }
}
