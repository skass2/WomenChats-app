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
import com.example.projectchat3.ui.chats.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FriendListFragment : Fragment() {

    private lateinit var adapter: FriendListAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFriends)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FriendListAdapter(emptyList()) { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("uid", user.uid)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadFriends()

        return view
    }

    private fun loadFriends() {
        if (currentUid.isEmpty()) return

        // 1. Lấy các friendship mà user tham gia và đã accepted
        db.collection("friendships")
            .whereArrayContains("participants", currentUid)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, _ ->
                val friendUids = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val participants = doc.get("participants") as? List<*>
                        participants?.firstOrNull { it != currentUid } as? String
                    } ?: emptyList()

                if (friendUids.isEmpty()) {
                    adapter.updateList(emptyList())
                    return@addSnapshotListener
                }

                // 2. Lấy thông tin user từ collection users
                db.collection("users")
                    .whereIn("uid", friendUids)
                    .get()
                    .addOnSuccessListener { result ->
                        val friends = result.mapNotNull { doc ->
                            val uid = doc.getString("uid")
                            val name = doc.getString("name")
                            val avatar = doc.getString("avatarUrl")
                            if (uid != null && name != null)
                                User(uid = uid, name = name, avatarUrl = avatar ?: "")
                            else null
                        }
                        adapter.updateList(friends)
                    }
            }
    }
}
