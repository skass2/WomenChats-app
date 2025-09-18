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
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.chats.Message

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onEdit: (Message) -> Unit,
    private val onDelete: (Message) -> Unit
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
            // Tin nhắn đã xóa
            holder.txtMessage.text = "Tin nhắn này đã bị xóa!"
            holder.txtMessage.setTypeface(null, Typeface.ITALIC)
            holder.txtMessage.setTextColor(Color.WHITE)
            holder.txtMessage.setBackgroundResource(R.drawable.bg_message_deleted)
            holder.container.gravity = if (isMe) Gravity.END else Gravity.START

            // Không bật menu
            holder.itemView.setOnLongClickListener { true }
        } else {
            // Tin nhắn bình thường
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

            // Menu sửa / xóa
            holder.itemView.setOnLongClickListener {
                if (isMe) {
                    val popup = PopupMenu(holder.itemView.context, holder.txtMessage)
                    popup.menuInflater.inflate(R.menu.message_menu, popup.menu)

                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.action_edit -> {
                                onEdit(message)
                                true
                            }
                            R.id.action_delete -> {
                                onDelete(message) //gọi callback
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
