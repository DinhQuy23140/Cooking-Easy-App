package com.example.cookingeasy.ui.main.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.ChatAdapter
import com.example.cookingeasy.databinding.FragmentAIChatBinding
import com.example.cookingeasy.ui.viewmodel.AIChatViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch

class AIChatFragment : Fragment() {

    private lateinit var binding: FragmentAIChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val viewModel: AIChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAIChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupEvents()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(mutableListOf()) { recipe ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.container, RecipeDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString("recipe", Gson().toJson(recipe))
                    }
                })
                .addToBackStack(null)
                .commit()
        }

        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            setHasFixedSize(false)
        }
    }

    private fun setupEvents() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnClearChat.setOnClickListener {
            viewModel.clearChat()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.edtMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        binding.btnScan.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.container, ScanFragment())
                .addToBackStack(null)
                .commit()
        }

        // Suggestion chips
        binding.chip1.setOnClickListener { fillAndSend("Dinner ideas") }
        binding.chip2.setOnClickListener { fillAndSend("Cook with chicken") }
        binding.chip3.setOnClickListener { fillAndSend("Healthy breakfast") }
        binding.chip4.setOnClickListener { fillAndSend("Quick 15 min meals") }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.clearMessages()
                        messages.forEach { chatAdapter.submitMessage(it) }
                        if (messages.isNotEmpty()) {
                            binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
                }

                launch {
                    viewModel.isTyping.collect { isTyping ->
                        // disable input khi bot đang trả lời
                        binding.btnSend.isEnabled = !isTyping
                        binding.edtMessage.isEnabled = !isTyping
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val text = binding.edtMessage.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return

        binding.edtMessage.setText("")
        hideKeyboard()
        viewModel.sendMessage(text)
    }

    private fun fillAndSend(text: String) {
        binding.edtMessage.setText(text)
        sendMessage()
    }

    private fun hideKeyboard() {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.edtMessage.windowToken, 0)
    }

    companion object {
        fun newInstance() = AIChatFragment()
    }
}