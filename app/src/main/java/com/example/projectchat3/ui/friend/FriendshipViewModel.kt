package com.example.projectchat3.ui.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projectchat3.data.friends.Friendship
import com.example.projectchat3.data.friends.FriendshipRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FriendshipViewModel(private val repo: FriendshipRepository) : ViewModel() {

    private val _incomingRequests = MutableLiveData<List<Friendship>>()   // lời mời đến
    val incomingRequests: LiveData<List<Friendship>> = _incomingRequests

    private val _sentRequests = MutableLiveData<List<Friendship>>()       // lời mời đã gửi
    val sentRequests: LiveData<List<Friendship>> = _sentRequests

    private val _friends = MutableLiveData<List<Friendship>>()            // danh sách bạn
    val friends: LiveData<List<Friendship>> = _friends

    private val _actionResult = MutableLiveData<Boolean>()                // kết quả thao tác
    val actionResult: LiveData<Boolean> = _actionResult

    /** Gửi lời mời kết bạn */
    fun sendRequest(currentUid: String, friendUid: String, onResult: (Boolean) -> Unit) {
        repo.sendRequest(currentUid, friendUid) { success ->
            _actionResult.postValue(success)
            if (success) loadSentRequests(currentUid)
            onResult(success)
        }
    }

    /** Chấp nhận lời mời */
    fun acceptRequest(request: Friendship) {
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        val otherUid = request.participants.first { it != currentUid }

        viewModelScope.launch {
            try {
                repo.acceptRequest(request.id, currentUid, otherUid)
                _actionResult.postValue(true)
                loadFriends(currentUid)
                loadIncomingRequests(currentUid)
            } catch (_: Exception) {
                _actionResult.postValue(false)
            }
        }
    }

    /** Từ chối lời mời đến */
    fun rejectRequest(request: Friendship) {
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        repo.rejectRequest(request) { success ->
            _actionResult.postValue(success)
            if (success) loadIncomingRequests(currentUid)
        }
    }

    /** Hủy lời mời đã gửi */
    fun cancelRequest(request: Friendship) {
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        repo.rejectRequest(request) { success ->
            _actionResult.postValue(success)
            if (success) loadSentRequests(currentUid)
        }
    }

    /** Lấy danh sách lời mời đến */
    fun loadIncomingRequests(currentUid: String) {
        repo.getIncomingRequests(currentUid) { list ->
            _incomingRequests.postValue(list)
        }
    }

    /** Lấy danh sách lời mời đã gửi */
    fun loadSentRequests(currentUid: String) {
        repo.getSentRequests(currentUid) { list ->
            _sentRequests.postValue(list)
        }
    }

    /** Lấy danh sách bạn bè */
    fun loadFriends(currentUid: String) {
        repo.getFriends(currentUid) { list ->
            _friends.postValue(list)
        }
    }

    /** Helper: lấy uid còn lại trong request */
    private fun getOtherUid(request: Friendship): String {
        return request.participants.firstOrNull { it != request.requestBy } ?: ""
    }
}

class FriendshipViewModelFactory(
    private val repo: FriendshipRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendshipViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendshipViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
