package com.example.projectchat3.ui.friends

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

class FriendRequestAdapter(
    private var requests: List<User>,
    private val onAccept: (User) -> Unit,
    private val onDecline: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnDecline: Button = itemView.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = requests[position]
        holder.tvName.text = user.name

        Glide.with(holder.itemView.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.btnAccept.setOnClickListener { onAccept(user) }
        holder.btnDecline.setOnClickListener { onDecline(user) }
    }

    override fun getItemCount() = requests.size

    fun updateList(newRequests: List<User>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
