package com.example.projectchat3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        firebaseAppCheck.getAppCheckToken(true)
            .addOnSuccessListener { tokenResult ->
                Log.d("MyApp_DebugToken", "Current Debug Token: ${tokenResult.token}")
            }
            .addOnFailureListener { e ->
                Log.e("MyApp_DebugToken", "Failed to get Debug Token", e)
            }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Chỉ cần tạo channel trên Android 8.0 (API 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "new_message_channel"
            val channelName = "Tin nhắn mới"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance)

            // --- ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT ĐỂ CÓ TIẾNG ---
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.www}")
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(soundUri, audioAttributes)
            // --- KẾT THÚC PHẦN ÂM THANH ---

            // Đăng ký channel với hệ thống
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}