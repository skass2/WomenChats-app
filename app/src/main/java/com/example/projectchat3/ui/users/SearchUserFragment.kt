package com.example.projectchat3.ui.users

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.friends.FriendshipRepository
import com.example.projectchat3.data.users.UserRepository
import com.example.projectchat3.ui.adapter.UserSearchAdapter
import com.example.projectchat3.ui.friend.FriendshipViewModel
import com.example.projectchat3.ui.friend.FriendshipViewModelFactory
import com.example.projectchat3.ui.user.UserViewModel
import com.example.projectchat3.ui.user.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchUserFragment : Fragment() {

    private lateinit var adapter: UserSearchAdapter
    private lateinit var currentUid: String

    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory(UserRepository(FirebaseFirestore.getInstance()))
    }
    private val friendViewModel: FriendshipViewModel by viewModels {
        FriendshipViewModelFactory(FriendshipRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_user, container, false)

        currentUid = FirebaseAuth.getInstance().uid ?: ""

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerUsers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Dùng UserSearchAdapter (có nút Add Friend)
        adapter = UserSearchAdapter(mutableListOf()) { user ->
            if (user.uid != currentUid) {
                friendViewModel.sendRequest(currentUid, user.uid)
                Toast.makeText(requireContext(), "Đã gửi lời mời kết bạn!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        recyclerView.adapter = adapter

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                userViewModel.searchUsers(s.toString())
            }
        })

        // Quan sát LiveData users
        userViewModel.users.observe(viewLifecycleOwner) { list ->
            adapter.updateUsers(list)
        }

        // load tất cả user ban đầu
        userViewModel.loadUsers()

        return view
    }
}
