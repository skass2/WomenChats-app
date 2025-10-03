package com.example.projectchat3.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectchat3.R
import com.example.projectchat3.ui.auth.RegisterActivity
import com.example.projectchat3.ui.home.MainHomeActivity
import com.example.projectchat3.ui.users.ProfileSetupActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ Nếu user đã đăng nhập thì vào thẳng MainHomeActivity
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainHomeActivity::class.java))
            finish()
            return
        }

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val Register = findViewById<TextView>(R.id.txtRegister)

        // toggle password visible
        password.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (password.right - password.compoundDrawables[2].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        password.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide, 0)
                    } else {
                        password.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide_off, 0)
                    }
                    password.setSelection(password.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        Register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            if (email.text.toString().isEmpty() || password.text.toString().isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && !user.isEmailVerified) {
                            Toast.makeText(this, "Email chưa xác thực. Vui lòng kiểm tra hộp thư!", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            return@addOnCompleteListener
                        }

                        val uid = user?.uid ?: return@addOnCompleteListener
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists() && doc.getString("name") != null && doc.getString("name")!!.isNotEmpty()) {
                                    startActivity(Intent(this, MainHomeActivity::class.java))
                                } else {
                                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                                }
                                finish()
                            }
                    } else {
                        Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
