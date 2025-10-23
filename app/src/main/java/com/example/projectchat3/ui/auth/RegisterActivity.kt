package com.example.projectchat3.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.projectchat3.R
import com.example.projectchat3.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = maxOf(imeInsets.bottom, navInsets.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
            insets
        }

        auth = FirebaseAuth.getInstance()

        binding.btnBackToLogin.setOnClickListener { finish() }
        setupSharedPasswordToggle(binding.edtPassword, binding.edtConfirmPassword)

        binding.btnRegister.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val confirm = binding.edtConfirmPassword.text.toString().trim()

            // Reset lỗi
            binding.txtPasswordError.visibility = View.GONE
            binding.txtConfirmError.visibility = View.GONE

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passError = validatePassword(password)
            if (passError != null) {
                binding.txtPasswordError.text = passError
                binding.txtPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password != confirm) {
                binding.txtConfirmError.text = "Mật khẩu xác nhận không khớp"
                binding.txtConfirmError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.progressBarRegister.visibility = View.VISIBLE
            binding.btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.progressBarRegister.visibility = View.GONE
                    binding.btnRegister.isEnabled = true

                    if (task.isSuccessful) {
                        auth.currentUser?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Đăng ký thành công! Vui lòng xác thực email.",
                                    Toast.LENGTH_LONG
                                ).show()

                                // ❗Không tạo Firestore user tại đây
                                // Chuyển sang màn xác thực
                                val intent = Intent(this, VerifyEmailActivity::class.java)
                                intent.putExtra("email", email)
                                intent.putExtra("password", password)
                                startActivity(intent)
                                finish()
                            }
                            ?.addOnFailureListener {
                                Toast.makeText(this, "Không gửi được email xác thực: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        binding.txtPasswordError.text = "Đăng ký thất bại: ${task.exception?.message}"
                        binding.txtPasswordError.visibility = View.VISIBLE
                    }
                }
        }
    }

    private fun validatePassword(password: String): String? {
        val errors = mutableListOf<String>()
        if (password.length < 6) errors.add("ít nhất 6 ký tự")
        if (password.length > 30) errors.add("không quá 30 ký tự")
        if (!password.any { it.isLowerCase() }) errors.add("ít nhất 1 chữ cái thường")
        if (!password.any { it.isDigit() }) errors.add("ít nhất 1 chữ số")
        if (!password.any { !it.isLetterOrDigit() }) errors.add("ít nhất 1 ký tự đặc biệt")

        return if (errors.isEmpty()) null else "Mật khẩu không hợp lệ: ${errors.joinToString(", ")}"
    }

    private fun setupSharedPasswordToggle(passwordEdit: EditText, confirmEdit: EditText) {
        val toggleListener = { editText: EditText ->
            editText.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width())) {
                        isPasswordVisible = !isPasswordVisible

                        val inputType = if (isPasswordVisible)
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        else
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                        passwordEdit.inputType = inputType
                        confirmEdit.inputType = inputType

                        val icon = if (isPasswordVisible) R.drawable.hide else R.drawable.hide_off
                        passwordEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
                        confirmEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)

                        editText.setSelection(editText.text.length)
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
        toggleListener(passwordEdit)
        toggleListener(confirmEdit)
    }
}
