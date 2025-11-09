package com.example.projectchat3.ui.users

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.projectchat3.R
import com.example.projectchat3.ui.auth.MainActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var imgAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnChangeAvatar: ImageButton
    private lateinit var btnChangeName: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) imagePickerLauncher.launch("image/*")
            else Toast.makeText(requireContext(), "Bạn cần cấp quyền để đổi ảnh.", Toast.LENGTH_SHORT).show()
        }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.changeAvatar(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar)
        btnChangeName = view.findViewById(R.id.btnChangeName)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout = view.findViewById(R.id.btnLogout)
        progressBar = view.findViewById(R.id.progressBar)

        setupObservers()
        setupActions()
        viewModel.loadUser()

        return view
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvName.text = it.name
                tvEmail.text = it.email
                Glide.with(this)
                    .load(it.avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar)
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun setupActions() {
        btnChangeAvatar.setOnClickListener { checkPermissionAndPickImage() }
        btnChangeName.setOnClickListener { showChangeNameDialog() }
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finishAffinity()
        }
    }

    private fun showChangeNameDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_change_name, null)
        val edtName = view.findViewById<EditText>(R.id.edtNewName)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).setCancelable(false).create()

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val newName = edtName.text.toString().trim()
            tvError.text = ""
            if (newName.isEmpty()) {
                tvError.text = "Tên không được để trống!"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            viewModel.changeUserName(newName) { success, message ->
                progressBar.visibility = View.GONE
                if (success) {
                    Toast.makeText(requireContext(), "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    tvError.text = message ?: "Có lỗi xảy ra!"
                }
            }
        }
        dialog.show()
    }

    private var isOldVisible = false
    private var isNewVisible = false
    private var isConfirmVisible = false

    private fun showChangePasswordDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val edtOld = view.findViewById<EditText>(R.id.edtOldPassword)
        val edtNew = view.findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirm = view.findViewById<EditText>(R.id.edtConfirmPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val progress = view.findViewById<ProgressBar>(R.id.progressBar)

        setupToggleEye(edtOld, ::isOldVisible)
        setupToggleEye(edtNew, ::isNewVisible)
        setupToggleEye(edtConfirm, ::isConfirmVisible)

        val dialog = AlertDialog.Builder(requireContext()).setView(view).setCancelable(false).create()
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val oldPass = edtOld.text.toString().trim()
            val newPass = edtNew.text.toString().trim()
            val confirmPass = edtConfirm.text.toString().trim()

            tvError.text = ""

            when {
                oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty() -> {
                    tvError.text = "Vui lòng nhập đầy đủ thông tin."
                    return@setOnClickListener
                }
                newPass.length < 6 -> {
                    tvError.text = "Mật khẩu mới phải ít nhất 6 ký tự."
                    return@setOnClickListener
                }
                newPass != confirmPass -> {
                    tvError.text = "Xác nhận mật khẩu không khớp."
                    return@setOnClickListener
                }
            }

            val user = auth.currentUser
            val email = user?.email
            if (email == null) {
                tvError.text = "Không thể xác thực người dùng hiện tại."
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            val credential = EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            tvError.text = "Lỗi khi đổi mật khẩu!"
                        }
                        .addOnCompleteListener { progress.visibility = View.GONE }
                }
                .addOnFailureListener {
                    progress.visibility = View.GONE
                    tvError.text = "Mật khẩu hiện tại không đúng!"
                }
        }
        dialog.show()
    }

    private fun setupToggleEye(editText: EditText, state: kotlin.reflect.KMutableProperty0<Boolean>) {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width())) {
                    state.set(!state.get())
                    val inputType = if (state.get())
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    editText.inputType = inputType

                    val icon = if (state.get()) R.drawable.hide else R.drawable.hide_off
                    editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
                    editText.setSelection(editText.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED ->
                imagePickerLauncher.launch("image/*")
            else -> requestPermissionLauncher.launch(permission)
        }
    }
}
