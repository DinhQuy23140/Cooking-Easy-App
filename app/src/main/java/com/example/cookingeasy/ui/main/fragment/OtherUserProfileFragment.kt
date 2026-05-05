package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeRepositoryImp
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.data.repository.UserRepositoryImp
import com.example.cookingeasy.databinding.FragmentOtherUserProfileBinding
import com.example.cookingeasy.domain.mapper.toRecipe
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.domain.repository.AuthRepository
import com.example.cookingeasy.domain.repository.RecipeRepository
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

    private val authRepository: AuthRepository = AuthRepositoryImp()
    private val recipeRepository: RecipeRepository = RecipeRepositoryImp()
    private val userRepository: UserRepository = UserRepositoryImp()

    private lateinit var recipeAdapter: RecipeAdapter
    private var allRecipes: List<RecipeUpload> = emptyList()
    private var tabsHooked = false

    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            applyRecipeList(tab?.position ?: 0)
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
                val uidAuth = authRepository.getCurrentUser()?.uid ?: return
                viewLifecycleOwner.lifecycleScope.launch {
                    recipeRepository.toggleFavorite(uidAuth, recipe)
                }
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
                            bindProfile(state)
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

    private fun bindProfile(state: OtherUserProfileViewModel.UiState.Success) {
        val profile = state.profile
        val publishedRecipeCount = state.publishedRecipeCount
        val profileUid = arguments?.getString(ARG_UID).orEmpty()
        val isOwnProfile =
            profileUid.isNotEmpty() && profileUid == userRepository.getUid()

        binding.layoutActions.isVisible = !isOwnProfile
        binding.tabProfile.isVisible = isOwnProfile

        if (isOwnProfile) {
            binding.tabProfile.getTabAt(0)?.text = getString(R.string.manage_tab_published)
            binding.tabProfile.getTabAt(1)?.text = getString(R.string.manage_tab_draft)
            if (!tabsHooked) {
                binding.tabProfile.addOnTabSelectedListener(tabListener)
                tabsHooked = true
            }
        }

        val fullName = (profile["fullName"] as? String).orEmpty()
            .ifEmpty { (profile["nickname"] as? String).orEmpty() }
        val nickname = (profile["nickname"] as? String).orEmpty()
        val email = (profile["email"] as? String).orEmpty()
        val avatarUrl = (profile["avatarUrl"] as? String).orEmpty()
        val bio = (profile["bio"] as? String).orEmpty()
        val verified = profile["verified"] as? Boolean == true

        binding.tvName.text = fullName.ifEmpty { getString(R.string.profile_name_placeholder) }
        binding.tvUsername.text = when {
            nickname.isNotEmpty() -> "@$nickname"
            email.isNotEmpty() -> email
            else -> ""
        }
        if (bio.isNotEmpty()) {
            binding.tvBio.text = bio
            binding.tvBio.isVisible = true
        } else {
            binding.tvBio.isVisible = false
        }

        binding.imgVerifiedBadge.isVisible = verified

        if (avatarUrl.isNotEmpty()) {
            Glide.with(binding.imgAvatar)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.imgAvatar)
        } else {
            Glide.with(binding.imgAvatar)
                .load(R.drawable.ic_person)
                .into(binding.imgAvatar)
        }

        binding.tvStatRecipes.text = publishedRecipeCount.toString()
        binding.tvStatFollowers.text = "0"
        binding.tvStatFollowing.text = "0"

        allRecipes = state.recipes
        val tabPos = if (isOwnProfile) binding.tabProfile.selectedTabPosition else 0
        applyRecipeList(tabPos)
    }

    private fun applyRecipeList(tabPosition: Int) {
        val profileUid = arguments?.getString(ARG_UID).orEmpty()
        val isOwnProfile =
            profileUid.isNotEmpty() && profileUid == userRepository.getUid()

        val filtered = when {
            !isOwnProfile -> allRecipes.filter { it.status == "published" }
            tabPosition == 0 -> allRecipes.filter { it.status == "published" }
            else -> allRecipes.filter { it.status == "draft" }
        }

        val mapped = filtered.map { it.toRecipe() }
        recipeAdapter.submitList(mapped)
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.rvRecipes.isVisible = filtered.isNotEmpty()
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
