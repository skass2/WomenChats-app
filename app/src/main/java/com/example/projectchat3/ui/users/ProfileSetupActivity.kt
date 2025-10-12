package com.example.projectchat3.ui.users

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectchat3.databinding.ActivityProfileSetupBinding
import com.example.projectchat3.ui.MainHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // chọn ảnh
        binding.btnChooseAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // lưu thông tin
        binding.btnSave.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val uid = auth.currentUser?.uid

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (uid == null) {
                Toast.makeText(this, "Không xác định được UID người dùng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Nếu có chọn ảnh thì upload trước
            if (imageUri != null) {
                val ref = storage.reference.child("avatars/$uid/${UUID.randomUUID()}.jpg")
                ref.putFile(imageUri!!)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { url ->
                            saveUserProfile(uid, name, url.toString())
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Không lấy được link ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Upload ảnh thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Không chọn ảnh thì vẫn lưu với avatar rỗng
                saveUserProfile(uid, name, "")
            }
        }
    }

    private fun saveUserProfile(uid: String, name: String, avatarUrl: String) {
        val user = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to auth.currentUser?.email,
            "avatarUrl" to avatarUrl
        )

        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainHomeActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi lưu dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            imageUri = data?.data
            binding.imgAvatar.setImageURI(imageUri) // preview ảnh
        }
    }
}
