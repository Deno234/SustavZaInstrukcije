package com.example.sustavzainstrukcije.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
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


class ChatViewModel(private val db: DatabaseReference = Firebase.database.reference) : ViewModel() {

    fun sendMessage(chatId: String, message: Message) {
        val chatRef = db.child("chats").child(chatId)
        val messagesRef = chatRef.child("messages")
        chatRef.child("participants").child(message.senderId).setValue(true)
        chatRef.child("participants").child(message.receiverId).setValue(true)

        val messageToSend = message.copy (
            readBy = mapOf(message.senderId to true)
        )

        messagesRef.push().setValue(messageToSend)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Message written directly")
            }
            .addOnFailureListener {
                Log.e("ChatViewModel", "Direct write failed", it)
            }
    }


    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val messages = mutableListOf<Message>()
        val query = db.child("chats/$chatId/messages")

        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result.children.forEach { child ->
                    child.getValue(Message::class.java)?.let { message ->
                        if (!messages.contains(message)) {
                            messages.add(message)
                            trySend(messages.toList().sortedBy { it.timestamp })
                        }
                    }
                }
            } else {
                Log.e("ChatViewModel", "Error loading messages", task.exception)
            }
        }

        val listener = query.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prevKey: String?) {
                snapshot.getValue(Message::class.java)?.let { message ->
                    if (!messages.any { msg ->
                            msg.timestamp == message.timestamp && msg.senderId == message.senderId && msg.text == message.text
                        }) {
                        messages.add(message)
                    }
                    trySend(messages.toList().sortedBy { it.timestamp })
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(Message::class.java)?.let { updatedMessage ->
                    val index = messages.indexOfFirst { msg ->
                        msg.timestamp == updatedMessage.timestamp && msg.senderId == updatedMessage.senderId && msg.text == updatedMessage.text
                    }
                    if (index != -1) {
                        messages[index] = updatedMessage
                        trySend(messages.toList().sortedBy { it.timestamp })
                    }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) { /* ... */ }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { /* ... */ }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Listener cancelled", error.toException())
                close(error.toException())
            }
        })

        awaitClose { query.removeEventListener(listener) }
    }

    fun markMessagesAsRead(chatId: String, readerUserId: String) {
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
