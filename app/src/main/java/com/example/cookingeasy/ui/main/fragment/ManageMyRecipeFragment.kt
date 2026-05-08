package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MyRecipeAdapter
import com.example.cookingeasy.common.listener.UploadRecipeListener
import com.example.cookingeasy.databinding.FragmentManageMyRecipeBinding
import com.example.cookingeasy.domain.mapper.toRecipe
import com.example.cookingeasy.domain.model.RecipeUpload
import com.example.cookingeasy.ui.viewmodel.MyRecipesViewModel
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageMyRecipeFragment : Fragment() {

    private lateinit var binding: FragmentManageMyRecipeBinding
    private val viewModel: MyRecipesViewModel by activityViewModels()
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
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

    private fun setupRecyclerView() {
        adapter = MyRecipeAdapter(mutableListOf(), object : UploadRecipeListener {
            override fun onItemClick(recipe: RecipeUpload) {
                recipeShareViewmodel.selectedRecipe(recipe.toRecipe())
                findNavController().navigate(R.id.recipeDetailFragment)
            }

            override fun onEdit(recipe: RecipeUpload) {
            }

            override fun onDelete(recipe: RecipeUpload) {
            }

        })
        binding.rvMyRecipes.apply {
            adapter = this@ManageMyRecipeFragment.adapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
        }
    }

    private fun setupEvents() {
        binding.btnAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.addRecipeFragment)
        }

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

        binding.btnAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.addRecipeFragment)
        }

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

                chips.keys.forEach { c ->
                    c.setBackgroundResource(R.drawable.shape_circle_glass)
                    c.setTextColor(0xCCFFFFFF.toInt())
                }

                chip.setBackgroundResource(R.drawable.shape_btn_primary)
                chip.setTextColor(0xFFFFFFFF.toInt())

                viewModel.filter(
                    binding.edtSearchRecipe.text.toString(),
                    currentFilter
                )
            }
        }
    }

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

    private fun showPublishConfirm(recipe: RecipeUpload) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.publish_recipe_title))
            .setMessage(getString(R.string.publish_recipe_message, recipe.mealName))
            .setPositiveButton(getString(R.string.action_publish)) { _, _ ->
                viewModel.publishRecipe(recipe.recipeId)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showDeleteConfirm(recipe: RecipeUpload) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_recipe_title))
            .setMessage(getString(R.string.delete_recipe_message, recipe.mealName))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                viewModel.deleteRecipe(recipe.recipeId)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.error_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.action_ok), null)
            .show()
    }

    companion object {
        fun newInstance() = ManageMyRecipeFragment()
    }
}