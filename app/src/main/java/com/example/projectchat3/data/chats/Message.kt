package com.example.projectchat3.data.chats

data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val text: String = "",
    val deleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun displayText(): String = if (deleted) "[Tin nhắn đã bị xóa]" else text

    fun formatTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy")
        return sdf.format(java.util.Date(timestamp))
    }

    fun markAsDeleted(): Message = copy(
        text = "[Tin nhắn đã bị xóa]",
        deleted = true
    )
}

