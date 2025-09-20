package com.example.projectchat3.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.ui.adapter.UserAdapter
import com.example.projectchat3.ui.chats.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListFragment : Fragment() {

    private lateinit var adapter: UserAdapter
    private lateinit var currentUid: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)

        currentUid = FirebaseAuth.getInstance().uid ?: ""

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerChats)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = UserAdapter(mutableListOf()) { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("uid", user.uid)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Load danh sách chat gần đây
        db.collection("chats")
            .whereArrayContains("participants", currentUid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val chatUsers = mutableListOf<com.example.projectchat3.data.users.User>()
                snapshot?.documents?.forEach { doc ->
                    val participants = doc.get("participants") as? List<String> ?: listOf()
                    val friendUid = participants.firstOrNull { it != currentUid }
                    if (friendUid != null) {
                        db.collection("users").document(friendUid).get()
                            .addOnSuccessListener { userDoc ->
                                val user = userDoc.toObject(com.example.projectchat3.data.users.User::class.java)
                                if (user != null) {
                                    chatUsers.add(user)
                                    adapter.updateUsers(chatUsers)
                                }
                            }
                    }
                }
            }

        return view
    }
}
