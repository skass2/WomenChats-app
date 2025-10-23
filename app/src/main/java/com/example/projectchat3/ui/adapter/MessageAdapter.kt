package com.example.projectchat3.ui.adapter

import android.content.Intent
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
import com.example.projectchat3.ui.chats.FullImageActivity
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
        val imgMessage: ImageView = view.findViewById(R.id.imgMessage)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
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
        val pad = (8 * density).toInt()

        if (message.deleted) {
            // --- Tin nhắn đã bị xóa ---
            holder.imgMessage.visibility = View.GONE
            holder.txtMessage.visibility = View.VISIBLE
            holder.txtMessage.text = "Tin nhắn đã bị xóa"
            holder.txtMessage.setTypeface(null, Typeface.ITALIC)
            holder.txtMessage.setTextColor(Color.GRAY)
            holder.bubble.setBackgroundColor(Color.parseColor("#E0E0E0")) // nền xám dịu
            holder.bubble.setPadding(pad, pad, pad, pad)
        } else if (!message.imageUrl.isNullOrEmpty()) {
            // --- Tin nhắn ảnh ---
            holder.txtMessage.visibility = View.GONE
            holder.imgMessage.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(message.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imgMessage)

            holder.imgMessage.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, FullImageActivity::class.java)
                intent.putExtra("imageUrl", message.imageUrl)
                context.startActivity(intent)
            }
        } else {
            // --- Tin nhắn text bình thường ---
            holder.imgMessage.visibility = View.GONE
            holder.txtMessage.visibility = View.VISIBLE
            holder.txtMessage.text = message.text
            holder.txtMessage.setTextColor(Color.BLACK)
        }

        // --- Căn hướng bong bóng ---
        if (isMe) {
            holder.bubble.setBackgroundResource(R.drawable.bg_message_right)
            holder.txtMessage.setTextColor(Color.BLACK)
            holder.container.gravity = Gravity.END
            holder.txtTime.gravity = Gravity.END
        } else {
            holder.bubble.setBackgroundResource(R.drawable.bg_message_left)
            holder.txtMessage.setTextColor(Color.BLACK)
            holder.container.gravity = Gravity.START
            holder.txtTime.gravity = Gravity.START
        }

        // --- Hiển thị thời gian gửi (HH:mm) ---
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = message.timestamp?.let {
            // nếu timestamp là Long thì xài luôn; nếu là Firestore Timestamp thì dùng seconds * 1000
            try {
                when (it) {
                    is com.google.firebase.Timestamp -> dateFormat.format(Date(it.seconds * 1000))
                    is Long -> dateFormat.format(Date(it))
                    else -> ""
                }
            } catch (e: Exception) {
                ""
            }
        } ?: ""
        holder.txtTime.text = timeText

        // --- Menu sửa / xóa ---
        holder.itemView.setOnLongClickListener {
            if (isMe && !message.deleted) {
                val popup = PopupMenu(holder.txtMessage.context, holder.txtMessage)
                popup.menuInflater.inflate(R.menu.message_menu, popup.menu)

                // Ảnh thì không cho sửa
                if (!message.imageUrl.isNullOrEmpty()) {
                    popup.menu.removeItem(R.id.action_edit)
                }

                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_edit -> { onEditMessage(message); true }
                        R.id.action_delete -> { onDeleteMessage(message); true }
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
