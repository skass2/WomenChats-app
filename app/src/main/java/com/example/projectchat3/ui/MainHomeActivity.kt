package com.example.projectchat3.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.projectchat3.R
import com.example.projectchat3.data.users.UserRepository
import com.example.projectchat3.ui.chats.ChatListFragment
import com.example.projectchat3.ui.friends.FriendListFragment
import com.example.projectchat3.ui.friends.FriendRequestFragment
import com.example.projectchat3.ui.users.SearchUserFragment
import com.example.projectchat3.ui.users.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainHomeActivity : AppCompatActivity() {

    // --- PHẦN MỚI: Khai báo trình khởi chạy yêu cầu quyền ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bạn sẽ không nhận được thông báo tin nhắn mới.", Toast.LENGTH_LONG).show()
        }
    }
    // --- KẾT THÚC PHẦN MỚI ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> loadFragment(ChatListFragment())
                R.id.nav_friends -> loadFragment(FriendListFragment())
                R.id.nav_add_friend -> loadFragment(SearchUserFragment())
                R.id.nav_requests -> loadFragment(FriendRequestFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        // --- GỌI CÁC HÀM MỚI ---
        askNotificationPermission()
        getAndSaveFcmToken()
        // --- KẾT THÚC GỌI HÀM MỚI ---

        bottomNav.selectedItemId = R.id.nav_chat
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // --- HÀM MỚI: Logic xin quyền thông báo ---
    private fun askNotificationPermission() {
        // Chỉ cần xin quyền trên Android 13 (API 33) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Quyền đã được cấp, không cần làm gì
            } else {
                // Quyền chưa được cấp, hiển thị hộp thoại xin quyền
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // --- KẾT THÚC HÀM MỚI ---

    private fun getAndSaveFcmToken() {
        val userRepo = UserRepository(FirebaseFirestore.getInstance())
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "Token fetched in MainHomeActivity: $token")
            if (token != null) {
                userRepo.saveDeviceToken(token)
            }
        }
    }
}