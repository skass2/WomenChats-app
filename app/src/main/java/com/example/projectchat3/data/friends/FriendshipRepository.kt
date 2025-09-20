package com.example.projectchat3.data.friends

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class FriendshipRepository(private val db: FirebaseFirestore) {

    fun sendRequest(currentUid: String, friendUid: String, onResult: (Boolean) -> Unit) {
        val friendship = Friendship(
            participants = listOf(currentUid, friendUid),
            status = "pending",
            requestBy = currentUid,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        db.collection("friendships")
            .add(friendship)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun acceptRequest(request: Friendship, onResult: (Boolean) -> Unit) {
        db.collection("friendships")
            .document(request.id)
            .update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to Timestamp.now()
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
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
                }.filter { it.requestBy != currentUid } // chỉ lấy request người khác gửi tới
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
}
