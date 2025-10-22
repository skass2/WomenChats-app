package com.example.projectchat3.data.users

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UserRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser(onResult: (User?) -> Unit) {
        val currentUid = auth.uid
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    onResult(doc.toObject(User::class.java))
                }
                .addOnFailureListener { onResult(null) }
        } else {
            onResult(null)
        }
    }

    fun loadUsers(onResult: (List<User>) -> Unit) {
        val currentUid = auth.uid ?: return onResult(emptyList())

        // Lấy tất cả friendship liên quan đến current user
        db.collection("friendships")
            .whereArrayContains("participants", currentUid)
            .get()
            .addOnSuccessListener { friendshipSnap ->
                val relatedUids = friendshipSnap.documents.flatMap { doc ->
                    (doc["participants"] as? List<String>).orEmpty()
                }.toSet()

                // Sau khi có danh sách liên quan, lấy toàn bộ users
                db.collection("users").get()
                    .addOnSuccessListener { result ->
                        val users = result.mapNotNull { it.toObject(User::class.java) }
                            .filter { it.uid != currentUid && it.uid !in relatedUids }
                        onResult(users)
                    }
                    .addOnFailureListener { onResult(emptyList()) }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val users = result.mapNotNull { it.toObject(User::class.java) }
                    .filter { it.uid != auth.uid }
                onResult(users)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getUserById(uid: String, onResult: (User?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(User::class.java))
            }
            .addOnFailureListener { onResult(null) }
    }

    // Cập nhật tên hiển thị
    fun updateUserName(newName: String, onResult: (Boolean) -> Unit) {
        val uid = auth.uid ?: return onResult(false)
        db.collection("users").document(uid)
            .update("name", newName)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // Upload avatar và cập nhật Firestore
    fun updateUserAvatar(uri: Uri, onResult: (String?) -> Unit) {
        val uid = auth.uid ?: return onResult(null)
        val userDocRef = db.collection("users").document(uid)

        // Ghi chú: Bắt đầu bằng việc lấy thông tin người dùng hiện tại
        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            // Bước 1: Lấy URL của ảnh cũ (nếu có)
            val oldAvatarUrl = documentSnapshot.getString("avatarUrl")

            // Bước 2: Tạo tên tệp mới và Upload
            val fileRef = storage.reference.child("avatars/$uid/${UUID.randomUUID()}.jpg")
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    // Bước 3: Lấy URL tải xuống của ảnh mới
                    fileRef.downloadUrl.addOnSuccessListener { newDownloadUri ->
                        val newUrl = newDownloadUri.toString()

                        // Bước 4: Cập nhật Firestore với URL mới
                        userDocRef.update("avatarUrl", newUrl)
                            .addOnSuccessListener {
                                // Ghi chú: Sau khi cập nhật DB thành công, ta mới tiến hành xóa ảnh cũ

                                // Bước 5: Xóa ảnh cũ khỏi Storage
                                if (!oldAvatarUrl.isNullOrEmpty()) {
                                    try {
                                        val oldStorageRef = storage.getReferenceFromUrl(oldAvatarUrl)
                                        oldStorageRef.delete()
                                            .addOnSuccessListener {
                                                // Xóa thành công, không cần thông báo cho người dùng
                                                println("SUCCESS: Old avatar deleted.")
                                            }
                                            .addOnFailureListener {
                                                // Xóa thất bại, có thể log lại để debug
                                                println("ERROR: Failed to delete old avatar: ${it.message}")
                                            }
                                    } catch (e: Exception) {
                                        println("ERROR: Invalid old avatar URL: ${e.message}")
                                    }
                                }

                                // Trả về URL mới cho ViewModel để cập nhật UI
                                onResult(newUrl)
                            }
                            .addOnFailureListener {
                                // Cập nhật Firestore thất bại
                                onResult(null)
                            }
                    }
                }
                .addOnFailureListener {
                    // Upload ảnh mới thất bại
                    onResult(null)
                }
        }.addOnFailureListener {
            // Không thể đọc thông tin người dùng ban đầu
            onResult(null)
        }
    }
    fun saveDeviceToken(token: String) {
        val uid = auth.uid ?: return
        val userRef = db.collection("users").document(uid)

        val updates = mapOf(
            "fcmToken" to token,
            "tokenUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        userRef.update(updates)
            .addOnSuccessListener {
                Log.d("UserRepository", "FCM token updated successfully for $uid")
            }
            .addOnFailureListener { e ->
                Log.e("UserRepository", "Failed to update FCM token", e)
            }
    }
}
