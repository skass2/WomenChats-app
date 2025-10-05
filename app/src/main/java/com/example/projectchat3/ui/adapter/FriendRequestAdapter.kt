package com.example.projectchat3.ui.friends

import android.util.Log
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

enum class RequestType { RECEIVED, SENT }

class FriendRequestAdapter(
    private val type: RequestType,
    requests: List<User>,
    private val onAccept: (User) -> Unit,
    private val onReject: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    private val requestList = requests.toMutableList()

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = requestList[position]
        Log.d("FriendRequestAdapter", "🧾 Bind ${user.name}, type=$type")

        holder.tvName.text = user.name

        Glide.with(holder.itemView.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(holder.imgAvatar)

        if (type == RequestType.RECEIVED) {
            holder.btnAccept.visibility = View.VISIBLE
            holder.btnReject.text = "TỪ CHỐI"
        } else {
            holder.btnAccept.visibility = View.GONE
            holder.btnReject.text = "THU HỒI"
        }

        holder.btnAccept.setOnClickListener {
            Log.d("FriendRequestAdapter", "✅ Click ĐỒNG Ý: ${user.uid}")
            onAccept(user)
            disableButtons(holder, "Đã chấp nhận")
        }

        holder.btnReject.setOnClickListener {
            Log.d("FriendRequestAdapter", "❌ Click ${holder.btnReject.text}: ${user.uid}")
            onReject(user)
            disableButtons(holder, "Đang xử lý...")
        }
    }

    private fun disableButtons(holder: RequestViewHolder, message: String) {
        holder.btnAccept.isEnabled = false
        holder.btnReject.isEnabled = false
        holder.btnReject.text = message
    }

    override fun getItemCount() = requestList.size

    fun updateList(newRequests: List<User>) {
        Log.d("FriendRequestAdapter", "🔄 Update list (${type.name}): ${newRequests.size} users")
        requestList.clear()
        requestList.addAll(newRequests)
        notifyDataSetChanged()
    }

    fun removeRequest(user: User) {
        val index = requestList.indexOfFirst { it.uid == user.uid }
        if (index != -1) {
            Log.d("FriendRequestAdapter", "🗑 Remove user=${user.name}, type=$type")
            requestList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
