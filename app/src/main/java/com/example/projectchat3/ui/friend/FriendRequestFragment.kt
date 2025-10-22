package com.example.projectchat3.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectchat3.R
import com.example.projectchat3.data.friends.FriendshipRepository
import com.example.projectchat3.data.users.User
import com.example.projectchat3.ui.friend.FriendshipViewModel
import com.example.projectchat3.ui.friend.FriendshipViewModelFactory
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestFragment : Fragment() {

    private lateinit var receivedAdapter: FriendRequestAdapter
    private lateinit var sentAdapter: FriendRequestAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().uid ?: ""

    private val friendViewModel: FriendshipViewModel by viewModels {
        FriendshipViewModelFactory(FriendshipRepository(FirebaseFirestore.getInstance()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_request, container, false)

        val recyclerReceived = view.findViewById<RecyclerView>(R.id.recyclerFriendRequestsReceived)
        recyclerReceived.layoutManager = LinearLayoutManager(requireContext())
        receivedAdapter = FriendRequestAdapter(
            type = RequestType.RECEIVED,
            requests = emptyList(),
            onAccept = { user -> acceptRequest(user) },
            onReject = { user -> declineRequest(user) }
        )
        recyclerReceived.adapter = receivedAdapter

        val recyclerSent = view.findViewById<RecyclerView>(R.id.recyclerFriendRequestsSent)
        recyclerSent.layoutManager = LinearLayoutManager(requireContext())
        sentAdapter = FriendRequestAdapter(
            type = RequestType.SENT,
            requests = emptyList(),
            onAccept = {},
            onReject = { user -> cancelRequest(user) }
        )
        recyclerSent.adapter = sentAdapter

        observeViewModel()
        friendViewModel.loadIncomingRequests(currentUid)
        friendViewModel.loadSentRequests(currentUid)

        return view
    }

    private fun observeViewModel() {
        friendViewModel.incomingRequests.observe(viewLifecycleOwner) { requests ->
            if (requests.isEmpty()) {
                receivedAdapter.updateList(emptyList())
                return@observe
            }

            val tasks = requests.mapNotNull { request ->
                val otherUid = request.participants.firstOrNull { it != currentUid }
                otherUid?.let {
                    db.collection("users").document(it).get().continueWith { task ->
                        task.result?.toObject(User::class.java)
                    }
                }
            }

            Tasks.whenAllSuccess<User>(tasks).addOnSuccessListener { users ->
                receivedAdapter.updateList(users)
            }
        }

        friendViewModel.sentRequests.observe(viewLifecycleOwner) { requests ->
            if (requests.isEmpty()) {
                sentAdapter.updateList(emptyList())
                return@observe
            }

            val tasks = requests.mapNotNull { request ->
                val otherUid = request.participants.firstOrNull { it != currentUid }
                otherUid?.let {
                    db.collection("users").document(it).get().continueWith { task ->
                        task.result?.toObject(User::class.java)
                    }
                }
            }

            Tasks.whenAllSuccess<User>(tasks).addOnSuccessListener { users ->
                sentAdapter.updateList(users)
            }
        }

        friendViewModel.actionResult.observe(viewLifecycleOwner) { success ->
            Toast.makeText(
                requireContext(),
                if (success) "Thao tác thành công" else "Thao tác thất bại, thử lại sau",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun acceptRequest(user: User) {
        val request = friendViewModel.incomingRequests.value
            ?.firstOrNull { it.participants.contains(user.uid) } ?: return
        friendViewModel.acceptRequest(request)
    }

    private fun declineRequest(user: User) {
        val request = friendViewModel.incomingRequests.value
            ?.firstOrNull { it.participants.contains(user.uid) } ?: return
        friendViewModel.rejectRequest(request)
    }

    private fun cancelRequest(user: User) {
        val request = friendViewModel.sentRequests.value
            ?.firstOrNull { it.participants.contains(user.uid) } ?: return
        friendViewModel.cancelRequest(request)
    }
}
