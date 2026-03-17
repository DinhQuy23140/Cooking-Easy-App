package com.example.cookingeasy.ui.main.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.data.remote.firebase.AuthDataSource
import com.example.cookingeasy.databinding.FragmentMyProfileBinding
import com.example.cookingeasy.ui.auth.LoginActivity
import com.example.cookingeasy.ui.main.viewmodel.MyProfileViewModel
import kotlinx.coroutines.launch

class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyProfileViewModel by viewModels()

    // ─── Lifecycle ───────────────────────────────────────────────────

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

    // ─── Setup ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
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

        binding.statMyRecipes.setOnClickListener {
            navigateToMyRecipes()
        }

        binding.statSaved.setOnClickListener {
            navigateToFavoriteRecipes()
        }

        binding.statUpload.setOnClickListener {
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
                            name  = state.user.displayName ?: "Chef",
                            email = state.user.email ?: ""
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

    // ─── UI ──────────────────────────────────────────────────────────

    private fun bindUserInfo(name: String, email: String) {
        binding.txtName.text = name
        binding.txtEmail.text = email
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnLogout.isEnabled = !isLoading
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancel", null)
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

    // ─── Navigation ──────────────────────────────────────────────────

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateToEditProfile() {
        // TODO: navigate to EditProfileActivity
    }

    private fun navigateToDraftRecipes() {
        // TODO: navigate to DraftRecipesFragment
    }

    private fun navigateToFavoriteRecipes() {
        // TODO: navigate to FavoriteRecipesFragment
    }

    private fun navigateToLanguageSettings() {
        // TODO: navigate to LanguageSettingsActivity
    }

    private fun navigateToMyRecipes() {
        // TODO: navigate to ManageMyRecipeFragment
    }

    private fun navigateToUpload() {
        // TODO: navigate to AddRecipeFragment
    }
}