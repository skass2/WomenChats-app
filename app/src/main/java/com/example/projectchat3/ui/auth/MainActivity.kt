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
import com.example.projectchat3.ui.MainHomeActivity
import com.example.projectchat3.ui.users.ProfileSetupActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false
    private lateinit var progressBar: android.widget.ProgressBar

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
        progressBar = findViewById(R.id.progressBarLogin)

        //Nếu user đã đăng nhập thì vào thẳng MainHomeActivity
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

            progressBar.visibility = android.view.View.VISIBLE
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser ?: return@addOnCompleteListener

                        // --- LOGIC MỚI BẮT ĐẦU TỪ ĐÂY ---
                        var isProceeded = false
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())

                        // 1. Đặt một "đồng hồ hẹn giờ" 3 giây
                        // Nếu sau 3 giây mà reload() chưa xong, ta sẽ tự động tiếp tục
                        handler.postDelayed({
                            if (!isProceeded) {
                                isProceeded = true
                                proceedToNextScreen(user)
                            }
                        }, 3000) // 3 giây timeout

                        // 2. Cố gắng reload() trạng thái user
                        user.reload().addOnCompleteListener {
                            if (!isProceeded) {
                                isProceeded = true
                                proceedToNextScreen(user)
                            }
                        }

                    } else {
                        progressBar.visibility = android.view.View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(this, "Sai tài khoản hoặc mật khẩu: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    }
    // Tách ra một hàm riêng để xử lý logic chuyển màn hình
    private fun proceedToNextScreen(user: com.google.firebase.auth.FirebaseUser) {
        // Luôn kiểm tra lại trạng thái email_verified sau khi đã reload (hoặc timeout)
        if (!user.isEmailVerified) {
            Toast.makeText(this, "Email chưa xác thực. Vui lòng kiểm tra hộp thư!", Toast.LENGTH_LONG).show()
            auth.signOut()
            return
        }

        val uid = user.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && !doc.getString("name").isNullOrEmpty()) {
                    startActivity(Intent(this, MainHomeActivity::class.java))
                } else {
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                // ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT: BẮT LỖI KHI ĐỌC FIRESTORE
                Toast.makeText(this, "Lỗi đọc dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                // Có thể người dùng chưa kịp tạo profile, chuyển họ đến màn hình tạo
                startActivity(Intent(this, ProfileSetupActivity::class.java))
                finish()
            }
    }
}
