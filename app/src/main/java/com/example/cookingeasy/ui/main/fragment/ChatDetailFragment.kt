package com.example.cookingeasy.ui.main.fragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MessageAdapter
import com.example.cookingeasy.common.adapter.MessageContentType
import com.example.cookingeasy.common.adapter.MessageSendStatus
import com.example.cookingeasy.common.adapter.MessageUiModel
import com.example.cookingeasy.databinding.FragmentChatDetailBinding

class ChatDetailFragment : Fragment() {
    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!
    private var isAttachmentExpanded = false
    private val messageAdapter = MessageAdapter(
        onImageClick = { message -> openImageFullScreen(message) },
        onAttachmentClick = { message -> openAttachment(message) }
    )
    private var nextMessageId = 1000L

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
    }

    private fun setUpListener() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }


    override fun onDestroyView() {
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
            Toast.makeText(requireContext(), R.string.cd_chat_attach_file, Toast.LENGTH_SHORT).show()
        }
        binding.btnAttachImage.setOnClickListener {
            Toast.makeText(requireContext(), R.string.cd_chat_attach_image, Toast.LENGTH_SHORT).show()
        }
        binding.btnCapture.setOnClickListener {
            Toast.makeText(requireContext(), R.string.cd_chat_take_photo, Toast.LENGTH_SHORT).show()
        }

        binding.btnSend.setOnClickListener {
            val content = binding.edtMessage.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) return@setOnClickListener

            messageAdapter.appendMessage(
                MessageUiModel(
                    id = "local-${nextMessageId++}",
                    isMine = true,
                    contentType = MessageContentType.TEXT,
                    text = content,
                    timeLabel = "Now",
                    sendStatus = MessageSendStatus.SENDING
                )
            )
            binding.edtMessage.setText("")
            binding.rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun setupMessages() {
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messageAdapter

        messageAdapter.submitList(
            listOf(
                MessageUiModel(
                    id = "1",
                    isMine = false,
                    contentType = MessageContentType.TEXT,
                    text = "Hi! I shared the recipe file.",
                    timeLabel = "2:24 PM"
                ),
                MessageUiModel(
                    id = "2",
                    isMine = true,
                    contentType = MessageContentType.ATTACHMENT,
                    attachmentName = "beef-stew.pdf",
                    attachmentUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    attachmentSize = "2.3 MB",
                    timeLabel = "2:25 PM",
                    sendStatus = MessageSendStatus.SEEN
                ),
                MessageUiModel(
                    id = "3",
                    isMine = false,
                    contentType = MessageContentType.IMAGE,
                    imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1080",
                    timeLabel = "2:26 PM"
                ),
                MessageUiModel(
                    id = "4",
                    isMine = true,
                    contentType = MessageContentType.TEXT,
                    text = "Looks great. Thanks!",
                    timeLabel = "2:27 PM",
                    sendStatus = MessageSendStatus.SENT
                )
            )
        )
        binding.rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
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
}