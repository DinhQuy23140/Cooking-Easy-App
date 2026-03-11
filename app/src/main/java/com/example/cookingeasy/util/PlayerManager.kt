package com.example.cookingeasy.util

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {
    private var player: ExoPlayer? = null
    fun getPlayer(context: Context): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build()
        }
        return player!!
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }
}