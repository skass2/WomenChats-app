package com.example.projectchat3.data.chats

data class Chat(
    val cid: String = "",
    val participants: List<String> = listOf(),
    val lastMessage: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
