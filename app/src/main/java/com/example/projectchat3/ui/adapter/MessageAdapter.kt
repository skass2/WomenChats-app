package com.example.projectchat3.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.projectchat3.R
import com.example.projectchat3.data.chats.Message

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
        val imgMessage: ImageView = view.findViewById(R.id.imgMessage)
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
        val bubble: FrameLayout = view.findViewById(R.id.bubble)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isMe = message.senderId == currentUserId

        val density = holder.itemView.context.resources.displayMetrics.density
        val padText = (8 * density).toInt()
        val padImage = (2 * density).toInt()

        when {
            message.deleted -> {
                // --- Tin nhắn đã bị thu hồi ---
                holder.imgMessage.visibility = View.GONE
                holder.txtMessage.visibility = View.VISIBLE
                holder.txtMessage.text = "Tin nhắn này đã bị thu hồi"
                holder.txtMessage.setTypeface(null, Typeface.ITALIC)
                holder.txtMessage.setTextColor(Color.GRAY)
                holder.bubble.setPadding(padText, padText, padText, padText)
            }

            !message.imageUrl.isNullOrEmpty() -> {
                // --- Tin nhắn là ảnh ---
                holder.txtMessage.visibility = View.GONE
                holder.imgMessage.visibility = View.VISIBLE

                Glide.with(holder.itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imgMessage)

                holder.bubble.setPadding(padImage, padImage, padImage, padImage)
            }

            else -> {
                // --- Tin nhắn là text ---
                holder.imgMessage.visibility = View.GONE
                holder.txtMessage.visibility = View.VISIBLE
                holder.txtMessage.text = message.text
                holder.txtMessage.setTypeface(null, Typeface.NORMAL)
                holder.bubble.setPadding(padText, padText, padText, padText)
            }
        }

        // --- Style bong bóng (phải / trái) ---
        if (isMe) {
            holder.bubble.setBackgroundResource(R.drawable.bg_message_right)
            holder.txtMessage.setTextColor(Color.WHITE)
            holder.container.gravity = Gravity.END
        } else {
            holder.bubble.setBackgroundResource(R.drawable.bg_message_left)
            holder.txtMessage.setTextColor(Color.BLACK)
            holder.container.gravity = Gravity.START
        }

        // --- Menu sửa / xóa ---
        holder.itemView.setOnLongClickListener {
            if (isMe && !message.deleted) {
                val popup = PopupMenu(holder.txtMessage.context, holder.txtMessage)
                popup.menuInflater.inflate(R.menu.message_menu, popup.menu)
                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_edit -> {
                            onEditMessage(message)
                            true
                        }

                        R.id.action_delete -> {
                            onDeleteMessage(message)
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

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
