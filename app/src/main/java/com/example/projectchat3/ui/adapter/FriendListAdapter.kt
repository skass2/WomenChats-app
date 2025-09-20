// FriendListAdapter.kt
package com.example.projectchat3.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.friends.Friendship
import com.example.projectchat3.data.users.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendListAdapter(
    private var friends: MutableList<Friendship>,
    private val db: FirebaseFirestore,
    private val onClick: (Friendship, User) -> Unit
) : RecyclerView.Adapter<FriendListAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friendship = friends[position]
        val myUid = FirebaseAuth.getInstance().uid
        val friendUid = friendship.participants.firstOrNull { it != myUid } ?: return

        // ✅ Lấy thông tin bạn bè từ Firestore
        db.collection("users").document(friendUid).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    holder.tvName.text = user.name
                    holder.itemView.setOnClickListener {
                        onClick(friendship, user)
                    }
                } else {
                    holder.tvName.text = "Unknown"
                }
            }
    }

    override fun getItemCount() = friends.size

    fun updateFriends(newList: List<Friendship>) {
        friends.clear()
        friends.addAll(newList)
        notifyDataSetChanged()
    }
}
