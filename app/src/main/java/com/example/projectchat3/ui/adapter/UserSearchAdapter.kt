package com.example.projectchat3.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User

class UserSearchAdapter(
    private val users: MutableList<User>,
    private val onAddFriend: (User, (Boolean) -> Unit) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    private val sentRequests = mutableSetOf<String>()
    private val friends = mutableSetOf<String>()
    private val sendingRequests = mutableSetOf<String>()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val btnAdd: Button = itemView.findViewById(R.id.btnFriend)
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_add, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.name

        Glide.with(holder.itemView.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(holder.imgAvatar)

        val uid = user.uid
        val isFriend = friends.contains(uid)
        val isSent = sentRequests.contains(uid)
        val isSending = sendingRequests.contains(uid)

        holder.btnAdd.apply {
            when {
                isFriend -> {
                    text = "Bạn bè"
                    isEnabled = false
                    alpha = 0.6f
                }
                isSent -> {
                    text = "Đã gửi lời mời"
                    isEnabled = false
                    alpha = 0.6f
                }
                isSending -> {
                    text = "Đang gửi..."
                    isEnabled = false
                    alpha = 0.7f
                }
                else -> {
                    text = "Thêm bạn"
                    isEnabled = true
                    alpha = 1f
                }
            }
        }

        holder.btnAdd.setOnClickListener {
            if (!isFriend && !isSent && !isSending) {
                sendingRequests.add(uid)
                notifyItemChanged(position)

                onAddFriend(user) { success ->
                    sendingRequests.remove(uid)
                    if (success) {
                        sentRequests.add(uid)
                    }
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun markSentRequests(uids: List<String>) {
        sentRequests.clear()
        sentRequests.addAll(uids)
        notifyDataSetChanged()
    }

    fun markFriends(uids: List<String>) {
        friends.clear()
        friends.addAll(uids)
        notifyDataSetChanged()
    }
}
