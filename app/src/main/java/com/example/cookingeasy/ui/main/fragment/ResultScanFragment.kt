package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.R
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultScanBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Parse ingredients từ bundle
        val strIngredients = arguments?.getString("ingredients") ?: ""
        val ingredients: List<Ingredient> = Gson().fromJson(
            strIngredients,
            object : TypeToken<List<Ingredient>>() {}.type
        )

        // Lấy tên ingredient viết thường
        listIngredientName.addAll(ingredients.map { it.name.lowercase(Locale.ROOT) })

        // Setup ingredient RecyclerView
        ingredientDetailAdapter = IngredientDetailAdapter(ingredients)
        binding.recyclerIngredients.apply {
            adapter = ingredientDetailAdapter
        }
        binding.tvIngredientCount.text = "${ingredients.size} items"

        // Setup recipe RecyclerView trước
        recipeAdapter = RecipeAdapter(mutableListOf(), object : RecipeListener {
            override fun OnClickItem(recipe: Recipe) {
                // navigate to detail
            }
            override fun OnFavoriteClick(boolean: Boolean) {
                // handle favorite
            }
        })

        binding.recyclerRecipes.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerRecipes.addItemDecoration(
            GridSpacingItemDecoration(2, 3)
        )
        binding.recyclerRecipes.apply {
            adapter = recipeAdapter
        }

        // Back button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        observe()
        loadData()
    }

    private fun loadData() {
        resultScanViewModel.getRecipesByIngredients(listIngredientName)
    }

    private fun observe() {
        // Observe loading
        viewLifecycleOwner.lifecycleScope.launch {
            resultScanViewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
                if (isLoading) {
                    binding.tvRecipeCount.text = "Loading..."
                    binding.layoutEmptyRecipes.isVisible = false
                }
            }
        }

        // Observe recipes
        viewLifecycleOwner.lifecycleScope.launch {
            resultScanViewModel.recipeByIngredients.collect { recipes ->
                recipeAdapter.updateData(recipes)
                binding.tvRecipeCount.text = "${recipes.size} found"
                binding.layoutEmptyRecipes.isVisible = recipes.isEmpty()
                binding.recyclerRecipes.isVisible = recipes.isNotEmpty()
                binding.progressBar.isVisible = false
                binding.layoutLoadingRecipes.isVisible = false
                Log.d("ResultScan", "Recipes found: ${recipes.size}")
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