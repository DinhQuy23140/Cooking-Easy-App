package com.example.cookingeasy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityRegisterBinding
import com.example.cookingeasy.ui.main.activity.EnterNameActivity
import com.example.cookingeasy.ui.main.activity.PickAvatarActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var credentialManager: CredentialManager

    private val viewModel: RegisterViewModel by viewModels()

    // ─── Lifecycle ───────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        credentialManager = CredentialManager.create(this)

        setupClickListeners()
        observeState()
    }

    // ─── Setup ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.registerBtnRegister.setOnClickListener {
            viewModel.register(
                email           = binding.registerEtEmail.text.toString().trim(),
                password        = binding.registerEtPassword.text.toString(),
                confirmPassword = binding.registerEtConfirmPassword.text.toString()
            )
        }

        binding.btnGoogle.setOnClickListener {
            startGoogleSignIn()
        }

        binding.registerTvLoginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterState.Idle    -> Unit
                    is RegisterState.Loading -> showLoading(true)
                    is RegisterState.Success -> {
                        showLoading(false)
                        navigateToEnterName()
                    }
                    is RegisterState.Error   -> {
                        showLoading(false)
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    // ─── Google Sign-In (Credential Manager) ─────────────────────────

    private fun startGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // false = hiện tất cả account kể cả account mới
            .setAutoSelectEnabled(false)           // false = luôn hiện dialog chọn account
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@RegisterActivity
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.registerWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    showError("Unsupported credential type")
                }

            } catch (e: GetCredentialException) {
                showError("Google sign-up failed: ${e.message}")
            }
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────

    private fun navigateToEnterName() {
        startActivity(Intent(this, EnterNameActivity::class.java))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // ─── UI Helpers ──────────────────────────────────────────────────

    private fun showLoading(isLoading: Boolean) {
        binding.registerBtnRegister.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}