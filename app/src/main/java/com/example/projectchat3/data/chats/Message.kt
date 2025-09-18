package com.example.projectchat3.data.chats

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val text: String = "",
    val deleted: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)

