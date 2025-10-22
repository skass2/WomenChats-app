package com.example.projectchat3.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.projectchat3.R
import com.example.projectchat3.ui.chats.ChatActivity
import com.example.projectchat3.ui.MainHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = data["title"]
        val body = data["body"]
        val avatarUrl = data["avatarUrl"]
        val senderUid = data["senderUid"]

        if (title != null && body != null && senderUid != null) {
            showCustomNotification(title, body, avatarUrl, senderUid)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showCustomNotification(title: String, body: String, avatarUrl: String?, senderUid: String) {
        val channelId = "new_message_channel"
        val notificationId = Random.nextInt()

        // --- TẠO BACK STACK HOÀN CHỈNH ---
        // 1. Tạo intent để mở màn hình chat, gắn UID của người gửi vào
        val chatIntent = Intent(this, ChatActivity::class.java).apply {
            putExtra("uid", senderUid)
        }

        // 2. Dùng TaskStackBuilder để tạo một "lối đi" nhân tạo
        // Khi người dùng bấm nút back từ ChatActivity, họ sẽ quay về MainHomeActivity
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Thêm MainHomeActivity vào "lối đi"
            addNextIntentWithParentStack(Intent(this@MyFirebaseMessagingService, MainHomeActivity::class.java))
            // Thêm ChatActivity lên trên cùng
            addNextIntent(chatIntent)
            // Lấy PendingIntent
            getPendingIntent(notificationId, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        // --- KẾT THÚC TẠO BACK STACK ---

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Gắn "lối đi" vào thông báo
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)

        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .asBitmap()
                .load(avatarUrl)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        builder.setLargeIcon(resource)
                        notificationManager.notify(notificationId, builder.build())
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        notificationManager.notify(notificationId, builder.build())
                    }
                })
        } else {
            notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun sendTokenToServer(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val updates = mapOf(
                "fcmToken" to token,
                "tokenUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates)
        }
    }
}