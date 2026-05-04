package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.ChatConversation
import com.example.cookingeasy.common.adapter.ChatConversationAdapter
import com.example.cookingeasy.databinding.FragmentListChatBinding
import com.google.android.material.tabs.TabLayout

class ListChatFragment : Fragment() {

    private var _binding: FragmentListChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatConversationAdapter
    private val allConversations = mutableListOf<ChatConversation>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seedDemoConversations()
        adapter = ChatConversationAdapter { conv ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    com.example.cookingeasy.R.anim.slide_in_left, R.anim.slide_out_right
                )
                .replace(com.example.cookingeasy.R.id.container, ChatDetailFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter
        applyFilter()

        binding.edtSearchChats.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        binding.tabChatsFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = applyFilter()
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        binding.swipeRefreshChats.setColorSchemeResources(R.color.success, R.color.primary)
        binding.swipeRefreshChats.setOnRefreshListener {
            binding.root.postDelayed({
                binding.swipeRefreshChats.isRefreshing = false
                applyFilter()
            }, 600)
        }

        binding.fabCompose.setOnClickListener {
            Toast.makeText(requireContext(), R.string.chat_toast_compose_soon, Toast.LENGTH_SHORT).show()
        }
        binding.btnNewChat.setOnClickListener { binding.fabCompose.performClick() }
        binding.btnCamera.setOnClickListener {
            Toast.makeText(requireContext(), R.string.chat_toast_camera_soon, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun seedDemoConversations() {
        if (allConversations.isNotEmpty()) return
        allConversations.addAll(
            listOf(
                ChatConversation(
                    id = "1",
                    displayName = "Chef Minh",
                    snippet = "You: Great recipe — thanks for the tips!",
                    timeLabel = "2:34 PM",
                    unreadCount = 2,
                    isOnline = true,
                    isGroup = false
                ),
                ChatConversation(
                    id = "2",
                    displayName = "Cooking Club",
                    snippet = "Anna: Who's bringing dessert?",
                    timeLabel = "Yesterday",
                    unreadCount = 0,
                    isOnline = false,
                    isGroup = true
                ),
                ChatConversation(
                    id = "3",
                    displayName = "Dinh Quy",
                    snippet = "Sent a photo.",
                    timeLabel = "Mon",
                    unreadCount = 5,
                    isOnline = true,
                    isGroup = false
                )
            )
        )
    }

    private fun applyFilter() {
        val q = binding.edtSearchChats.text?.toString()?.trim()?.lowercase().orEmpty()
        val tab = binding.tabChatsFilter.selectedTabPosition

        val filtered = allConversations.filter { conv ->
            val matchesSearch = q.isEmpty() ||
                conv.displayName.lowercase().contains(q) ||
                conv.snippet.lowercase().contains(q)
            val matchesTab = when (tab) {
                1 -> conv.unreadCount > 0
                2 -> conv.isGroup
                else -> true
            }
            matchesSearch && matchesTab
        }

        adapter.submitList(filtered)
        val empty = filtered.isEmpty()
        binding.layoutEmptyChats.isVisible = empty
        binding.rvChats.isVisible = !empty
    }
}
