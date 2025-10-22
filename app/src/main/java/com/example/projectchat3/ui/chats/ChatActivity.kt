package com.example.projectchat3.ui.chats

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.chats.ChatRepository
import com.example.projectchat3.data.chats.Message
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
    private lateinit var chatId: String
    private lateinit var btnAttachImage: ImageView

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(ChatRepository(FirebaseFirestore.getInstance()), application)
    }

    // --- Permission & Image Picker ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                imagePickerLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để gửi ảnh.", Toast.LENGTH_SHORT).show()
            }
        }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val participants = listOf(currentUserId, chatUserId)
                viewModel.sendImageMessage(it, participants)
            }
        }
    // --- End ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Xử lý khoảng trống bàn phím & thanh điều hướng
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
        chatId = listOf(currentUserId, chatUserId).sorted().joinToString("_")

        tvTitle = findViewById(R.id.tvTitle)
        FirebaseFirestore.getInstance().collection("users").document(chatUserId).get()
            .addOnSuccessListener { doc ->
                val chatUser = doc.toObject(User::class.java)
                tvTitle.text = chatUser?.name ?: "User"
            }

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        btnAttachImage = findViewById(R.id.btnAttach)
        btnAttachImage.setOnClickListener { checkPermissionAndPickImage() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMessages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        adapter = MessageAdapter(
            messages = mutableListOf(),
            currentUserId = currentUserId,
            onEditMessage = { message -> showEditDialog(message) },
            onDeleteMessage = { message -> showDeleteDialog(message) }
        )
        recyclerView.adapter = adapter

        val edtMessage = findViewById<EditText>(R.id.editMessage)
        val btnSend = findViewById<ImageView>(R.id.btnSend)
        val participants = listOf(currentUserId, chatUserId)

        fun sendMessage() {
            val text = edtMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val msg = Message(senderId = currentUserId, text = text)
                viewModel.sendMessage(msg, participants)
                edtMessage.text.clear()
            } else {
                edtMessage.error = "Nhập nội dung trước khi gửi nhé!"
            }
        }

        btnSend.setOnClickListener { sendMessage() }
        edtMessage.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendMessage(); true
            } else false
        }

        viewModel.getOrCreateChat(participants) { id ->
            id?.let {
                chatId = it
                viewModel.loadMessages(it)
                viewModel.messages.observe(this) { list ->
                    adapter.updateMessages(list)
                    if (list.isNotEmpty()) recyclerView.scrollToPosition(list.size - 1)
                }
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                imagePickerLauncher.launch("image/*")
            else ->
                requestPermissionLauncher.launch(permission)
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
                    viewModel.updateMessage(chatId, message.id, newText)
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
                viewModel.deleteMessage(chatId, message.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
