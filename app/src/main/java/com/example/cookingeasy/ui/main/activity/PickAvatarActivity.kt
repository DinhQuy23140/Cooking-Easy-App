package com.example.cookingeasy.ui.main.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityPickAvatarBinding
import com.example.cookingeasy.ui.main.MainActivity

class PickAvatarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickAvatarBinding

    // Uri ảnh đã chọn — null nếu chưa chọn
    private var selectedImageUri: Uri? = null

    // ─── Gallery picker launcher ─────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { setAvatarImage(it) }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPickAvatarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initListeners()
    }

    // ─── Setup ───────────────────────────────────────────────────────

    private fun initListeners() {
        // Camera badge trên avatar preview — mở gallery
        binding.btnUploadBadge.setOnClickListener {
            openGallery()
        }

        // Upload from gallery button trong card
        binding.btnUploadFromGallery.setOnClickListener {
            openGallery()
        }

        binding.btnConfirm.setOnClickListener {
            // TODO: upload ảnh lên Storage rồi lưu URL vào Firestore
            navigateToMain()
        }

        binding.btnSkip.setOnClickListener {
            navigateToMain()
        }
    }

    // ─── Gallery ─────────────────────────────────────────────────────

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun setAvatarImage(uri: Uri) {
        // Lưu lại uri để dùng khi upload
        selectedImageUri = uri

        // Load ảnh vào preview — Glide tự thay thế ảnh cũ nếu đã có
        Glide.with(this)
            .load(uri)
            .transform(CircleCrop())
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(binding.imgAvatarPreview)
    }

    // ─── Navigation ──────────────────────────────────────────────────

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}