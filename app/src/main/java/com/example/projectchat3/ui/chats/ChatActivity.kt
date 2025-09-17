package com.example.projectchat3.ui.chats

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.data.chats.Message
import com.example.projectchat3.R
import com.example.projectchat3.data.chats.ChatRepository
import com.example.projectchat3.data.chats.ChatUtils
import com.example.projectchat3.ui.adapter.MessageAdapter
import com.example.projectchat3.data.users.User
import com.example.projectchat3.ui.chat.ChatViewModel
import com.example.projectchat3.ui.chat.ChatViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {
    private lateinit var adapter: MessageAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var currentUserId: String
    private lateinit var chatUserId: String
    private lateinit var roomId: String

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(ChatRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentUserId = FirebaseAuth.getInstance().uid!!
        chatUserId = intent.getStringExtra("uid")!!

        // Title
        tvTitle = findViewById(R.id.tvTitle)
        FirebaseFirestore.getInstance().collection("users").document(chatUserId).get()
            .addOnSuccessListener { doc ->
                val chatUser = doc.toObject(User::class.java)
                tvTitle.text = chatUser?.name ?: "User"
            }

        // Back button
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Room id (thống nhất với ChatUtils)
        roomId = ChatUtils.generateRoomId(currentUserId, chatUserId)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(
            mutableListOf(),
            currentUserId,
            onEdit = { message -> showEditDialog(message) },
            onDelete = { message -> showDeleteDialog(message) }
        )
        recyclerView.adapter = adapter

        // Send message
        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)
        btnSend.setOnClickListener {
            val msg = Message(
                senderId = currentUserId,
                roomId = roomId,                // ✅ gán roomId vào message luôn
                text = edtMessage.text.toString()
            )
            if (msg.text.isNotEmpty()) {
                viewModel.sendMessage(roomId, msg)
                edtMessage.text.clear()
            } else {
                edtMessage.error = "Nhập nội dung trước khi gửi nhé!"
            }
        }

        viewModel.messages.observe(this) { list ->
            adapter.updateMessages(list)
            recyclerView.scrollToPosition(list.size - 1)
        }

        viewModel.loadMessages(roomId)
    }

    private fun showEditDialog(message: Message) {
        val input = EditText(this)
        input.setText(message.text)
        AlertDialog.Builder(this)
            .setTitle("Sửa tin nhắn")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newText = input.text.toString()
                if (newText.isNotEmpty()) {
                    viewModel.updateMessage(roomId, message.id, newText)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteDialog(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Xóa tin nhắn")
            .setMessage("Bạn có chắc muốn xóa tin nhắn này?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteMessage(roomId, message.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
