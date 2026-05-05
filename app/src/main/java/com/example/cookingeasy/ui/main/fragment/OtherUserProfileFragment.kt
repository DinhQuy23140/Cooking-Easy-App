package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentOtherUserProfileBinding
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.OtherUserProfileViewModel
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class OtherUserProfileFragment : Fragment() {

    private var _binding: FragmentOtherUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OtherUserProfileViewModel by viewModels {
        OtherUserProfileViewModel.Factory(
            arguments?.getString(ARG_UID).orEmpty(),
            requireContext().contentResolver
        )
    }

    private val recipeShare: RecipeShareViewmodel by activityViewModels()
    private lateinit var recipeAdapter: RecipeAdapter
    private var tabsHooked = false
    private var chatTargetName: String = ""
    private var chatTargetAvatar: String = ""

    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            viewModel.onTabSelected(tab?.position ?: 0)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtherUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListener()
        observeUiState()
        viewModel.loadProfile()
    }

    override fun onDestroyView() {
        if (tabsHooked) {
            binding.tabProfile.removeOnTabSelectedListener(tabListener)
            tabsHooked = false
        }
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(mutableListOf(), object : RecipeListener {
            override fun OnClickItem(recipe: Recipe) {
                recipeShare.selectedRecipe(recipe)
                openRecipeDetail()
            }

            override fun OnFavoriteClick(recipe: Recipe) {
                viewModel.toggleFavorite(recipe)
            }

            override fun onClickInf(recipe: Recipe) {
                if (recipe.userUid.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.other_user_profile_unavailable,
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (!isAdded) return
                val fm = parentFragmentManager
                if (fm.isStateSaved) return
                fm.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(R.id.container, newInstance(recipe.userUid))
                    .addToBackStack(null)
                    .commit()
            }
        })
        binding.rvRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvRecipes.addItemDecoration(GridSpacingItemDecoration(2, 5))
        binding.rvRecipes.adapter = recipeAdapter
    }

    private fun setupListener() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnMessage.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("userUid", arguments?.getString(ARG_UID).orEmpty())
            bundle.putString("userName", chatTargetName)
            bundle.putString("userAvatar", chatTargetAvatar)
            val fragment = ChatDetailFragment()
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is OtherUserProfileViewModel.UiState.Idle -> Unit
                        is OtherUserProfileViewModel.UiState.Loading -> {
                            binding.progressLoad.isVisible = true
                        }
                        is OtherUserProfileViewModel.UiState.Success -> {
                            binding.progressLoad.isVisible = false
                            renderSuccess(state)
                        }
                        is OtherUserProfileViewModel.UiState.Error -> {
                            binding.progressLoad.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderSuccess(state: OtherUserProfileViewModel.UiState.Success) {
        binding.layoutActions.isVisible = !state.isOwnProfile
        binding.tabProfile.isVisible = state.isOwnProfile

        if (state.isOwnProfile) {
            binding.tabProfile.getTabAt(0)?.text = getString(R.string.manage_tab_published)
            binding.tabProfile.getTabAt(1)?.text = getString(R.string.manage_tab_draft)
            if (!tabsHooked) {
                binding.tabProfile.addOnTabSelectedListener(tabListener)
                tabsHooked = true
            }
            val current = binding.tabProfile.selectedTabPosition
            if (current != state.selectedTab) {
                binding.tabProfile.getTabAt(state.selectedTab)?.select()
            }
        } else if (tabsHooked) {
            binding.tabProfile.removeOnTabSelectedListener(tabListener)
            tabsHooked = false
        }

        binding.tvName.text = state.profile.fullName.ifEmpty {
            getString(R.string.profile_name_placeholder)
        }
        chatTargetName = state.profile.fullName.ifEmpty {
            getString(R.string.profile_name_placeholder)
        }
        chatTargetAvatar = state.profile.avatarUrl
        binding.tvUsername.text = state.profile.usernameOrEmail
        binding.tvBio.text = state.profile.bio
        binding.tvBio.isVisible = state.profile.bio.isNotEmpty()
        binding.imgVerifiedBadge.isVisible = state.profile.verified

        if (state.profile.avatarUrl.isNotEmpty()) {
            Glide.with(binding.imgAvatar)
                .load(state.profile.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.imgAvatar)
        } else {
            Glide.with(binding.imgAvatar).load(R.drawable.ic_person).into(binding.imgAvatar)
        }

        binding.tvStatRecipes.text = state.publishedRecipeCount.toString()
        binding.tvStatFollowers.text = "0"
        binding.tvStatFollowing.text = "0"
        recipeAdapter.submitList(state.recipes)
        binding.layoutEmpty.isVisible = state.recipes.isEmpty()
        binding.rvRecipes.isVisible = state.recipes.isNotEmpty()
    }

    private fun openRecipeDetail() {
        if (!isAdded) return
        val fm = parentFragmentManager
        if (fm.isStateSaved) return
        fm.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.container, RecipeDetailFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val ARG_UID = "uid"

        fun newInstance(uid: String) = OtherUserProfileFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_UID, uid)
            }
        }
    }
}
