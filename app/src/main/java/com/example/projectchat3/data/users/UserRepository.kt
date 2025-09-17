package com.example.projectchat3.data.users

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(private val db: FirebaseFirestore) {

    fun getCurrentUser(onResult: (User?) -> Unit) {
        val currentUid = FirebaseAuth.getInstance().uid
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
                    .filter { it.uid != FirebaseAuth.getInstance().uid }
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
                    .filter { it.uid != FirebaseAuth.getInstance().uid }
                onResult(users)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}
