package com.example.projectchat3.data.users

import android.net.Uri
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
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val users = result.mapNotNull { it.toObject(User::class.java) }
                    .filter { it.uid != auth.uid }
                onResult(users)
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

    // ✅ Cập nhật tên hiển thị
    fun updateUserName(newName: String, onResult: (Boolean) -> Unit) {
        val uid = auth.uid ?: return onResult(false)
        db.collection("users").document(uid)
            .update("name", newName)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // ✅ Upload avatar và cập nhật Firestore
    fun updateUserAvatar(uri: Uri, onResult: (String?) -> Unit) {
        val uid = auth.uid ?: return onResult(null)
        val fileRef = storage.reference.child("avatars/$uid-${UUID.randomUUID()}.jpg")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    db.collection("users").document(uid).update("avatarUrl", url)
                        .addOnSuccessListener { onResult(url) }
                        .addOnFailureListener { onResult(null) }
                }
            }
            .addOnFailureListener { onResult(null) }
    }
}
