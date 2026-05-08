package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.FragmentManageFolloweBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_INITIAL_TAB = "initialTab"

/**
 * A simple [Fragment] subclass.
 * Use the [ManageFolloweFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ManageFolloweFragment : Fragment() {
    private var _binding: FragmentManageFolloweBinding? = null
    private val binding get() = _binding!!
    private var showingFollowers = true
    private var initialTab: String = TAB_FOLLOWERS

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentManageFolloweBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialTab = arguments?.getString(ARG_INITIAL_TAB).orEmpty().ifEmpty { TAB_FOLLOWERS }
        showingFollowers = initialTab != TAB_FOLLOWING
        setupActions()
        renderTabState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnTabFollowers.setOnClickListener {
            showingFollowers = true
            renderTabState()
        }
        binding.btnTabFollowing.setOnClickListener {
            showingFollowers = false
            renderTabState()
        }
    }

    private fun renderTabState() {
        binding.wrapFollowers.isVisible = showingFollowers
        binding.wrapFollowing.isVisible = !showingFollowers

        val selectedBg = resources.getColor(R.color.md_theme_primary, null)
        val unselectedText = resources.getColor(R.color.md_theme_primary, null)
        val selectedText = resources.getColor(android.R.color.white, null)

        if (showingFollowers) {
            binding.btnTabFollowers.setBackgroundColor(selectedBg)
            binding.btnTabFollowers.setTextColor(selectedText)
            binding.btnTabFollowing.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            binding.btnTabFollowing.setTextColor(unselectedText)
        } else {
            binding.btnTabFollowing.setBackgroundColor(selectedBg)
            binding.btnTabFollowing.setTextColor(selectedText)
            binding.btnTabFollowers.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            binding.btnTabFollowers.setTextColor(unselectedText)
        }
    }

    companion object {
        const val TAB_FOLLOWERS = "followers"
        const val TAB_FOLLOWING = "following"

        @JvmStatic
        fun newInstance(initialTab: String = TAB_FOLLOWERS) =
            ManageFolloweFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_TAB, initialTab)
                }
            }
    }
}