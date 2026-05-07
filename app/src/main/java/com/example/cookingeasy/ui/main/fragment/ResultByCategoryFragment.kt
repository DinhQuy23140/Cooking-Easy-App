package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MealSimpleAdapter
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.data.remote.api.RecipeService
import com.example.cookingeasy.databinding.FragmentResultByCategoryBinding
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.ui.viewmodel.ResultByCategoryViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultByCategoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

@AndroidEntryPoint
class ResultByCategoryFragment : Fragment() {

    private lateinit var binding: FragmentResultByCategoryBinding
    private var strCategory = ""
    private val viewModel: ResultByCategoryViewModel by viewModels()
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
    private lateinit var mealSimpleAdapter: MealSimpleAdapter
    private lateinit var listRecipeService: List<Recipe>
    private var isLoadingMore = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultByCategoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getInstance()
        setupRecyclerView()
        loadData()
        observe()
        setupClickListeners()
    }

    private fun getInstance() {
        strCategory = arguments?.getString("category") ?: ""
    }

    private fun setupRecyclerView() {
        mealSimpleAdapter = MealSimpleAdapter(
            listMeal = mutableListOf(),
            object : RecipeListener{
                override fun OnClickItem(recipe: Recipe) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.getRecipeById(recipe.idMeal.toString())
                            .onSuccess { fullRecipe ->
                                showRecipePreviewDialog(fullRecipe)
                            }
                            .onFailure {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.data_not_found),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                override fun OnFavoriteClick(recipe: Recipe) {
                    viewModel.toggleFavorite(recipe)
                }

                override fun onClickInf(recipe: Recipe) {
                    if (recipe.userUid.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            R.string.other_user_profile_unavailable,
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()
                    fragmentTransaction.setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right
                    )
                    fragmentTransaction.replace(
                        R.id.container,
                        OtherUserProfileFragment.newInstance(recipe.userUid)
                    )
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                }
            }
        )

        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = mealSimpleAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadData() {
        if (strCategory.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.category_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val category = Gson().fromJson(strCategory, Category::class.java)

        binding.tvCategoryName.text = category.strCategory
        binding.tvDescription.text = category.strCategoryDescription
        binding.tvRecipeCount.text = getString(R.string.loading)

        Glide.with(requireContext())
            .load(category.strCategoryThumb)
            .placeholder(R.drawable.ic_category)
            .error(R.drawable.ic_category)
            .into(binding.imgCategory)

        category.strCategory?.let {
            viewModel.getRecipesByCategory(it.lowercase(Locale.ROOT))
        }
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recipesByCategory.collect { recipes ->
                        if (!recipes.isEmpty()){
                            mealSimpleAdapter.updateData(recipes)
                            binding.tvRecipeCount.text = resources.getQuantityString(
                                R.plurals.recipe_count,
                                recipes.size,
                                recipes.size
                            )
                            binding.layoutEmpty.isVisible = recipes.isEmpty()
                            binding.rvRecipes.isVisible = recipes.isNotEmpty()
                            listRecipeService = recipes
                            Log.d("ResultByCategoryFragment", "recipes: ${recipes.size}")
                        }
                    }
                }

                launch {
//                    viewModel.isLoading.collect { isLoading ->
//                        binding.progressBar.isVisible = isLoading
//                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnExpand.setOnClickListener {
            binding.tvDescription.apply {
                if (maxLines == 3) {
                    maxLines = Int.MAX_VALUE
                    binding.btnExpand.rotation = 90f
                } else {
                    maxLines = 3
                    binding.btnExpand.rotation = -90f
                }
            }
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {
            }

            override fun onTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {
                val keyWord = p0.toString().trim()
                binding.btnClear.isVisible = keyWord.isNotEmpty()
                filterRecipes(keyWord)
            }

        })

        binding.btnClear.setOnClickListener {
            binding.edtSearch.text = null
            binding.btnClear.isVisible = false
            mealSimpleAdapter.updateData(listRecipeService)
        }

        binding.content.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                if (isLoadingMore) return@OnScrollChangeListener

                val totalHeight = v.getChildAt(0).measuredHeight
                val scrollViewHeight = v.measuredHeight

                if (scrollY >= totalHeight - scrollViewHeight - 200) {
                    if (mealSimpleAdapter.hasMoreData()) {
                        isLoadingMore = true
                        binding.layoutLoadingMore.isVisible = true
                        mealSimpleAdapter.loadNextPage()
                        binding.layoutLoadingMore.isVisible = false
                        isLoadingMore = false
                    }
                }
            }
        )
    }

    private fun filterRecipes(keyword: String) {
        if (keyword.isEmpty()) {
            mealSimpleAdapter.clearFilter()
        } else {
            val filtered = viewModel.recipesByCategory.value.filter { meal ->
                meal.strMeal.contains(keyword, ignoreCase = true)
            }
            mealSimpleAdapter.updateDisplay(filtered)
        }

        val count = if (keyword.isEmpty())
            viewModel.recipesByCategory.value.size
        else
            viewModel.recipesByCategory.value.count {
                it.strMeal.contains(keyword, ignoreCase = true)
            }

        binding.tvRecipeCount.text = resources.getQuantityString(
            R.plurals.recipe_count,
            count,
            count
        )
        binding.layoutEmpty.isVisible = count == 0
        binding.rvRecipes.isVisible = count > 0
    }

    private fun showRecipePreviewDialog(recipe: Recipe) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recipe_preview, null)
        val imgPreview = dialogView.findViewById<ImageView>(R.id.imgRecipePreview)
        val tvName = dialogView.findViewById<TextView>(R.id.tvRecipeNamePreview)
        val tvMeta = dialogView.findViewById<TextView>(R.id.tvRecipeMetaPreview)
        val tvInstruction = dialogView.findViewById<TextView>(R.id.tvInstructionPreview)

        tvName.text = recipe.strMeal.ifEmpty { getString(R.string.data_not_found) }
        tvMeta.text = getString(
            R.string.dialog_recipe_preview_meta,
            recipe.strCategory.ifEmpty { "-" },
            recipe.strArea.ifEmpty { "-" }
        )
        tvInstruction.text = recipe.strInstructions
            .trim()
            .ifEmpty { getString(R.string.data_not_found) }
            .let { if (it.length > 240) "${it.take(237)}..." else it }

        Glide.with(this)
            .load(recipe.strMealThumb)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_cooking)
            .into(imgPreview)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.view_detail) { _, _ ->
                recipeShareViewmodel.selectedRecipe(recipe)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right
                    )
                    .replace(R.id.container, RecipeDetailFragment())
                    .addToBackStack(null)
                    .commit()
            }
            .show()
    }

    companion object {
        fun newInstance(category: String) = ResultByCategoryFragment().apply {
            arguments = Bundle().apply {
                putString("category", category)
            }
        }
    }
}