package com.example.cookingeasy.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.cookingeasy.R
import com.example.cookingeasy.data.preferences.ShareprefConstants
import com.example.cookingeasy.databinding.ActivityMainBinding
import com.example.cookingeasy.ui.main.fragment.AIChatFragment
import com.example.cookingeasy.ui.main.fragment.ChatDetailFragment
import com.example.cookingeasy.ui.main.fragment.ExploreFragment
import com.example.cookingeasy.ui.main.fragment.HomeFragment
import com.example.cookingeasy.ui.main.fragment.ListChatFragment
import com.example.cookingeasy.ui.main.fragment.MyProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HomeFragment())
                .commit()
        }
        handleNotificationNavigation(intent)
        setupFcmDebug()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when(item.itemId) {
                R.id.bottom_home -> HomeFragment()
                R.id.bottom_explore -> ExploreFragment()
                R.id.bottom_add_recipe -> ListChatFragment()
                R.id.bottom_ai -> AIChatFragment()
                R.id.bottom_person -> MyProfileFragment()
                else -> null
            }
            fragment?.let {
                replaceFragment(it)
                true
            } ?: false
        }
    }

    fun replaceFragment(fragment: Fragment) {
        val fragmentTransacsion: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransacsion.replace(R.id.container, fragment).commit()
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
        val fragment = ChatDetailFragment().apply { arguments = bundle }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
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
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener { Log.e(TAG, "Synced fcmToken for uid=$uid") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync fcmToken", e) }
    }

}