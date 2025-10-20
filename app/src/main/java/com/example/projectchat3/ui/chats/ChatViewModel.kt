package com.example.projectchat3.ui.chat

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.projectchat3.data.chats.ChatRepository
import com.example.projectchat3.data.chats.Message

class ChatViewModel(private val repo: ChatRepository, private val app: Application) : AndroidViewModel(app) {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    /**
     * Load tin nhắn từ 1 phòng chat có sẵn
     */
    fun loadMessages(roomId: String) {
        repo.listenMessages(roomId) { list ->
            _messages.postValue(list)
        }
    }

    /**
     * Gửi tin nhắn: nếu phòng chat chưa có thì tự động tạo mới
     */
    fun getOrCreateChat(participants: List<String>, onResult: (String?) -> Unit) {
        repo.getOrCreateChat(participants) { chatId ->
            if (chatId == null) {
                Toast.makeText(app, "❌ Không thể tạo hoặc lấy phòng chat!", Toast.LENGTH_SHORT).show()
            }
            onResult(chatId)
        }
    }

    fun sendMessage(message: Message, participants: List<String>) {
        repo.getOrCreateChat(participants) { chatId ->
            if (chatId != null) {
                repo.sendMessage(chatId, message, participants) { success ->
                    if (!success) {
                        Toast.makeText(app, "❌ Gửi tin nhắn thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(app, "❌ Không thể tạo hoặc lấy phòng chat!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateMessage(roomId: String, messageId: String, newText: String) {
        repo.updateMessage(roomId, messageId, newText) { success ->
            if (!success) {
                Toast.makeText(app, "❌ Sửa tin nhắn thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteMessage(roomId: String, messageId: String) {
        repo.deleteMessage(roomId, messageId) { success ->
            if (!success) {
                Toast.makeText(app, "❌ Xóa tin nhắn thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun sendImageMessage(imageUri: Uri, participants: List<String>) {
        val senderId = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        repo.getOrCreateChat(participants) { chatId ->
            if (chatId != null) {
                repo.sendImageMessage(chatId, imageUri, senderId, participants) { success ->
                    if (!success) {
                        Toast.makeText(app, "❌ Gửi ảnh thất bại!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(app, "❌ Không thể tạo phòng chat!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class ChatViewModelFactory(
    private val repo: ChatRepository,
    private val app: Application
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repo, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
