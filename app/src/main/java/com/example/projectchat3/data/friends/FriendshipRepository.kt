package com.example.projectchat3.data.friends

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FriendshipRepository(private val db: FirebaseFirestore) {

    fun sendRequest(currentUid: String, friendUid: String, onComplete: (Boolean) -> Unit) {
        val ids = listOf(currentUid, friendUid).sorted()
        val docId = "${ids[0]}_${ids[1]}"

        val friendshipData = hashMapOf(
            "participants" to listOf(currentUid, friendUid),
            "requestBy" to currentUid,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("friendships").document(docId)
            .set(friendshipData)
            .addOnSuccessListener {
                Log.d("FriendRepo", "✅ Gửi lời mời thành công, docId: $docId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ Gửi lời mời thất bại: ${e.message}")
                onComplete(false)
            }
    }

    // --- HÀM QUAN TRỌNG CẦN SỬA LÀ HÀM NÀY ---
    suspend fun acceptRequest(fid: String, currentUid: String, otherUid: String) {
        val friendshipRef = db.collection("friendships").document(fid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(friendshipRef)
            if (snapshot.getString("status") == "pending") {
                transaction.update(friendshipRef, "status", "accepted")
            } else {
                throw Exception("Friendship not pending")
            }
        }.await()

        // --- BẮT ĐẦU LOGIC TẠO CHAT ID CHUẨN ---
        val participants = listOf(currentUid, otherUid)
        // 1. Sắp xếp 2 UID để tạo ra ID nhất quán
        val sortedIds = participants.sorted()
        // 2. Tạo ID theo định dạng uid1_uid2
        val chatId = "${sortedIds[0]}_${sortedIds[1]}"

        // 3. Tạo đối tượng chat
        val chat = mapOf(
            "participants" to participants,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastMessage" to "",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // 4. Dùng ID đã tạo để tạo document mới một cách TƯỜNG MINH
        db.collection("chats").document(chatId).set(chat).await()
        // --- KẾT THÚC LOGIC TẠO CHAT ID ---
    }

    fun rejectRequest(request: Friendship, onResult: (Boolean) -> Unit) {
        db.collection("friendships")
            .document(request.id)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getIncomingRequests(currentUid: String, onResult: (List<Friendship>) -> Unit) {
        db.collection("friendships")
            .whereArrayContains("participants", currentUid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { doc ->
                    doc.toObject(Friendship::class.java).copy(id = doc.id)
                }.filter { it.requestBy != currentUid }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getFriends(currentUid: String, onResult: (List<Friendship>) -> Unit) {
        db.collection("friendships")
            .whereArrayContains("participants", currentUid)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { doc ->
                    doc.toObject(Friendship::class.java).copy(id = doc.id)
                }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getSentRequests(currentUid: String, onResult: (List<Friendship>) -> Unit) {
        db.collection("friendships")
            .whereArrayContains("participants", currentUid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { doc ->
                    doc.toObject(Friendship::class.java).copy(id = doc.id)
                }.filter { it.requestBy == currentUid }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}