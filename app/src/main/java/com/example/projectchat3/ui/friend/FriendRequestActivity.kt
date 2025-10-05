package com.example.projectchat3.ui.friend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User
import com.example.projectchat3.ui.friends.FriendRequestAdapter
import com.example.projectchat3.ui.friends.RequestType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestActivity : AppCompatActivity() {

    private lateinit var adapter: FriendRequestAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_request)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerFriendRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FriendRequestAdapter(
            type = RequestType.RECEIVED,
            requests = emptyList(),
            onAccept = { user -> acceptRequest(user) },
            onReject = { user -> declineRequest(user) }
        )
        recyclerView.adapter = adapter

        loadFriendRequests()
    }

    private fun loadFriendRequests() {
        db.collection("friend_requests")
            .whereEqualTo("to", currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || snapshot.isEmpty) {
                    adapter.updateList(emptyList())
                    return@addSnapshotListener
                }

                val users = mutableListOf<User>()
                snapshot.documents.forEach { doc ->
                    val uid = doc.getString("from")
                    if (uid != null) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { userDoc ->
                                val user = userDoc.toObject(User::class.java)
                                if (user != null) {
                                    users.add(user)
                                    adapter.updateList(users)
                                }
                            }
                    }
                }
            }
    }

    private fun acceptRequest(user: User) {
        val batch = db.batch()

        val currentUserRef = db.collection("users").document(currentUid)
            .collection("friends").document(user.uid)
        val friendUserRef = db.collection("users").document(user.uid)
            .collection("friends").document(currentUid)

        batch.set(currentUserRef, user)
        batch.set(friendUserRef, mapOf("uid" to currentUid))

        val query = db.collection("friend_requests")
            .whereEqualTo("from", user.uid)
            .whereEqualTo("to", currentUid)

        query.get().addOnSuccessListener { result ->
            for (doc in result) {
                batch.delete(doc.reference)
            }
            batch.commit()
        }
    }

    private fun declineRequest(user: User) {
        val query = db.collection("friend_requests")
            .whereEqualTo("from", user.uid)
            .whereEqualTo("to", currentUid)

        query.get().addOnSuccessListener { result ->
            for (doc in result) {
                doc.reference.delete()
            }
        }
    }
}
