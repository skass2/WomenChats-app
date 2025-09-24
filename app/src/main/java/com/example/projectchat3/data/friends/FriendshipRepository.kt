package com.example.projectchat3.data.friends

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

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

    fun acceptRequest(request: Friendship, onResult: (Boolean) -> Unit) {
        val batch = db.batch()
        val friendshipRef = db.collection("friendships").document(request.id)
        val chatRef = db.collection("chats").document(request.id)

        batch.update(
            friendshipRef, mapOf(
                "status" to "accepted",
                "updatedAt" to Timestamp.now()
            )
        )

        batch.set(
            chatRef, mapOf(
                "participants" to request.participants,
                "lastMessage" to "",
                "updatedAt" to Timestamp.now()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                Log.d("FriendRepo", "✅ acceptRequest success: ${request.id}")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("FriendRepo", "❌ acceptRequest failed: ${e.message}")
                onResult(false)
            }
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

