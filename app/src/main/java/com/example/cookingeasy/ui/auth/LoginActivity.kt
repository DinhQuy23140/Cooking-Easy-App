package com.example.cookingeasy.ui.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager

    private val viewModel: LoginViewModel by viewModels()


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
                            navigateToEnterName()
                        } else {
                            navigateToMain()
                        }
                    }
                    is LoginState.ResetSuccess -> {
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

    private fun observeResetPasswordState() {
        lifecycleScope.launch {
            viewModel.resetPasswordState.collect { state ->
                when (state) {
                    is LoginState.Idle         -> Unit
                    is LoginState.Loading      -> showLoading(true)
                    is LoginState.ResetSuccess -> {
                        showLoading(false)
                        showMessage("Reset link sent! Please check your email.")
                        viewModel.resetState()
                    }
                    is LoginState.Error        -> {
                        showLoading(false)
                        showError(state.message)
                        viewModel.resetState()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        lifecycleScope.launch {
            val success = tryGoogleSignIn(filterByAuthorizedAccounts = true)
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
        } catch (_: NoCredentialException) {
            if (filterByAuthorizedAccounts) {
                Log.i("GoogleSignIn", "No authorized account, trying all accounts")
            } else {
                showError(getString(R.string.google_no_credentials_available))
                Log.i("GoogleSignIn", "No Google credential available on device")
            }
            false
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignIn", "filterByAuthorized=$filterByAuthorizedAccounts | ${e.message}")
            showError(getString(R.string.google_signin_failed_try_again))
            false
        }
    }

    @SuppressLint("InflateParams")
    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val btnSend  = dialogView.findViewById<View>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)

        val dialog = Dialog(this).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        btnSend.setOnClickListener {
            viewModel.resetPassword(edtEmail.text.toString().trim())
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun navigateToMain() {
        AuthNavigator.openMain(this, clearTask = true, finishCurrent = true)
    }

    private fun navigateToRegister() {
        AuthNavigator.openRegister(this)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled  = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
        binding.lnNav.isVisible = !isLoading
        binding.prbLoading.isVisible = isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToEnterName() {
        binding.lnNav.isVisible = true
        binding.prbLoading.isVisible = false
        AuthNavigator.openEnterName(this, clearTask = true, finishCurrent = true)
    }
}