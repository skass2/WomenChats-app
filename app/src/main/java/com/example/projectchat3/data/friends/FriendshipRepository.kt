package com.example.projectchat3.data.friends

import android.util.Log
import com.example.projectchat3.data.chats.ChatUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FriendshipRepository(private val db: FirebaseFirestore) {

    fun sendRequest(currentUid: String, friendUid: String, onResult: (Boolean) -> Unit) {
        val friendshipId = "${currentUid}_$friendUid"

        val friendship = Friendship(
            id = friendshipId,
            participants = listOf(currentUid, friendUid),
            status = "pending",
            requestBy = currentUid,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        db.collection("friendships")
            .document(friendship.id)
            .set(friendship)
            .addOnSuccessListener {
                Log.d("FriendRepo", "✅ sendRequest success: $friendshipId")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ sendRequest failed: ${e.message}")
                onResult(false)
            }
    }
    private val firestore = FirebaseFirestore.getInstance()
    suspend fun acceptRequest(fid: String, currentUid: String, otherUid: String) {

        val friendshipRef = firestore.collection("friendships").document(fid)
        // 1. Update friendship trước
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(friendshipRef)
            if (snapshot.getString("status") == "pending") {
                transaction.update(friendshipRef, "status", "accepted")
            } else {
                throw Exception("Friendship not pending")
            }
        }.await()

        // 2. Sau khi update thành công → tạo chat
        val participants = listOf(currentUid, otherUid)
        val chatId = firestore.collection("chats").document().id
        val chat = mapOf(
            "participants" to participants,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("chats").document(chatId).set(chat).await()
    }



    fun rejectRequest(request: Friendship, onResult: (Boolean) -> Unit) {
        db.collection("friendships")
            .document(request.id)
            .delete()
            .addOnSuccessListener {
                Log.d("FriendRepo", "✅ rejectRequest success: ${request.id}")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ rejectRequest failed: ${e.message}")
                onResult(false)
            }
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

                Log.d("FriendRepo", "📥 Incoming requests for $currentUid = ${list.size}")
                list.forEach { Log.d("FriendRepo", "↪️ $it") }

                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ getIncomingRequests failed: ${e.message}")
                onResult(emptyList())
            }
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

                Log.d("FriendRepo", "👫 Friends of $currentUid = ${list.size}")
                list.forEach { Log.d("FriendRepo", "↪️ $it") }

                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ getFriends failed: ${e.message}")
                onResult(emptyList())
            }
    }

    fun getSentRequests(currentUid: String, onResult: (List<Friendship>) -> Unit) {
        db.collection("friendships")
            .whereArrayContains("participants", currentUid) // 🔑 đổi chỗ này
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { doc ->
                    doc.toObject(Friendship::class.java).copy(id = doc.id)
                }.filter { it.requestBy == currentUid } // 🔑 lọc ở client

                Log.d("FriendRepo", "📤 Sent requests by $currentUid = ${list.size}")
                list.forEach { Log.d("FriendRepo", "↪️ $it") }

                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ getSentRequests failed: ${e.message}")
                onResult(emptyList())
            }
    }
}

