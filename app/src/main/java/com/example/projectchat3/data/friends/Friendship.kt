package com.example.projectchat3.data.friends

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Friendship(
    @get:Exclude val id: String = "",                     // uid1_uid2
    val participants: List<String> = listOf(), // [uid1, uid2]
    val status: String = "pending",          // "pending" | "accepted" | "blocked"
    val requestBy: String = "",              // uid của user gửi request
    val createdAt: Timestamp? = null,        // thời điểm tạo
    val updatedAt: Timestamp? = null         // thời điểm update
)