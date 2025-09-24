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

    private lateinit var receivedAdapter: FriendRequestAdapter
    private lateinit var sentAdapter: FriendRequestAdapter

    private val viewModel: FriendshipViewModel by viewModels {
        FriendshipViewModelFactory(
            FriendshipRepository(FirebaseFirestore.getInstance())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friend_request, container, false)

        // RecyclerView cho lời mời đã nhận
        val recyclerReceived = view.findViewById<RecyclerView>(R.id.recyclerFriendRequestsReceived)
        recyclerReceived.layoutManager = LinearLayoutManager(requireContext())
        receivedAdapter = FriendRequestAdapter(
            requests = mutableListOf(),
            db = FirebaseFirestore.getInstance(),
            type = FriendRequestAdapter.RequestType.RECEIVED,
            onAccept = { request: Friendship -> viewModel.acceptRequest(request) },
            onReject = { request: Friendship -> viewModel.rejectRequest(request) }
        )
        recyclerReceived.adapter = receivedAdapter

        // RecyclerView cho lời mời đã gửi
        val recyclerSent = view.findViewById<RecyclerView>(R.id.recyclerFriendRequestsSent)
        recyclerSent.layoutManager = LinearLayoutManager(requireContext())
        sentAdapter = FriendRequestAdapter(
            requests = mutableListOf(),
            db = FirebaseFirestore.getInstance(),
            type = FriendRequestAdapter.RequestType.SENT,
            onCancel = { request: Friendship -> viewModel.cancelRequest(request) } // dùng cancelRequest
        )
        recyclerSent.adapter = sentAdapter

        // Lắng nghe LiveData
        viewModel.incomingRequests.observe(viewLifecycleOwner) { list ->
            receivedAdapter.updateRequests(list)
        }
        viewModel.sentRequests.observe(viewLifecycleOwner) { list ->
            sentAdapter.updateRequests(list)
        }

        // Load dữ liệu
        FirebaseAuth.getInstance().uid?.let { uid ->
            viewModel.loadIncomingRequests(uid)
            viewModel.loadSentRequests(uid)
        }
        return view
    }
}
