package com.example.projectchat3.data.chats

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChatRepository(private val db: FirebaseFirestore) {

    fun sendMessage(roomId: String, message: Message, onResult: (Boolean) -> Unit) {
        val docRef = db.collection("test")
            .document(roomId)
            .collection("messages")
            .document()

        // ✅ gán roomId vào message trước khi lưu
        val msgWithId = message.copy(id = docRef.id, roomId = roomId)

        docRef.set(msgWithId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }

        // cập nhật lastMessage trong Chat document
        val chat = mapOf("lastMessage" to msgWithId)
        db.collection("test").document(roomId).set(chat, SetOptions.merge())
    }

    fun listenMessages(roomId: String, onChange: (List<Message>) -> Unit) {
        db.collection("test")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, _ ->
                val msgs = snapshots?.map { it.toObject(Message::class.java) } ?: emptyList()
                onChange(msgs)
            }
    }

    fun updateMessage(roomId: String, messageId: String, newText: String, onResult: (Boolean) -> Unit) {
        db.collection("test")
            .document(roomId)
            .collection("messages")
            .document(messageId)
            .update("text", newText)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteMessage(roomId: String, messageId: String, onResult: (Boolean) -> Unit) {
        db.collection("test")
            .document(roomId)
            .collection("messages")
            .document(messageId)
            .update(
                mapOf(
                    "text" to "[Tin nhắn đã bị xóa]",
                    "deleted" to true
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
