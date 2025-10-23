package com.example.projectchat3.data.chats

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {
    private val storage = FirebaseStorage.getInstance()
    fun getOrCreateChat(participants: List<String>, onResult: (String?) -> Unit) {
        // Sắp xếp ID để truy vấn cho đúng
        val sortedIds = participants.sorted()
        val chatId = "${sortedIds[0]}_${sortedIds[1]}"

        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    //Chats đã tồn tại, trả về ID
                    onResult(doc.id)
                } else {
                    //Chưa có -> tạo mới với ID đã được chuẩn hóa
                    val newChat = mapOf(
                        "participants" to participants,
                        "lastMessage" to "",
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("chats").document(chatId).set(newChat)
                        .addOnSuccessListener { onResult(chatId) } // Trả về ID vừa tạo
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
    fun uploadMediaToStorage(
        uri: Uri,
        chatId: String,
        folder: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("$folder/$chatId/${System.currentTimeMillis()}_${uri.lastPathSegment}")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { url ->
                    onSuccess(url.toString())
                }
            }
            .addOnFailureListener(onFailure)
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

    fun sendImageMessage(
        roomId: String,
        imageUri: Uri,
        senderId: String,
        participants: List<String>,
        onResult: (Boolean) -> Unit
    ) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("chat_media/$roomId/$fileName")

        // 1. Upload ảnh lên Storage
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                // 2. Lấy URL của ảnh vừa upload
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // 3. Tạo một tin nhắn mới chứa URL ảnh
                    val message = Message(
                        senderId = senderId,
                        imageUrl = downloadUrl.toString(),
                        text = "" // Tin nhắn ảnh không có text
                    )
                    // 4. Gửi tin nhắn đó đi
                    sendMessage(roomId, message, participants, onResult)
                }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

}
