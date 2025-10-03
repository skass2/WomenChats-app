package com.example.projectchat3.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User

class FriendListAdapter(
    private var friends: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<FriendListAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = friends[position]
        holder.tvName.text = user.name

        Glide.with(holder.itemView.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = friends.size

    fun updateList(newFriends: List<User>) {
        friends = newFriends
        notifyDataSetChanged()
    }
}
