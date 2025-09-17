package com.example.projectchat3.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectchat3.R
import com.example.projectchat3.data.users.User
import com.example.projectchat3.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }
        setupSharedPasswordToggle(binding.edtPassword, binding.edtConfirmPassword)

        binding.btnRegister.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val confirm = binding.edtConfirmPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirm) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkUserExists(username, email) { exists, message ->
                if (exists) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: ""
                                val user = User(uid, username, email)

                                db.collection("users")
                                    .document(uid)
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Lỗi lưu dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    private fun checkUserExists(username: String, email: String, callback: (Boolean, String) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { usernameDocs ->
                if (!usernameDocs.isEmpty) {
                    callback(true, "Tên người dùng đã tồn tại")
                } else {
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { emailDocs ->
                            if (!emailDocs.isEmpty) {
                                callback(true, "Email đã được sử dụng")
                            } else {
                                callback(false, "")
                            }
                        }
                        .addOnFailureListener {
                            callback(true, "Lỗi kiểm tra email")
                        }
                }
            }
            .addOnFailureListener {
                callback(true, "Lỗi kiểm tra username")
            }
    }
    private fun setupSharedPasswordToggle(passwordEdit: EditText, confirmEdit: EditText) {
        val toggleListener = { editText: EditText ->
            editText.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width())) {
                        isPasswordVisible = !isPasswordVisible

                        if (isPasswordVisible) {
                            passwordEdit.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            confirmEdit.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            passwordEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide, 0)
                            confirmEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide, 0)
                        } else {
                            passwordEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            confirmEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            passwordEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide_off, 0)
                            confirmEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide_off, 0)
                        }

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