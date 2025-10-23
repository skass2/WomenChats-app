package com.example.projectchat3.data.chats

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",              // ID của tin nhắn (Firestore documentId)
    val senderId: String = "",     // Email người gửi (optional cho Firestore)
    var text: String = "",            // Nội dung tin nhắn
    val imageUrl: String? = null, // Link ảnh, có thể null
    val videoUrl: String? = null, // Link video, có thể null
    var deleted: Boolean = false,     // Đánh dấu đã xoá hay chưa
    val timestamp: Timestamp = Timestamp.now() // Thời điểm gửi
)
