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

        //  Adapter xử lý gửi lời mời thật (Firestore)
        adapter = UserSearchAdapter(mutableListOf()) { user, onResult ->
            if (user.uid != currentUid) {
                friendViewModel.sendRequest(currentUid, user.uid) { success ->
                    onResult(success)
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            "Đã gửi lời mời kết bạn đến ${user.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Gửi lời mời thất bại, thử lại sau",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                onResult(false)
            }
        }

        recyclerView.adapter = adapter

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    userViewModel.searchUsers(query)
                } else {
                    adapter.updateUsers(emptyList())
                }
            }
        })

        //  Quan sát danh sách người dùng được tìm thấy
        userViewModel.users.observe(viewLifecycleOwner) { list ->
            adapter.updateUsers(list)
        }

        //  Quan sát danh sách bạn bè
        friendViewModel.friends.observe(viewLifecycleOwner) { friendships ->
            if (currentUid.isBlank()) return@observe
            val friendUids = friendships.mapNotNull { fr ->
                fr.participants.firstOrNull { it != currentUid }
            }.distinct()
            adapter.markFriends(friendUids)
        }

        //  Quan sát danh sách lời mời đã gửi
        friendViewModel.sentRequests.observe(viewLifecycleOwner) { requests ->
            if (currentUid.isBlank()) return@observe
            val sentUids = requests.mapNotNull { fr ->
                fr.participants.firstOrNull { it != currentUid }
            }.distinct()
            adapter.markSentRequests(sentUids)
        }

        //  Load dữ liệu ban đầu
        if (currentUid.isNotBlank()) {
            friendViewModel.loadFriends(currentUid)
            friendViewModel.loadSentRequests(currentUid)
        }

        return view
    }
}
