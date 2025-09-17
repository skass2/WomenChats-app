package com.example.projectchat3.ui.chat

import com.example.projectchat3.data.chats.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projectchat3.data.chats.ChatRepository

class ChatViewModel(private val repo: ChatRepository) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    fun loadMessages(roomId: String) {
        repo.listenMessages(roomId) { list ->
            _messages.postValue(list)
        }
    }

    fun sendMessage(roomId: String, message: Message) {
        repo.sendMessage(roomId, message) {}
    }

    fun updateMessage(roomId: String, messageId: String, newText: String) {
        repo.updateMessage(roomId, messageId, newText) {}
    }

    fun deleteMessage(roomId: String, messageId: String) {
        repo.deleteMessage(roomId, messageId) {}
    }
}

class ChatViewModelFactory(private val repo: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
