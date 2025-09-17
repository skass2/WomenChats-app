package com.example.projectchat3.data.chats

data class Chat(
    val roomId: String = "",
    val participants: List<String> = emptyList(), // chứa 2 uid
    val lastMessage: Message? = null
)