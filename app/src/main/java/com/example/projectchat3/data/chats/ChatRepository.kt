package com.example.projectchat3.data.chats

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChatRepository(private val db: FirebaseFirestore) {
    fun getOrCreateChat(participants: List<String>, onResult: (String?) -> Unit) {
        db.collection("chats")
            .whereArrayContains("participants", participants[0])
            .get()
            .addOnSuccessListener { snapshot ->
                val existingChat = snapshot.documents.find { doc ->
                    val users = doc.get("participants") as? List<*>
                    users != null && users.containsAll(participants)
                }

                if (existingChat != null) {
                    // ✅ Chat đã tồn tại
                    onResult(existingChat.id)
                } else {
                    // ❌ Chưa có -> tạo mới
                    val newChatRef = db.collection("chats").document()
                    val newChat = mapOf(
                        "participants" to participants,
                        "lastMessage" to "",
                        "updatedAt" to System.currentTimeMillis()
                    )
                    newChatRef.set(newChat)
                        .addOnSuccessListener { onResult(newChatRef.id) }
                        .addOnFailureListener { onResult(null) }
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun sendMessage(roomId: String, message: Message, participants: List<String>, onResult: (Boolean) -> Unit) {
        val docRef = db.collection("chats")
            .document(roomId)
            .collection("messages")
            .document()

        val msgWithId = message.copy(id = docRef.id)

        docRef.set(msgWithId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }

        val chatUpdate = mapOf(
            "participants" to participants,
            "lastMessage" to msgWithId.text,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("chats").document(roomId).set(chatUpdate, SetOptions.merge())
    }


    fun listenMessages(roomId: String, onChange: (List<Message>) -> Unit) {
        db.collection("chats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, _ ->
                val msgs = snapshots?.map { it.toObject(Message::class.java) } ?: emptyList()
                onChange(msgs)
            }
    }

    fun updateMessage(roomId: String, messageId: String, newText: String, onResult: (Boolean) -> Unit) {
        db.collection("chats")
            .document(roomId)
            .collection("messages")
            .document(messageId)
            .update("text", newText)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteMessage(roomId: String, messageId: String, onResult: (Boolean) -> Unit) {
        db.collection("chats")
            .document(roomId)
            .collection("messages")
            .document(messageId)
            .update(
                mapOf(
                    "text" to "",        // để trống text
                    "deleted" to true    // đánh dấu đã xoá
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

}
