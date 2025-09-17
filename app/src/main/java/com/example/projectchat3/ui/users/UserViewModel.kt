package com.example.projectchat3.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projectchat3.data.users.User
import com.example.projectchat3.data.users.UserRepository

class UserViewModel(private val repo: UserRepository) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    fun loadCurrentUser() {
        repo.getCurrentUser { user ->
            _currentUser.postValue(user)
        }
    }

    fun loadUsers() {
        repo.loadUsers { list ->
            _users.postValue(list)
        }
    }

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            loadUsers()
        } else {
            repo.searchUsers(query) { list ->
                _users.postValue(list)
            }
        }
    }
}

class UserViewModelFactory(private val repo: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
