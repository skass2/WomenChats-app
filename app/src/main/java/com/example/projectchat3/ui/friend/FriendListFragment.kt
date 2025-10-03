package com.example.projectchat3.ui.friends

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User
import com.example.projectchat3.ui.adapter.UserAdapter
import com.example.projectchat3.ui.chats.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendListFragment : Fragment() {

    private lateinit var adapter: FriendListAdapter
    private lateinit var currentUid: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_list, container, false)

        currentUid = FirebaseAuth.getInstance().uid ?: ""

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFriends)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendListAdapter(emptyList()) { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("uid", user.uid)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Load danh sách bạn bè
        db.collection("users")
            .document(currentUid)
            .collection("friends")
            .addSnapshotListener { snapshot, _ ->
                val friendList = mutableListOf<User>()
                snapshot?.documents?.forEach { doc ->
                    val friend = doc.toObject(User::class.java)
                    if (friend != null) friendList.add(friend)
                }
                adapter.updateList(friendList)
            }

        return view
    }
}
