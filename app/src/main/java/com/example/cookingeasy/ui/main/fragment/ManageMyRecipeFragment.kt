package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MyRecipeAdapter
import com.example.cookingeasy.databinding.FragmentManageMyRecipeBinding
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.ui.viewmodel.MyRecipesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ManageMyRecipeFragment : Fragment() {

    private lateinit var binding: FragmentManageMyRecipeBinding
    private val viewModel: MyRecipesViewModel by viewModels {
        MyRecipesViewModel.Factory(requireContext().contentResolver)
    }
    private lateinit var adapter: MyRecipeAdapter
    private var currentFilter = "all"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentManageMyRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupEvents()
        observeViewModel()
        viewModel.loadMyRecipes()
    }

    // ─────────────────────────────────────────────
    // Setup RecyclerView
    // ─────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = MyRecipeAdapter(
            mutableListOf()
        )
        binding.rvMyRecipes.apply {
            adapter = this@ManageMyRecipeFragment.adapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
        }
    }

    // ─────────────────────────────────────────────
    // Events
    // ─────────────────────────────────────────────

    private fun setupEvents() {
        binding.btnClear.setOnClickListener {
            binding.edtSearchRecipe.setText("")
        }

        binding.edtSearchRecipe.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClear.isVisible = s?.isNotEmpty() == true
                viewModel.filter(s.toString(), currentFilter)
            }
        })

        setupFilterChips()
    }

    private fun setupFilterChips() {
        val chips = mapOf(
            binding.filterAll       to "all",
            binding.filterDraft     to "draft",
            binding.filterPublished to "published",
            binding.filterFavorite  to "favorite"
        )

        chips.forEach { (chip, filter) ->
            chip.setOnClickListener {
                currentFilter = filter

                // Reset tất cả về inactive
                chips.keys.forEach { c ->
                    c.setBackgroundResource(R.drawable.shape_circle_glass)
                    c.setTextColor(0xCCFFFFFF.toInt())
                }

                // Active chip được chọn
                chip.setBackgroundResource(R.drawable.shape_btn_primary)
                chip.setTextColor(0xFFFFFFFF.toInt())

                viewModel.filter(
                    binding.edtSearchRecipe.text.toString(),
                    currentFilter
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // Observe ViewModel
    // ─────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.filteredRecipes.collect { recipes ->
                        adapter.update(recipes)
                        binding.rvMyRecipes.isVisible = recipes.isNotEmpty()
                        binding.layoutEmpty.isVisible =
                            recipes.isEmpty() && !viewModel.isLoading.value
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.rvMyRecipes.isVisible = !isLoading
                        if (isLoading) binding.layoutEmpty.isVisible = false
                    }
                }

//                launch {
//                    viewModel.error.collect { error ->
//                        error?.let {
//                            showError(it)
//                            viewModel.clearError()
//                        }
//                    }
//                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Dialogs
    // ─────────────────────────────────────────────

    private fun showPublishConfirm(recipe: RecipeUpload) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Publish Recipe")
            .setMessage("Publish \"${recipe.mealName}\"? It will be visible to everyone.")
            .setPositiveButton("Publish") { _, _ ->
                viewModel.publishRecipe(recipe.recipeId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirm(recipe: RecipeUpload) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Recipe")
            .setMessage("Delete \"${recipe.mealName}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRecipe(recipe.recipeId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance() = ManageMyRecipeFragment()
    }
}