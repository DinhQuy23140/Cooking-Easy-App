package com.example.cookingeasy.ui.main.fragment

import android.app.Dialog
import android.graphics.Bitmap
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LOGGER
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.call.InCallActivity
import com.example.cookingeasy.call.IncomingCallActivity
import com.example.cookingeasy.common.adapter.MessageAdapter
import com.example.cookingeasy.common.adapter.MessageUiModel
import com.example.cookingeasy.databinding.FragmentChatDetailBinding
import com.example.cookingeasy.ui.viewmodel.ChatDetailViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatDetailFragment : Fragment() {
    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!
    private var isAttachmentExpanded = false
    private var mediaRecorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var isRecordingVoice = false

    private val messageAdapter = MessageAdapter(
        onImageClick = { message -> openImageFullScreen(message) },
        onAttachmentClick = { message -> openAttachment(message) }
    )
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val mime = requireContext().contentResolver.getType(it).orEmpty()
                if (mime.startsWith("video/")) {
                    viewModel.sendVideo(it)
                } else {
                    viewModel.sendImage(it)
                }
            }
        }
    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            val uri = bitmap?.let { saveBitmapToCache(it) }
            if (uri != null) {
                viewModel.sendImage(uri)
            }
        }
    private val pickAttachmentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.sendAttachment(it) }
        }
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                captureImageLauncher.launch(null)
            } else {
                Toast.makeText(requireContext(), R.string.chat_camera_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                toggleVoiceRecording()
            } else {
                Toast.makeText(requireContext(), R.string.chat_audio_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    private var pendingCallType: String? = null
    private val requestCallPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
            val audioGranted = grantResults[Manifest.permission.RECORD_AUDIO] == true
            val type = pendingCallType ?: return@registerForActivityResult
            if (type == "video") {
                val cameraGranted = grantResults[Manifest.permission.CAMERA] == true
                if (audioGranted && cameraGranted) {
                    startCall(type)
                } else {
                    Toast.makeText(requireContext(), "Camera/Mic permissions required", Toast.LENGTH_SHORT).show()
                }
            } else if (audioGranted) {
                startCall(type)
            } else {
                Toast.makeText(requireContext(), "Microphone permission required", Toast.LENGTH_SHORT).show()
            }
        }

    private val viewModel: ChatDetailViewModel by viewModels {
        ChatDetailViewModel.Factory(
            arguments?.getString(ARG_USER_UID).orEmpty(),
            arguments?.getString(ARG_USER_NAME).orEmpty(),
            arguments?.getString(ARG_USER_AVATAR).orEmpty(),
            requireContext().contentResolver
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMessages()
        setupComposerInteractions()
        setUpListener()
        observeState()
        viewModel.start()
    }

    private fun setUpListener() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.txtName.setOnClickListener {
            val uid = arguments?.getString(ARG_USER_UID).orEmpty()
            if (uid.isEmpty()) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, OtherUserProfileFragment.newInstance(uid))
                .addToBackStack(null)
                .commit()
        }
        binding.btnCall.setOnClickListener {
            ensureCallPermissionsThenStart("audio")
        }
        binding.btnVideoCall.setOnClickListener {
            ensureCallPermissionsThenStart("video")
        }
    }


    override fun onDestroyView() {
        stopVoiceRecording(cancel = true)
        super.onDestroyView()
        _binding = null
    }

    private fun setupComposerInteractions() {
        binding.btnToggleAttachment.setOnClickListener {
            toggleAttachmentActions()
        }

        binding.edtMessage.doAfterTextChanged { editable ->
            val hasText = !editable.isNullOrBlank()
            if (hasText && isAttachmentExpanded) {
                setAttachmentExpanded(expanded = false, animate = true)
            }
        }

        binding.btnAttachFile.setOnClickListener {
            pickAttachmentLauncher.launch("*/*")
        }
        binding.btnAttachImage.setOnClickListener {
            pickMediaLauncher.launch("*/*")
        }
        binding.btnCapture.setOnClickListener {
            openCameraWithPermissionCheck()
        }
        binding.btnRecordVoice.setOnClickListener {
            ensureAudioPermissionThenRecord()
        }

        binding.btnSend.setOnClickListener {
            val content = binding.edtMessage.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) return@setOnClickListener
            binding.edtMessage.setText("")
            viewModel.sendMessage(content)
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri? {
        return runCatching {
            val file = File(requireContext().cacheDir, "chat_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun openCameraWithPermissionCheck() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            captureImageLauncher.launch(null)
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun ensureAudioPermissionThenRecord() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            toggleVoiceRecording()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun toggleVoiceRecording() {
        if (!isRecordingVoice) {
            startVoiceRecording()
        } else {
            stopVoiceRecording(cancel = false)
        }
    }

    private fun startVoiceRecording() {
        runCatching {
            val output = File(requireContext().cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(output.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            voiceFile = output
            isRecordingVoice = true
            binding.btnRecordVoice.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            Toast.makeText(requireContext(), R.string.chat_voice_recording_started, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(requireContext(), R.string.chat_voice_recording_failed, Toast.LENGTH_SHORT).show()
            stopVoiceRecording(cancel = true)
        }
    }

    private fun stopVoiceRecording(cancel: Boolean) {
        val file = voiceFile
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        voiceFile = null
        if (isAdded) {
            binding.btnRecordVoice.clearColorFilter()
        }
        val wasRecording = isRecordingVoice
        isRecordingVoice = false
        if (!cancel && wasRecording && file != null && file.exists()) {
            viewModel.sendVoice(Uri.fromFile(file))
            Toast.makeText(requireContext(), R.string.chat_voice_recording_sent, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMessages() {
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messageAdapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.error != null) {
                        Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
                        Log.e("Error chat", state.error)
                    }
                    binding.txtName.text = state.otherName.ifEmpty {
                        getString(R.string.chat_detail_name_placeholder)
                    }
                    binding.txtStatus.text = if (state.isOtherActive) {
                        getString(R.string.chat_status_active_now)
                    } else {
                        formatLastSeen(state.otherLastActiveAt)
                    }
                    binding.viewOnline.isVisible = state.isOtherActive
                    messageAdapter.submitList(state.messages)
                    if (state.messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(state.messages.lastIndex)
                    }
                }
            }
        }
    }

    private fun openImageFullScreen(message: MessageUiModel) {
        if (message.imageUrl.isBlank()) {
            Toast.makeText(requireContext(), R.string.chat_image_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val root = FrameLayout(requireContext()).apply {
            setBackgroundColor(Color.BLACK)
        }
        val imageView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { dialog.dismiss() }
        }
        Glide.with(this).load(message.imageUrl).into(imageView)
        root.addView(imageView)
        dialog.setContentView(root)
        dialog.show()
    }

    private fun openAttachment(message: MessageUiModel) {
        if (message.attachmentUrl.isBlank()) {
            Toast.makeText(requireContext(), R.string.chat_file_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse(message.attachmentUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
        }
        runCatching {
            startActivity(Intent.createChooser(intent, getString(R.string.chat_open_attachment)))
        }.onFailure {
            Toast.makeText(requireContext(), R.string.chat_file_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAttachmentActions() {
        setAttachmentExpanded(!isAttachmentExpanded, animate = true)
    }

    private fun setAttachmentExpanded(expanded: Boolean, animate: Boolean) {
        isAttachmentExpanded = expanded
//        binding.btnToggleAttachment.animate().rotation(if (expanded) 45f else 0f).setDuration(160L).start()

        if (!animate) {
            binding.layoutAttachmentActions.visibility = if (expanded) View.VISIBLE else View.GONE
            return
        }

        if (expanded) {
            binding.layoutAttachmentActions.alpha = 0f
            binding.layoutAttachmentActions.translationY = 14f
            binding.layoutAttachmentActions.visibility = View.VISIBLE
            binding.layoutAttachmentActions.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(170L)
                .start()
        } else {
            binding.layoutAttachmentActions.animate()
                .alpha(0f)
                .translationY(10f)
                .setDuration(140L)
                .withEndAction {
                    binding.layoutAttachmentActions.visibility = View.GONE
                    binding.layoutAttachmentActions.translationY = 0f
                }
                .start()
        }
    }

    private fun formatLastSeen(lastActiveAt: Long): String {
        if (lastActiveAt <= 0L) return getString(R.string.chat_status_inactive)
        val deltaMs = System.currentTimeMillis() - lastActiveAt
        val minutes = (deltaMs / 60000L).coerceAtLeast(1L)
        return when {
            minutes < 60L -> getString(R.string.chat_status_last_seen_min, minutes)
            minutes < 24L * 60L -> getString(R.string.chat_status_last_seen_hour, minutes / 60L)
            else -> getString(
                R.string.chat_status_last_seen_date,
                SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(lastActiveAt))
            )
        }
    }

    private fun ensureCallPermissionsThenStart(type: String) {
        pendingCallType = type
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (type == "video") required.add(Manifest.permission.CAMERA)
        val denied = required.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isEmpty()) {
            startCall(type)
            return
        }
        requestCallPermissionsLauncher.launch(denied.toTypedArray())
    }

    private fun startCall(type: String) {
        val peerUid = arguments?.getString(ARG_USER_UID).orEmpty()
        if (peerUid.isBlank()) return
        startActivity(Intent(requireContext(), InCallActivity::class.java).apply {
            putExtra(IncomingCallActivity.EXTRA_PEER_ID, peerUid)
            putExtra(IncomingCallActivity.EXTRA_CALL_TYPE, type)
            putExtra(InCallActivity.EXTRA_IS_CALLER, true)
        })
    }

    companion object {
        private const val ARG_USER_UID = "userUid"
        private const val ARG_USER_NAME = "userName"
        private const val ARG_USER_AVATAR = "userAvatar"
    }
}