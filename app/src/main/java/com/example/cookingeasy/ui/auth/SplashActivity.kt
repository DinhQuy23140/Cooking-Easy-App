package com.example.cookingeasy.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivitySplashBinding
import com.example.cookingeasy.ui.auth.SplashViewModel.SplashState
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeState()
        setupFcmStartupDebug()
        viewModel.checkLoginStatus()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is SplashState.Idle,
                    is SplashState.Loading         -> Unit
                    is SplashState.NavigateToLogin -> {
                        AuthNavigator.openLogin(this@SplashActivity, clearTask = true, finishCurrent = true)
                    }
                    is SplashState.NavigateToEnterName -> {
                        AuthNavigator.openEnterName(this@SplashActivity, clearTask = true, finishCurrent = true)
                    }
                    is SplashState.NavigateToMain  -> {
                        AuthNavigator.openMain(this@SplashActivity, clearTask = true, finishCurrent = true)
                    }
                }
            }
        }
    }

    private fun setupFcmStartupDebug() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> Log.e(TAG, "Splash token: $token") }
            .addOnFailureListener { e -> Log.e(TAG, "Splash get token failed", e) }
    }

    companion object {
        private const val TAG = "ChatFCM"
    }
}