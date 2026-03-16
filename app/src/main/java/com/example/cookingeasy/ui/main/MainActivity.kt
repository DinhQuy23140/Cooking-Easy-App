package com.example.cookingeasy.ui.main

import android.os.Bundle
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
import com.example.cookingeasy.ui.main.fragment.ManageMyRecipeFragment
import com.example.cookingeasy.ui.main.fragment.ExploreFragment
import com.example.cookingeasy.ui.main.fragment.HomeFragment
import com.example.cookingeasy.ui.main.fragment.MyProfileFragment

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

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when(item.itemId) {
                R.id.bottom_home -> HomeFragment()
                R.id.bottom_explore -> ExploreFragment()
                R.id.bottom_add_recipe -> ManageMyRecipeFragment()
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
}