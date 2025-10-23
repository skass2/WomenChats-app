package com.example.projectchat3.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectchat3.databinding.ActivityVerifyEmailBinding
import com.google.firebase.auth.FirebaseAuth

class VerifyEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyEmailBinding
    private lateinit var auth: FirebaseAuth
    private var cooldown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // --- Lấy email & password từ intent gửi sang ---
        val regEmail = intent.getStringExtra("email") ?: auth.currentUser?.email ?: ""
        val regPassword = intent.getStringExtra("password") ?: ""

        // --- Hiển thị email bị ẩn ---
        binding.txtEmail.text = maskEmail(regEmail)

        startCooldown()

        // --- Gửi lại email xác thực ---
        binding.btnResend.setOnClickListener {
            auth.currentUser?.sendEmailVerification()?.addOnSuccessListener {
                Toast.makeText(this, "Đã gửi lại email xác thực", Toast.LENGTH_SHORT).show()
                startCooldown()
            }?.addOnFailureListener {
                Toast.makeText(this, "Gửi lại thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Kiểm tra trạng thái xác thực ---
        binding.btnCheckVerified.setOnClickListener {
            auth.currentUser?.reload()?.addOnSuccessListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show()

                    auth.signOut()

                    // Trả lại màn hình login, truyền kèm email + password
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("email", regEmail)
                    intent.putExtra("password", regPassword)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Bạn chưa xác thực email", Toast.LENGTH_SHORT).show()
                }
            }?.addOnFailureListener {
                Toast.makeText(this, "Không thể kiểm tra trạng thái: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Quay lại đăng nhập ---
        binding.btnBackLogin.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // --- Bộ đếm thời gian cho nút gửi lại ---
    private fun startCooldown() {
        binding.btnResend.isEnabled = false
        cooldown?.cancel()
        cooldown = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.btnResend.text = "Gửi lại sau ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.btnResend.isEnabled = true
                binding.btnResend.text = "Gửi lại email"
            }
        }.start()
    }

    // --- Ẩn bớt phần tên trong email ---
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        val hiddenUsername = when {
            username.length <= 2 -> username.first() + "*"
            username.length <= 4 -> username.take(2) + "*".repeat(username.length - 2)
            else -> username.take(3) + "*".repeat(username.length - 5) + username.takeLast(2)
        }

        return "$hiddenUsername@$domain"
    }

    override fun onDestroy() {
        super.onDestroy()
        cooldown?.cancel()
    }
}
