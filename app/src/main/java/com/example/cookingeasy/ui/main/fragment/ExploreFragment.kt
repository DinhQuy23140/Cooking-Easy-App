package com.example.cookingeasy.ui.main.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.ranges.contains

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

    @SuppressLint("SetTextI18n")
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
                        binding.tvTrendingCount.text = it.size.toString() + "picks"
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

        binding.btnAddRecipe.setOnClickListener {
            if (!isAdded || parentFragmentManager.isStateSaved) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.container, AddRecipeFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnRefreshFeatured.setOnClickListener {
            if (binding.progressRefreshFeatured.isVisible) return@setOnClickListener
            binding.btnRefreshFeatured.isVisible = false
            binding.progressRefreshFeatured.isVisible = true
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    delay(REFRESH_FEATURED_DELAY_MS)
                    viewmodel.loadRandomRecipe()
                } finally {
                    if (!isAdded) return@launch
                    binding.btnRefreshFeatured.isVisible = true
                    binding.progressRefreshFeatured.isVisible = false
                }
            }
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

        binding.tvSeeAllCategory.setOnClickListener {
            binding.rvCategory.apply {
                layoutManager = GridLayoutManager(context, 4)
                addItemDecoration(GridSpacingItemDecoration(4, 3))
                setHasFixedSize(true)
                binding.tvSeeAllCategory.visibility = View.GONE
                binding.tvPopularHide.visibility = View.VISIBLE
            }
        }

        binding.tvPopularHide.setOnClickListener {
            binding.rvCategory.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                binding.tvSeeAllCategory.visibility = View.VISIBLE
                binding.tvPopularHide.visibility = View.GONE
            }
        }

        binding.tvSeeAllArea.setOnClickListener {
            binding.rvArea.apply {
                val column = viewmodel.caculatorColumn(context)
                layoutManager = GridLayoutManager(context, column)
                addItemDecoration(GridSpacingItemDecoration(column, 3))
                setHasFixedSize(true)
                binding.tvSeeAllArea.visibility = View.GONE
                binding.tvExploreHide.visibility = View.VISIBLE
            }
        }

        binding.tvExploreHide.setOnClickListener {
            binding.rvArea.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                binding.tvSeeAllArea.visibility = View.VISIBLE
                binding.tvExploreHide.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val REFRESH_FEATURED_DELAY_MS = 300L

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