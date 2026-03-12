package com.example.cookingeasy.ui.main.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.common.adapter.IngredientDetailAdapter
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentResultScanBinding
import com.example.cookingeasy.domain.model.Ingredient
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.ResultScanViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultScanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultScanFragment : Fragment() {

    private lateinit var binding: FragmentResultScanBinding
    private lateinit var ingredientDetailAdapter: IngredientDetailAdapter
    private lateinit var recipeAdapter: RecipeAdapter
    private val resultScanViewModel: ResultScanViewModel by viewModels()
    private val listIngredientName: MutableList<String> = mutableListOf()
    private var isLoadingMore = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultScanBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIngredients()
        setupRecyclerViews()
        setupScrollListener()
        setupClickListeners()
        observe()
        loadData()
    }

    @SuppressLint("SetTextI18n")
    private fun setupIngredients() {
        val strIngredients = arguments?.getString("ingredients") ?: ""
        val ingredients: List<Ingredient> = Gson().fromJson(
            strIngredients,
            object : TypeToken<List<Ingredient>>() {}.type
        )
        listIngredientName.addAll(ingredients.map { it.name.lowercase(Locale.ROOT) })

        ingredientDetailAdapter = IngredientDetailAdapter(ingredients)
        binding.recyclerIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientDetailAdapter
            setHasFixedSize(true)
        }
        binding.tvIngredientCount.text = "${ingredients.size} items"
    }

    private fun setupRecyclerViews() {
        recipeAdapter = RecipeAdapter(mutableListOf(), object : RecipeListener {
            override fun OnClickItem(recipe: Recipe) {
                // navigate to detail
            }
            override fun OnFavoriteClick(boolean: Boolean) { }
        })

        binding.recyclerRecipes.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            adapter = recipeAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupScrollListener() {
        binding.content.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                if (isLoadingMore) return@OnScrollChangeListener

                val totalHeight = v.getChildAt(0).measuredHeight
                val scrollViewHeight = v.measuredHeight

                if (scrollY >= totalHeight - scrollViewHeight - 200) {
                    if (recipeAdapter.hasMoreData()) {
                        isLoadingMore = true
                        recipeAdapter.loadNextPage()
                        isLoadingMore = false
                    }
                }
            }
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadData() {
        resultScanViewModel.getRecipesByIngredients(listIngredientName)
    }

    @SuppressLint("SetTextI18n")
    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading
                launch {
                    resultScanViewModel.isLoading.collect { isLoading ->
                        binding.layoutLoadingRecipes.isVisible = isLoading
                        if (isLoading) {
                            binding.tvRecipeCount.text = "Loading..."
                            binding.layoutEmptyRecipes.isVisible = false
                            binding.recyclerRecipes.isVisible = false
                        }
                    }
                }

                // Observe recipes
                launch {
                    resultScanViewModel.recipeByIngredients
                        .filter { it.isNotEmpty() }
                        .distinctUntilChanged()
                        .collect { recipes ->
                            binding.layoutLoadingRecipes.isVisible = false
                            binding.progressBar.isVisible = false
                            binding.tvRecipeCount.text = "${recipes.size} found"
                            binding.layoutEmptyRecipes.isVisible = recipes.isEmpty()
                            binding.recyclerRecipes.isVisible = recipes.isNotEmpty()
                            recipeAdapter.updateData(recipes)
                            Log.d("ResultScan", "Recipes found: ${recipes.size}")
                        }
                }
            }
        }
    }

    companion object {
        fun newInstance(ingredients: String) = ResultScanFragment().apply {
            arguments = Bundle().apply {
                putString("ingredients", ingredients)
            }
        }
    }
}