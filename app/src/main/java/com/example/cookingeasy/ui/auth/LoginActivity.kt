package com.example.cookingeasy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityLoginBinding
import com.example.cookingeasy.ui.main.MainActivity
import com.example.cookingeasy.ui.main.activity.EnterNameActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager

    private val viewModel: LoginViewModel by viewModels()

    // ─── Lifecycle ───────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        credentialManager = CredentialManager.create(this)

        setupClickListeners()
        observeLoginState()
        observeResetPasswordState()
    }

    // ─── Setup ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                email    = binding.edtEmail.text.toString().trim(),
                password = binding.edtPassword.text.toString()
            )
        }

        binding.btnGoogle.setOnClickListener {
            startGoogleSignIn()
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.tvSignUp.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Idle    -> Unit
                    is LoginState.Loading -> showLoading(true)
                    is LoginState.Success -> {
                        showLoading(false)
                        if (state.isNewUser) {
                            navigateToEnterName()   // → EnterName → PickAvatar → Main
                        } else {
                            navigateToMain()        // → Main trực tiếp
                        }
                    }
                    is LoginState.Error   -> {
                        showLoading(false)
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun observeResetPasswordState() {
        lifecycleScope.launch {
            viewModel.resetPasswordState.collect { state ->
                when (state) {
                    is LoginState.Idle    -> Unit
                    is LoginState.Loading -> showLoading(true)
                    is LoginState.Success -> {
                        showLoading(false)
                        showMessage("Reset link sent! Please check your email.")
                        viewModel.resetState()
                    }
                    is LoginState.Error   -> {
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
        lifecycleScope.launch {
            // Thử authorized accounts trước (accounts đã dùng app)
            val success = tryGoogleSignIn(filterByAuthorizedAccounts = true)
            // Nếu không có → fallback hiện tất cả accounts
            if (!success) {
                tryGoogleSignIn(filterByAuthorizedAccounts = false)
            }
        }
    }

    private suspend fun tryGoogleSignIn(filterByAuthorizedAccounts: Boolean): Boolean {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = this@LoginActivity
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data)
                viewModel.loginWithGoogle(token.idToken)
                true
            } else {
                showError("Unsupported credential type")
                false
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignIn", "filterByAuthorized=$filterByAuthorizedAccounts | ${e.message}")
            false
        }
    }

    // ─── Forgot Password Dialog ───────────────────────────────────────

    private fun showForgotPasswordDialog() {
        val emailInput = android.widget.EditText(this).apply {
            hint = "Enter your email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a reset link.")
            .setView(emailInput)
            .setPositiveButton("Send") { _, _ ->
                viewModel.resetPassword(emailInput.text.toString().trim())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Navigation ──────────────────────────────────────────────────

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    // ─── UI Helpers ──────────────────────────────────────────────────

    private fun showLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled  = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToEnterName() {
        startActivity(Intent(this, EnterNameActivity::class.java))
        finish()
    }
}