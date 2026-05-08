package com.example.cookingeasy.ui.main.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.cookingeasy.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.cookingeasy.data.preferences.ThemeModePreference
import com.example.cookingeasy.data.remote.firebase.fireAuth.AuthDataSource
import com.example.cookingeasy.databinding.FragmentMyProfileBinding
import com.example.cookingeasy.ui.auth.LoginActivity
import com.example.cookingeasy.ui.main.viewmodel.MyProfileViewModel
import com.example.cookingeasy.ui.viewmodel.MyRecipesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyProfileViewModel by viewModels()
    private val myRecipesViewModel: MyRecipesViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { setAvatarImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeState()
        observeRecipeStats()
    }

    override fun onResume() {
        super.onResume()
        myRecipesViewModel.loadMyRecipes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupClickListeners() {
        binding.switchDarkMode.isChecked = ThemeModePreference.isDarkMode(requireContext())
        binding.imgAvatar.setOnClickListener {
            openGallery()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.rowProfile.setOnClickListener {
            navigateToProfile()
        }

        binding.rowFavorite.setOnClickListener {
            navigateToFavoriteRecipes()
        }

        binding.rowLanguage.setOnClickListener {
            navigateToLanguageSettings()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }

        binding.quickMyRecipes.setOnClickListener {
            navigateToMyRecipes()
        }

        binding.statSaved.setOnClickListener {
            navigateToFavoriteRecipes()
        }

        binding.statMyRecipes.setOnClickListener {
            navigateToMyRecipes()
        }

        binding.statUpload.setOnClickListener {
            navigateToUpload()
        }

        binding.quickSaved.setOnClickListener {
            navigateToFavoriteRecipes()
        }

        binding.quickUpload.setOnClickListener {
            navigateToUpload()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is MyProfileViewModel.ProfileState.Idle       -> Unit
                    is MyProfileViewModel.ProfileState.Loading    -> showLoading(true)
                    is MyProfileViewModel.ProfileState.UserLoaded -> {
                        showLoading(false)
                        bindUserInfo(
                            state.user
                        )
                    }
                    is MyProfileViewModel.ProfileState.LoggedOut  -> {
                        showLoading(false)
                        navigateToLogin()
                    }
                    is MyProfileViewModel.ProfileState.Error      -> {
                        showLoading(false)
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun observeRecipeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                myRecipesViewModel.stats.collect { s ->
                    binding.tvMyRecipesCount.text = s.total.toString()
                    binding.tvSavedCount.text = s.savedFavorites.toString()
                    binding.tvUploadCount.text = s.published.toString()
                }
            }
        }
    }

    private fun bindUserInfo(user: Map<String, Any>) {
        val fullName = (user["fullName"] as? String).orEmpty()
            .ifEmpty { (user["nickname"] as? String).orEmpty() }
        val email = (user["email"] as? String).orEmpty()
        val avatarUrl = (user["avatarUrl"] as? String).orEmpty()

        binding.txtName.text = fullName.ifEmpty { getString(R.string.profile_name_placeholder) }
        binding.txtEmail.text = email.ifEmpty { getString(R.string.profile_email_placeholder) }

        if (avatarUrl.isNotEmpty()) {
            Glide.with(binding.imgAvatar)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.imgAvatar)
        } else {
            Glide.with(binding.imgAvatar)
                .load(R.drawable.ic_person)
                .into(binding.imgAvatar)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnLogout.isEnabled = !isLoading
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.logout_confirm_title))
            .setMessage(getString(R.string.logout_confirm_message))
            .setPositiveButton(getString(R.string.action_logout)) { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun toggleDarkMode(isEnabled: Boolean) {
        ThemeModePreference.setDarkMode(requireContext(), isEnabled)
        ThemeModePreference.apply(requireContext())
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun setAvatarImage(uri: Uri) {
        selectedImageUri = uri

        Glide.with(this)
            .load(uri)
            .transform(CircleCrop())
            .placeholder(com.example.cookingeasy.R.drawable.ic_person)
            .error(com.example.cookingeasy.R.drawable.ic_person)
            .into(binding.imgAvatar)
    }

    private fun navigateToEditProfile() {
        findNavController().navigate(R.id.updateProfileFragment)
    }

    private fun navigateToProfile() {
        val uid = viewModel.getUid()
        if (uid.isEmpty()) return
        val bundle = Bundle().apply { putString("uid", uid) }
        findNavController().navigate(R.id.otherUserProfileFragment, bundle)
    }

    private fun navigateToFavoriteRecipes() {
        findNavController().navigate(R.id.favoriteFragment2)
    }

    private fun navigateToLanguageSettings() {
        findNavController().navigate(R.id.languageFragment)
    }

    private fun navigateToMyRecipes() {
        findNavController().navigate(R.id.manageMyRecipeFragment)
    }

    private fun navigateToUpload() {
        findNavController().navigate(R.id.addRecipeFragment)
    }
}