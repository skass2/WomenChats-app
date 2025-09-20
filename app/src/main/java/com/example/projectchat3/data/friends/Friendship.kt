package com.example.projectchat3.data.friends

import com.google.firebase.Timestamp

data class Friendship(
    val id: String = "",                     // uid1_uid2
    val participants: List<String> = listOf(), // [uid1, uid2]
    val status: String = "pending",          // "pending" | "accepted" | "blocked"
    val requestBy: String = "",              // uid của user gửi request
    val createdAt: Timestamp? = null,        // thời điểm tạo
    val updatedAt: Timestamp? = null         // thời điểm update
)