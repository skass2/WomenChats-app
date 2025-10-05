package com.example.projectchat3.data.friends

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FriendshipRepository(private val db: FirebaseFirestore) {

    fun sendRequest(currentUid: String, friendUid: String, onComplete: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onComplete(false)
            return
        }

        var called = false
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            if (!called) {
                called = true
                proceedWithToken(user, currentUid, friendUid, onComplete)
            }
        }, 5000)

        user.reload()
            .addOnCompleteListener { reloadTask ->
                if (called) return@addOnCompleteListener
                called = true
                if (!reloadTask.isSuccessful) {
                    onComplete(false)
                    return@addOnCompleteListener
                }
                proceedWithToken(user, currentUid, friendUid, onComplete)
            }
            .addOnFailureListener {
                if (!called) {
                    called = true
                    onComplete(false)
                }
            }
    }

    private fun proceedWithToken(
        user: FirebaseUser,
        currentUid: String,
        friendUid: String,
        onComplete: (Boolean) -> Unit
    ) {
        user.getIdToken(true)
            .addOnSuccessListener { result ->
                val emailVerified = result.claims["email_verified"] as? Boolean ?: false
                if (!emailVerified) {
                    onComplete(false)
                    return@addOnSuccessListener
                }

                val ids = listOf(currentUid, friendUid).sorted()
                val docId = "${ids[0]}_${ids[1]}"

                val friendship = hashMapOf(
                    "participants" to listOf(currentUid, friendUid),
                    "requestBy" to currentUid,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                FirebaseFirestore.getInstance()
                    .collection("friendships")
                    .document(docId)
                    .set(friendship)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun acceptRequest(fid: String, currentUid: String, otherUid: String) {
        val friendshipRef = firestore.collection("friendships").document(fid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(friendshipRef)
            if (snapshot.getString("status") == "pending") {
                transaction.update(friendshipRef, "status", "accepted")
            } else {
                throw Exception("Friendship not pending")
            }
        }.await()

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
