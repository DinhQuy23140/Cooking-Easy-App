package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MealSimpleAdapter
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.data.remote.api.RecipeService
import com.example.cookingeasy.databinding.FragmentResultByCategoryBinding
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.ResultByCategoryViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.gson.Gson
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
class ResultByCategoryFragment : Fragment() {

    private lateinit var binding: FragmentResultByCategoryBinding
    private var strCategory = ""
    private val viewModel: ResultByCategoryViewModel by viewModels()
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
                    TODO("Not yet implemented")
                }

                override fun OnFavoriteClick(boolean: Boolean) {
                    TODO("Not yet implemented")
                }

            }
        )

        binding.rvRecipes.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            adapter = mealSimpleAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadData() {
        if (strCategory.isEmpty()) {
            Toast.makeText(requireContext(), "Not found category", Toast.LENGTH_SHORT).show()
            return
        }

        val category = Gson().fromJson(strCategory, Category::class.java)

        binding.tvCategoryName.text = category.strCategory
        binding.tvDescription.text = category.strCategoryDescription
        binding.tvRecipeCount.text = "Loading..."

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
                        mealSimpleAdapter.updateData(recipes)
                        binding.tvRecipeCount.text = "${recipes.size} recipes"
                        binding.layoutEmpty.isVisible = recipes.isEmpty()
                        binding.rvRecipes.isVisible = recipes.isNotEmpty()
                        listRecipeService = recipes
                        Log.d("ResultByCategoryFragment", "recipes: ${recipes.size}")
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

        binding.tvRecipeCount.text = "$count recipes"
        binding.layoutEmpty.isVisible = count == 0
        binding.rvRecipes.isVisible = count > 0
    }

    companion object {
        fun newInstance(category: String) = ResultByCategoryFragment().apply {
            arguments = Bundle().apply {
                putString("category", category)
            }
        }
    }
}