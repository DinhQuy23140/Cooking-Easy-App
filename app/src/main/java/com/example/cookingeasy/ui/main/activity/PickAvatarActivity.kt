package com.example.cookingeasy.ui.main.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityPickAvatarBinding
import com.example.cookingeasy.ui.auth.AuthNavigator
import com.example.cookingeasy.ui.viewmodel.PickAvatarViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import kotlin.getValue

@AndroidEntryPoint
class PickAvatarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickAvatarBinding
    private val viewmodel: PickAvatarViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { setAvatarImage(it) }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureAuthenticated()) return
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

    private fun ensureAuthenticated(): Boolean {
        if (FirebaseAuth.getInstance().currentUser != null) return true
        AuthNavigator.openLogin(this, clearTask = true, finishCurrent = true)
        return false
    }

    // ─── Setup ───────────────────────────────────────────────────────

    private fun initListeners() {
        binding.btnUploadBadge.setOnClickListener {
            openGallery()
        }

        binding.btnUploadFromGallery.setOnClickListener {
            openGallery()
        }

        binding.btnConfirm.setOnClickListener {
            selectedImageUri ?.let {
                val strImg = uriToBase64(this, selectedImageUri!!)
                if (strImg != null) {
                    viewmodel.base64Img.value = strImg
                }
                navigateToMain()
                viewmodel.uploadImg()
            }
        }

        binding.btnSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun setAvatarImage(uri: Uri) {
        // Lưu lại uri để dùng khi upload
        selectedImageUri = uri

        Glide.with(this)
            .load(uri)
            .transform(CircleCrop())
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(binding.imgAvatarPreview)
    }

    private fun navigateToMain() {
        AuthNavigator.openMain(this, clearTask = true, finishCurrent = true)
    }

    fun uriToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return null

            val bytes = inputStream.readBytes()

            val compressedBytes = compressImage(bytes)

            Base64.encodeToString(compressedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressImage(imageBytes: ByteArray, quality: Int = 70): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val outputStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        return outputStream.toByteArray()
    }
}