package com.example.projectchat3.ui.chats

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
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
import com.example.projectchat3.R
import com.example.projectchat3.data.chats.Message
import com.example.projectchat3.data.chats.ChatRepository
import com.example.projectchat3.data.users.User
import com.example.projectchat3.ui.adapter.MessageAdapter
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

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(ChatRepository(FirebaseFirestore.getInstance()), application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = maxOf(imeInsets.bottom, navInsets.bottom)

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
            insets
        }
        currentUserId = FirebaseAuth.getInstance().uid!!
        chatUserId = intent.getStringExtra("uid")!!

        // Title hiển thị tên người đang chat
        tvTitle = findViewById(R.id.tvTitle)
        FirebaseFirestore.getInstance().collection("users").document(chatUserId).get()
            .addOnSuccessListener { doc ->
                val chatUser = doc.toObject(User::class.java)
                tvTitle.text = chatUser?.name ?: "User"
            }

        // Nút Back
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(
            mutableListOf(),
            currentUserId,
            onEdit = { message -> showEditDialog(message) },
            onDelete = { message -> showDeleteDialog(message) }
        )
        recyclerView.adapter = adapter

        // Gửi tin nhắn
        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val participants = listOf(currentUserId, chatUserId)

        // Hàm gửi chung
        fun sendMessage() {
            val text = edtMessage.text.toString().trim() // ✅ trim khoảng trắng
            if (text.isNotEmpty()) {
                val msg = Message(senderId = currentUserId, text = text)
                viewModel.sendMessage(msg, participants)
                edtMessage.text.clear()
            } else {
                edtMessage.error = "Nhập nội dung trước khi gửi nhé!"
            }
        }

        // Nhấn nút gửi
        btnSend.setOnClickListener { sendMessage() }

        // Nhấn Enter để gửi
        edtMessage.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // Quan sát messages realtime
        viewModel.getOrCreateChat(participants) { chatId ->
            if (chatId != null) {
                viewModel.loadMessages(chatId)
                viewModel.messages.observe(this) { list ->
                    adapter.updateMessages(list)
                    if (list.isNotEmpty()) {
                        recyclerView.scrollToPosition(list.size - 1)
                    }
                }
            }
        }
    }

    private fun showEditDialog(message: Message) {
        val input = EditText(this)
        input.setText(message.text)
        AlertDialog.Builder(this)
            .setTitle("Sửa tin nhắn")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val participants = listOf(currentUserId, chatUserId)
                    viewModel.getOrCreateChat(participants) { chatId ->
                        if (chatId != null) {
                            viewModel.updateMessage(chatId, message.id, newText)
                        }
                    }
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
                val participants = listOf(currentUserId, chatUserId)
                viewModel.getOrCreateChat(participants) { chatId ->
                    if (chatId != null) {
                        viewModel.deleteMessage(chatId, message.id)
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

}
