package com.example.cookingeasy.ui.main.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.ActivityFullscreenVideoBinding
import com.example.cookingeasy.util.PlayerManager

class FullscreenVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenVideoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFullscreenVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val player = PlayerManager.getPlayer(this)
        binding.playerView.player = player
        player.playWhenReady = true

        binding.btnExitFullscreen.setOnClickListener {
            finish()
        }
    }
}