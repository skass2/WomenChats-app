package com.example.projectchat3.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectchat3.R
import com.example.projectchat3.ui.auth.MainActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvEmail = view.findViewById(R.id.tvEmail)
        btnLogout = view.findViewById(R.id.btnLogout)

        val currentUser = FirebaseAuth.getInstance().currentUser
        tvEmail.text = currentUser?.email ?: "Không rõ"

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
}
