package com.example.projectchat3.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User

class FriendListAdapter(
    private var friends: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<FriendListAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatarFriend)
        val tvName: TextView = itemView.findViewById(R.id.tvUserNameFriend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = friends[position]
        holder.tvName.text = user.name.ifBlank { "Người dùng ẩn danh" }

        Glide.with(holder.itemView.context)
            .load(
                user.avatarUrl.takeIf { !it.isNullOrEmpty() }
                    ?: R.drawable.ic_person // fallback ảnh mặc định
            )
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount(): Int = friends.size

    fun updateList(newFriends: List<User>) {
        val diffCallback = FriendDiffCallback(friends, newFriends)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        friends = newFriends
        diffResult.dispatchUpdatesTo(this)
    }

    private class FriendDiffCallback(
        private val oldList: List<User>,
        private val newList: List<User>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].uid == newList[newItemPosition].uid
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
