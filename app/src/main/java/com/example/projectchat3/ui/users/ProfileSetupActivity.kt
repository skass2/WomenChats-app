package com.example.projectchat3.ui.users

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.projectchat3.databinding.ActivityProfileSetupBinding
import com.example.projectchat3.ui.MainHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    // --- Permission & Image Picker ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                imagePickerLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để chọn ảnh đại diện.", Toast.LENGTH_SHORT).show()
            }
        }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                binding.imgAvatar.setImageURI(it)
            }
        }
    // --- End ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.btnChooseAvatar.setOnClickListener { checkPermissionAndPickImage() }

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

            binding.btnSave.isEnabled = false
            binding.progressBarSetup.visibility = View.VISIBLE

            if (imageUri != null) {
                val ref = storage.reference.child("avatars/$uid/${UUID.randomUUID()}.jpg")
                ref.putFile(imageUri!!)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { url ->
                            saveUserProfile(uid, name, url.toString())
                        }.addOnFailureListener { e ->
                            handleFailure("Không lấy được link ảnh: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        handleFailure("Upload ảnh thất bại: ${e.message}")
                    }
            } else {
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
                handleFailure("Lỗi lưu dữ liệu: ${e.message}")
            }
    }

    private fun handleFailure(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        binding.btnSave.isEnabled = true
        binding.progressBarSetup.visibility = View.GONE
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                imagePickerLauncher.launch("image/*")
            else ->
                requestPermissionLauncher.launch(permission)
        }
    }
}
