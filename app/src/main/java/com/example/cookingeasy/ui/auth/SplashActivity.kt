package com.example.cookingeasy.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.data.remote.firebase.AuthDataSource
import com.example.cookingeasy.data.repository.UserRepository
import com.example.cookingeasy.databinding.ActivitySplashBinding
import com.example.cookingeasy.ui.auth.SplashViewModel.SplashState
import com.example.cookingeasy.ui.main.MainActivity
import com.example.cookingeasy.ui.main.activity.EnterNameActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val viewModel: SplashViewModel by viewModels()

    // ─── Lifecycle ───────────────────────────────────────────────────

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
        viewModel.checkLoginStatus()
    }

    // ─── Observe ─────────────────────────────────────────────────────

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is SplashState.Idle,
                    is SplashState.Loading         -> Unit
                    is SplashState.NavigateToLogin -> {
                        navigateTo(LoginActivity::class.java)
                    }
                    is SplashState.NavigateToEnterName -> {
                        navigateTo(EnterNameActivity::class.java)
                    }
                    is SplashState.NavigateToMain  -> {
                        navigateTo(MainActivity::class.java)
                    }
                }
            }
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}