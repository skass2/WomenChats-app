package com.example.projectchat3.ui.users

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.ui.adapter.UserAdapter
import com.example.projectchat3.data.users.UserRepository
import com.example.projectchat3.ui.auth.MainActivity
import com.example.projectchat3.ui.chats.ChatActivity
import com.example.projectchat3.ui.user.UserViewModel
import com.example.projectchat3.ui.user.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserListActivity : AppCompatActivity() {
    private lateinit var adapter: UserAdapter

    private val viewModel: UserViewModel by viewModels {
        UserViewModelFactory(UserRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerUsers)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter(mutableListOf(), onUserClick = { user ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("uid", user.uid)
            startActivity(intent)
        })
        recyclerView.adapter = adapter

        // Observe current user
        viewModel.currentUser.observe(this) { user ->
            tvTitle.text = if (user != null) "Hello, ${user.name} 🙌🚩" else "User 🙌🚩"
        }

        // Observe users
        viewModel.users.observe(this) { list ->
            adapter.updateUsers(list)
        }

        // Search listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchUsers(s.toString())
            }
        })

        // 🔥 Logout button
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bố có chắc muốn đăng xuất không?")
                .setPositiveButton("Có") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .setNegativeButton("Không", null)
                .show()
        }

        // Load data
        viewModel.loadCurrentUser()
        viewModel.loadUsers()
    }
}
