package com.example.projectchat3.data.users

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val fcmToken: String = "",
    val tokenUpdatedAt: com.google.firebase.Timestamp? = null
)
