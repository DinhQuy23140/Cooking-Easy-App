package com.example.cookingeasy.ui.main.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityEnterNameBinding
import com.example.cookingeasy.ui.auth.EnternameViewmodel
import com.example.cookingeasy.ui.auth.EnternameViewmodel.EnterNameState
import kotlinx.coroutines.launch

class EnterNameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterNameBinding

    private val viewModel: EnternameViewmodel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEnterNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        observeState()
    }

    // ─── Setup ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener {
            viewModel.saveName(
                fullName = binding.edtFullName.text.toString().trim(),
                nickname = binding.edtNickname.text.toString().trim()
            )
        }

        binding.btnSkip.setOnClickListener {
            viewModel.skip()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is EnterNameState.Idle    -> Unit
                    is EnterNameState.Loading -> showLoading(true)
                    is EnterNameState.Success -> {
                        showLoading(false)
                        navigateToPickAvatar()
                    }
                    is EnterNameState.Error   -> {
                        showLoading(false)
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────

    private fun navigateToPickAvatar() {
        startActivity(Intent(this, PickAvatarActivity::class.java))
        finish()
    }

    // ─── UI Helpers ──────────────────────────────────────────────────

    private fun showLoading(isLoading: Boolean) {
        binding.btnContinue.isEnabled = !isLoading
        binding.btnSkip.isEnabled     = !isLoading
        binding.edtFullName.isEnabled = !isLoading
        binding.edtNickname.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}