package com.example.projectchat3.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.projectchat3.R
import com.example.projectchat3.ui.friend.FriendListFragment
import com.example.projectchat3.ui.friend.FriendRequestFragment
import com.example.projectchat3.ui.users.SearchUserFragment
import com.example.projectchat3.ui.users.ChatListFragment
import com.example.projectchat3.ui.users.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainHomeActivity : AppCompatActivity() {

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

        // load mặc định tab chat
        bottomNav.selectedItemId = R.id.nav_chat
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
