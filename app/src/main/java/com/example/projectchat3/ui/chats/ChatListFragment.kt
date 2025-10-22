package com.example.projectchat3.ui.chats

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

        val recyclerView = view.findViewById<RecyclerView>(R.id.chatListRecyclerView)
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
                if (snapshot == null || snapshot.isEmpty) {
                    adapter.updateUsers(emptyList())
                    return@addSnapshotListener
                }
                // Bước 1: Lấy hết ID của bạn bè trong các cuộc trò chuyện
                val friendUids = snapshot.documents.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<String>
                    participants?.firstOrNull { it != currentUid }
                }.filter { it.isNotEmpty() }
                if (friendUids.isEmpty()) {
                    adapter.updateUsers(emptyList())
                    return@addSnapshotListener
                }
                // Bước 2: Chỉ thực hiện 1 truy vấn duy nhất để lấy tất cả user
                db.collection("users").whereIn("uid", friendUids)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        val users = userSnapshot.toObjects(User::class.java)
                        adapter.updateUsers(users) // Cập nhật UI chỉ 1 lần
                    }
            }
        return view
    }
}