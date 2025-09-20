package com.example.projectchat3.ui.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.friends.Friendship
import com.example.projectchat3.data.friends.FriendshipRepository
import com.example.projectchat3.ui.adapter.FriendRequestAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestFragment : Fragment() {

    private lateinit var adapter: FriendRequestAdapter

    private val viewModel: FriendshipViewModel by viewModels {
        FriendshipViewModelFactory(FriendshipRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friend_request, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFriendRequests)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendRequestAdapter(
            requests = mutableListOf(),
            db = FirebaseFirestore.getInstance(),
            onAccept = { request: Friendship -> viewModel.acceptRequest(request) },
            onReject = { request: Friendship -> viewModel.rejectRequest(request) }
        )
        recyclerView.adapter = adapter

        viewModel.incomingRequests.observe(viewLifecycleOwner) { list ->
            adapter.updateRequests(list)
        }

        val currentUid = FirebaseAuth.getInstance().uid
        if (currentUid != null) {
            viewModel.loadIncomingRequests(currentUid)
        }

        return view
    }
}
