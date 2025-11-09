package com.example.projectchat3.ui.users

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.projectchat3.data.users.User
import com.example.projectchat3.data.users.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileViewModel : ViewModel() {

    private val repo = UserRepository(
        db = FirebaseFirestore.getInstance(),
        storage = FirebaseStorage.getInstance()
    )

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    //  Load user hiện tại
    fun loadUser() {
        _loading.value = true
        repo.getCurrentUser { u ->
            _loading.postValue(false)
            if (u != null) {
                _user.postValue(u)
            } else {
                _error.postValue("Không thể tải thông tin người dùng")
            }
        }
    }

    // ProfileViewModel.kt

    fun changeUserName(newName: String, callback: (Boolean, String?) -> Unit) {
        if (newName.isBlank()) {
            callback(false, "Tên không được để trống")
            return
        }

        _loading.value = true
        repo.updateUserName(newName) { success ->
            _loading.postValue(false)
            if (success) {
                // Cập nhật LiveData user trong ViewModel
                _user.value = _user.value?.copy(name = newName)
                callback(true, null)
            } else {
                callback(false, "Cập nhật tên thất bại")
            }
        }
    }


    //  Đổi avatar (upload Firebase Storage + Firestore)
    fun changeAvatar(uri: Uri) {
        _loading.value = true
        repo.updateUserAvatar(uri) { url ->
            _loading.postValue(false)
            if (url != null) {
                _user.value = _user.value?.copy(avatarUrl = url)
            } else {
                _error.postValue("Cập nhật avatar thất bại")
            }
        }
    }
}
