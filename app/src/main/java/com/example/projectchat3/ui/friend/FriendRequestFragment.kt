package com.example.projectchat3.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestFragment : Fragment() {

    private lateinit var adapter: FriendRequestAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_request, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFriendRequestsReceived)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendRequestAdapter(
            requests = emptyList(),
            onAccept = { user -> acceptRequest(user) },
            onDecline = { user -> declineRequest(user) }
        )
        recyclerView.adapter = adapter

        loadFriendRequests()

        return view
    }

    private fun loadFriendRequests() {
        db.collection("friend_requests")
            .whereEqualTo("to", currentUid)
            .addSnapshotListener { snapshot, _ ->
                val users = mutableListOf<User>()
                snapshot?.documents?.forEach { doc ->
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

        // xóa request
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
