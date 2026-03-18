package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.AreaAdapter
import com.example.cookingeasy.common.adapter.CategoryAdapter
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.AreaListener
import com.example.cookingeasy.common.listener.CategoryListener
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentExploreBinding
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.ExploreViewModel
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.google.gson.Gson
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ExploreFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExploreFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentExploreBinding
    private val viewmodel: ExploreViewModel by viewModels()
    private lateinit var recipe: Recipe
    private lateinit var areaAdapter: AreaAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeShareViewModel: RecipeShareViewmodel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExploreBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        setupListeners()
        loadData()
        observeData()
    }

    fun loadData() {
        viewmodel.getRandomRecipe()
        viewmodel.getCategories()
        viewmodel.getAreas()
        viewmodel.getTrending()
    }

    fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    Log.d("ExploreFragment Random", "observeData: ${viewmodel.randomRecipe.value}")
                    viewmodel.randomRecipe.collect {
                        if (it != null) {
                            recipe = it
                            binding.tvFeaturedName.text = it.strMeal
                            binding.tvFeaturedCategory.text = (it.strCategory)
                            binding.tvFeaturedArea.text = (it.strArea)
                            binding.progressFeatured.visibility = View.GONE
                            Glide.with(requireContext())
                                .load(it.strMealThumb)
                                .placeholder(R.drawable.ic_ingredients)
                                .into(binding.imgFeatured)
                        }
                    }
                }
                launch {
                    viewmodel.categories.collect {
                        categoryAdapter.updateData(it)
                    }
                }
                launch {
                    viewmodel.areas.collect {
                        areaAdapter.updateData(it)
                    }
                }
                launch {
                    viewmodel.trendingRecipes.collect {
                        recipeAdapter.updateData(it)
                    }
                }
            }
        }
    }

    fun setupUI() {
        binding.rvCategory.apply {
            categoryAdapter = CategoryAdapter(mutableListOf(), object: CategoryListener{
                override fun onClickItem(category: Category) {

                }
            })
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.rvArea.apply {
            areaAdapter = AreaAdapter(mutableListOf(), object: AreaListener{
                override fun OnClickItem(are: Area) {

                }
            })
            adapter = areaAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.rvTrending.apply {
            recipeAdapter = RecipeAdapter(mutableListOf(), object: RecipeListener{
                override fun OnClickItem(recipe: Recipe) {

                }

                override fun OnFavoriteClick(recipe: Recipe) {

                }

            })
            adapter = recipeAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    fun setupListeners() {
        binding.edtSearch.setOnClickListener {
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.container, SearchFragment())
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

        binding.btnCookNow.setOnClickListener {
            recipe.let {
                recipeShareViewModel.selectedRecipe(recipe)
                val fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.container, RecipeDetailFragment())
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ExploreFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExploreFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}