package com.example.projectchat3.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.projectchat3.R
import com.example.projectchat3.ui.MainHomeActivity
import com.example.projectchat3.ui.users.ProfileSetupActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // --- Giao diện ---
        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        progressBar = findViewById(R.id.progressBarLogin)

        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)

        // --- Nhận email và mật khẩu từ VerifyEmailActivity ---
        intent.getStringExtra("email")?.let { emailField.setText(it) }
        intent.getStringExtra("password")?.let { passwordField.setText(it) }

        // --- Nếu user đã đăng nhập và đã xác thực ---
        auth.currentUser?.let { user ->
            if (user.isEmailVerified) {
                startActivity(Intent(this, MainHomeActivity::class.java))
                finish()
                return
            } else {
                auth.signOut()
            }
        }

        // --- Toggle hiện/ẩn mật khẩu ---
        passwordField.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = passwordField.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (passwordField.right - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    val inputType = if (isPasswordVisible)
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    passwordField.inputType = inputType

                    val icon = if (isPasswordVisible) R.drawable.hide else R.drawable.hide_off
                    passwordField.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
                    passwordField.setSelection(passwordField.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        // --- Chuyển đến đăng ký ---
        txtRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // --- Xử lý đăng nhập ---
        btnLogin.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser ?: return@addOnCompleteListener
                        user.reload().addOnSuccessListener {
                            if (user.isEmailVerified) {
                                handleVerifiedLogin(user)
                            } else {
                                progressBar.visibility = View.GONE
                                btnLogin.isEnabled = true
                                Toast.makeText(
                                    this,
                                    "Email chưa xác thực. Vui lòng kiểm tra hộp thư!",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                            }
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(
                            this,
                            "Sai tài khoản hoặc mật khẩu: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    // --- Khi user đã xác thực email ---
    private fun handleVerifiedLogin(user: com.google.firebase.auth.FirebaseUser) {
        val uid = user.uid
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                // 🔹 Tạo user mới chỉ khi chưa có
                val newUser = hashMapOf(
                    "uid" to uid,
                    "email" to (user.email ?: ""),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "name" to "",
                    "avatar" to ""
                )
                userRef.set(newUser)
                    .addOnSuccessListener {
                        navigateToProfileSetup()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi khi tạo user: ${it.message}", Toast.LENGTH_LONG).show()
                        navigateToProfileSetup()
                    }
            } else {
                // 🔹 Nếu user đã có thông tin
                if (!doc.getString("name").isNullOrEmpty()) {
                    navigateToMainHome()
                } else {
                    navigateToProfileSetup()
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Lỗi đọc dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
            navigateToProfileSetup()
        }
    }

    private fun navigateToMainHome() {
        progressBar.visibility = View.GONE
        startActivity(Intent(this, MainHomeActivity::class.java))
        finish()
    }

    private fun navigateToProfileSetup() {
        progressBar.visibility = View.GONE
        startActivity(Intent(this, ProfileSetupActivity::class.java))
        finish()
    }
}
