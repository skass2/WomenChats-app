package com.example.projectchat3.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.friends.Friendship
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestAdapter(
    private var requests: MutableList<Friendship>,
    private val db: FirebaseFirestore,
    private val onAccept: (Friendship) -> Unit,
    private val onReject: (Friendship) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        val otherUid = request.participants.firstOrNull {
            it != FirebaseAuth.getInstance().uid
        } ?: ""

        // Lấy tên user từ Firestore
        db.collection("users").document(otherUid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown"
                holder.tvName.text = name
            }

        holder.btnAccept.setOnClickListener { onAccept(request) }
        holder.btnReject.setOnClickListener { onReject(request) }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newList: List<Friendship>) {
        requests.clear()
        requests.addAll(newList)
        notifyDataSetChanged()
    }
}
