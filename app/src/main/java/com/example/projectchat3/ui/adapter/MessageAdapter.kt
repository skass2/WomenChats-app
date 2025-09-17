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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        if (message.deleted) {
            holder.txtMessage.text = "Tin nhắn này đã bị xóa!"
            holder.txtMessage.setTypeface(null, Typeface.ITALIC)
            holder.txtMessage.setTextColor(Color.GRAY)
            holder.itemView.setOnLongClickListener { true }
        } else {
            holder.txtMessage.text = message.text
            holder.txtMessage.setTypeface(null, Typeface.NORMAL)
            holder.txtMessage.setTextColor(Color.BLACK)

            holder.itemView.setOnLongClickListener {
                if (message.senderId == currentUserId) {
                    val gravity = if (message.senderId == currentUserId) Gravity.END else Gravity.START

                    val popup = PopupMenu(holder.itemView.context, holder.itemView, gravity)
                    popup.menuInflater.inflate(R.menu.message_menu, popup.menu)

                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.action_edit -> {
                                onEdit(message)
                                true
                            }
                            R.id.action_delete -> {
                                onDelete(message)
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
        val params = holder.txtMessage.layoutParams as LinearLayout.LayoutParams
        params.gravity = if (message.senderId == currentUserId) Gravity.END else Gravity.START
        holder.txtMessage.layoutParams = params
    }


    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}