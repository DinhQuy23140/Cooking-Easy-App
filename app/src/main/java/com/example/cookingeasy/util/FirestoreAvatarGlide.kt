package com.example.cookingeasy.util

import android.util.Base64
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

/**
 * Loads a user avatar from Firestore's `avatarUrl` field, which may be an http(s) URL,
 * a data URI, or a raw Base64 JPEG payload (same shape as pick-avatar / profile upload).
 */
fun ImageView.loadFirestoreAvatar(
    avatarValue: String?,
    @DrawableRes placeholder: Int,
    @DrawableRes error: Int
) {
    val raw = avatarValue?.trim().orEmpty()
    if (raw.isEmpty()) {
        Glide.with(this)
            .load(placeholder)
            .into(this)
        return
    }

    val model: Any = when {
        raw.startsWith("http", ignoreCase = true) ||
            raw.startsWith("content:", ignoreCase = true) ||
            raw.startsWith("file:", ignoreCase = true) ||
            raw.startsWith("data:", ignoreCase = true) -> raw
        else -> {
            val decoded: ByteArray? = runCatching { Base64.decode(raw, Base64.DEFAULT) }.getOrNull()
            if (decoded != null && decoded.isNotEmpty()) decoded else raw
        }
    }

    Glide.with(this)
        .load(model)
        .apply(
            RequestOptions()
                .transform(CircleCrop())
                .placeholder(placeholder)
                .error(error)
        )
        .into(this)
}
