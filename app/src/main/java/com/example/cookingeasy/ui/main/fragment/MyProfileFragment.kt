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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.cookingeasy.data.remote.firebase.fireAuth.AuthDataSource
import com.example.cookingeasy.databinding.FragmentMyProfileBinding
import com.example.cookingeasy.ui.auth.LoginActivity
import com.example.cookingeasy.ui.main.viewmodel.MyProfileViewModel
import kotlinx.coroutines.launch

class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyProfileViewModel by viewModels()

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupClickListeners() {
        binding.imgAvatar.setOnClickListener {
            openGallery()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.rowDraft.setOnClickListener {
            navigateToDraftRecipes()
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


    private fun bindUserInfo(user: Map<String, Any>) {
        binding.txtName.text = user.get("fullName") as String
        binding.txtEmail.text = user.get("email") as String
        var imgUrl = user.get("avatarUrl").toString()
        if (!imgUrl.isEmpty()) {
            Glide.with(binding.imgAvatar)
                .load(user.get("avatarUrl").toString())
                .into(binding.imgAvatar)
        } else {
            Glide.with(binding.imgAvatar)
                .load(com.example.cookingeasy.R.drawable.ic_person)
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
        val mode = if (isEnabled) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
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

    }

    private fun navigateToDraftRecipes() {

    }

    private fun navigateToFavoriteRecipes() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                com.example.cookingeasy.R.anim.slide_in_right, com.example.cookingeasy.R.anim.slide_out_left,
                com.example.cookingeasy.R.anim.slide_in_left, com.example.cookingeasy.R.anim.slide_out_right
            )
            .replace(com.example.cookingeasy.R.id.container, FavoriteFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToLanguageSettings() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                com.example.cookingeasy.R.anim.slide_in_right, com.example.cookingeasy.R.anim.slide_out_left,
                com.example.cookingeasy.R.anim.slide_in_left, com.example.cookingeasy.R.anim.slide_out_right
            )
            .replace(com.example.cookingeasy.R.id.container, LanguageFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToMyRecipes() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                com.example.cookingeasy.R.anim.slide_in_right, com.example.cookingeasy.R.anim.slide_out_left,
                com.example.cookingeasy.R.anim.slide_in_left, com.example.cookingeasy.R.anim.slide_out_right
            )
            .replace(com.example.cookingeasy.R.id.container, ManageMyRecipeFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToUpload() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                com.example.cookingeasy.R.anim.slide_in_right, com.example.cookingeasy.R.anim.slide_out_left,
                com.example.cookingeasy.R.anim.slide_in_left, com.example.cookingeasy.R.anim.slide_out_right
            )
            .replace(com.example.cookingeasy.R.id.container, AddRecipeFragment())
            .addToBackStack(null)
            .commit()
    }
}