package com.example.projectchat3.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.chats.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isMe = message.senderId == currentUserId

        if (message.deleted) {
            holder.txtMessage.text = "Tin nhắn này đã bị xóa!"
            holder.txtMessage.setTypeface(null, Typeface.ITALIC)
            holder.txtMessage.setTextColor(Color.WHITE)
            holder.txtMessage.setBackgroundResource(R.drawable.bg_message_deleted)
            holder.container.gravity = if (isMe) Gravity.END else Gravity.START
            holder.itemView.setOnLongClickListener { true } // khóa menu
        } else {
            holder.txtMessage.text = message.text
            holder.txtMessage.setTypeface(null, Typeface.NORMAL)

            if (isMe) {
                holder.txtMessage.setBackgroundResource(R.drawable.bg_message_right)
                holder.txtMessage.setTextColor(Color.WHITE)
                holder.container.gravity = Gravity.END
            } else {
                holder.txtMessage.setBackgroundResource(R.drawable.bg_message_left)
                holder.txtMessage.setTextColor(Color.BLACK)
                holder.container.gravity = Gravity.START
            }

            holder.itemView.setOnLongClickListener {
                if (isMe && !message.deleted) { // Chỉ cho phép với tin nhắn của mình và chưa bị xóa
                    val popup = PopupMenu(holder.txtMessage.context, holder.txtMessage)
                    popup.menuInflater.inflate(R.menu.message_menu, popup.menu)
                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.action_edit -> {
                                onEditMessage(message) // <-- GỌI HÀM LAMBDA
                                true
                            }
                            R.id.action_delete -> {
                                onDeleteMessage(message) // <-- GỌI HÀM LAMBDA
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
                true
            }
        }
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
