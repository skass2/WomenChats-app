package com.example.projectchat3.ui.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projectchat3.data.friends.Friendship
import com.example.projectchat3.data.friends.FriendshipRepository

class FriendshipViewModel(private val repo: FriendshipRepository) : ViewModel() {

    private val _incomingRequests = MutableLiveData<List<Friendship>>()   // lời mời đến
    val incomingRequests: LiveData<List<Friendship>> = _incomingRequests

    private val _friends = MutableLiveData<List<Friendship>>()            // danh sách bạn
    val friends: LiveData<List<Friendship>> = _friends

    private val _actionResult = MutableLiveData<Boolean>()                // kết quả thao tác
    val actionResult: LiveData<Boolean> = _actionResult

    /** Gửi lời mời kết bạn */
    fun sendRequest(currentUid: String, friendUid: String) {
        repo.sendRequest(currentUid, friendUid) { success ->
            _actionResult.postValue(success)
        }
    }

    /** Chấp nhận lời mời */
    fun acceptRequest(request: Friendship) {
        repo.acceptRequest(request) { success ->
            _actionResult.postValue(success)
            if (success) loadFriends(getOtherUid(request)) // load lại bạn bè
        }
    }

    /** Từ chối hoặc hủy lời mời */
    fun rejectRequest(request: Friendship) {
        repo.rejectRequest(request) { success ->
            _actionResult.postValue(success)
            if (success) loadIncomingRequests(getOtherUid(request)) // load lại request
        }
    }

    /** Load danh sách lời mời đến */
    fun loadIncomingRequests(currentUid: String) {
        repo.getIncomingRequests(currentUid) { list ->
            _incomingRequests.postValue(list)
        }
    }

    /** Load danh sách bạn bè */
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
