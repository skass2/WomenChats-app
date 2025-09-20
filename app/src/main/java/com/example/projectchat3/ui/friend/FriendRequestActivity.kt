package com.example.projectchat3.ui.friend

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.friends.Friendship
import com.example.projectchat3.data.friends.FriendshipRepository
import com.example.projectchat3.ui.adapter.FriendRequestAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestActivity : AppCompatActivity() {

    private lateinit var adapter: FriendRequestAdapter

    private val viewModel: FriendshipViewModel by viewModels {
        FriendshipViewModelFactory(FriendshipRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friend_request)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerFriendRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FriendRequestAdapter(
            requests = mutableListOf(),
            db = FirebaseFirestore.getInstance(),
            onAccept = { request: Friendship -> viewModel.acceptRequest(request) },
            onReject = { request: Friendship -> viewModel.rejectRequest(request) }
        )
        recyclerView.adapter = adapter

        viewModel.incomingRequests.observe(this) { list ->
            adapter.updateRequests(list)
        }

        val currentUid = FirebaseAuth.getInstance().uid
        if (currentUid != null) {
            viewModel.loadIncomingRequests(currentUid)
        }
    }
}
