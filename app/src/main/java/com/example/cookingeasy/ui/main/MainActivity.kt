package com.example.cookingeasy.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.cookingeasy.R
import com.example.cookingeasy.data.preferences.ShareprefConstants
import com.example.cookingeasy.databinding.ActivityMainBinding
import com.example.cookingeasy.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.e(TAG, "POST_NOTIFICATIONS granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureAuthenticated()) return
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val selectedId = savedInstanceState?.getInt(ShareprefConstants.KEY_STATE) ?: R.id.bottom_home
        binding.bottomNavigation.selectedItemId = selectedId

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        navController = navHostFragment.navController

        handleNotificationNavigation(intent)
        requestNotificationPermissionIfNeeded()
        setupFcmDebug()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val destinationId = when (item.itemId) {
                R.id.bottom_home -> R.id.homeFragment2
                R.id.bottom_explore -> R.id.exploreFragment
                R.id.bottom_add_recipe -> R.id.listChatFragment
                R.id.bottom_ai -> R.id.AIChatFragment2
                R.id.bottom_person -> R.id.myProfileFragment
                else -> null
            }
            destinationId?.let { id ->
                if (navController.currentDestination?.id != id) {
                    navController.navigate(id)
                }
                true
            } ?: false
        }
    }

    private fun ensureAuthenticated(): Boolean {
        if (FirebaseAuth.getInstance().currentUser != null) return true
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationNavigation(intent)
    }


    private fun handleNotificationNavigation(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_CHAT, false) != true) return
        val uid = intent.getStringExtra(EXTRA_CHAT_UID).orEmpty()
        if (uid.isBlank()) return
        val bundle = Bundle().apply {
            putString("userUid", uid)
            putString("userName", intent.getStringExtra(EXTRA_CHAT_NAME).orEmpty())
            putString("userAvatar", intent.getStringExtra(EXTRA_CHAT_AVATAR).orEmpty())
        }
        if (::navController.isInitialized) {
            navController.navigate(R.id.chatDetailFragment, bundle)
        }
        intent.removeExtra(EXTRA_OPEN_CHAT)
    }

    companion object {
        private const val TAG = "ChatFCM"
        const val EXTRA_OPEN_CHAT = "extra_open_chat"
        const val EXTRA_CHAT_UID = "extra_chat_uid"
        const val EXTRA_CHAT_NAME = "extra_chat_name"
        const val EXTRA_CHAT_AVATAR = "extra_chat_avatar"
    }

    private fun setupFcmDebug() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.e(TAG, "MainActivity token: $token")
                syncFcmToken(token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "MainActivity get token failed", e)
            }

        FirebaseMessaging.getInstance().subscribeToTopic("chat_debug")
            .addOnSuccessListener { Log.e(TAG, "Subscribed topic: chat_debug") }
            .addOnFailureListener { e -> Log.e(TAG, "Subscribe topic failed", e) }
    }

    private fun syncFcmToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank() || token.isBlank()) return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "fcm_tokens" to com.google.firebase.firestore.FieldValue.arrayUnion(token)
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { Log.e(TAG, "Synced fcmToken/fcm_tokens for uid=$uid") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync fcmToken", e) }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

}