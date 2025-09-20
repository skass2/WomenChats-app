package com.example.projectchat3.ui.friend

import android.content.Intent
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
import com.example.projectchat3.ui.adapter.FriendListAdapter
import com.example.projectchat3.ui.chats.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendListFragment : Fragment() {

    private lateinit var adapter: FriendListAdapter

    private val viewModel: FriendshipViewModel by viewModels {
        FriendshipViewModelFactory(FriendshipRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friend_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFriends)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendListAdapter(
            friends = mutableListOf(),
            db = FirebaseFirestore.getInstance(),
            onClick = { friendship, user ->
                // ví dụ: mở ChatActivity
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("uid", user.uid)
                startActivity(intent)
            }
        )

        recyclerView.adapter = adapter

        viewModel.friends.observe(viewLifecycleOwner) { list ->
            adapter.updateFriends(list)
        }

        val currentUid = FirebaseAuth.getInstance().uid
        if (currentUid != null) {
            viewModel.loadFriends(currentUid)
        }

        return view
    }
}
