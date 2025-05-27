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


class ChatViewModel(private val db: DatabaseReference = Firebase.database.reference) : ViewModel() {

    fun sendMessage(chatId: String, message: Message) {
        val chatRef = db.child("chats").child(chatId)
        chatRef.child("participants").child(message.senderId).setValue(true)
        chatRef.child("participants").child(message.receiverId).setValue(true)

        chatRef.child("messages").push().setValue(message)
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

        val listener = query
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, prevKey: String?) {
                    snapshot.getValue(Message::class.java)?.let { message ->
                        messages.add(message)
                        trySend(messages.toList().sortedBy { it.timestamp })
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    TODO("Not yet implemented")
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Log.d("ChatViewModel", "onChildRemoved: Poruka obrisana (key: ${snapshot.key}), (value: ${snapshot.value})")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    TODO("Not yet implemented")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Listener cancelled", error.toException())
                    close(error.toException())
                }
            })

        awaitClose { query.removeEventListener(listener) }
    }
}
