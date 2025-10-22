package com.example.projectchat3.data.chats

object ChatUtils {
    fun generateRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }
}
