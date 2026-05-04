package com.example.cookingeasy.ui.main.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.AreaAdapter
import com.example.cookingeasy.common.adapter.CategoryAdapter
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.AreaListener
import com.example.cookingeasy.common.listener.CategoryListener
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentHomeBinding
import com.example.cookingeasy.domain.model.Area
import com.example.cookingeasy.domain.model.Category
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.HomeViewModel
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.gson.Gson
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
    private lateinit var binding: FragmentHomeBinding

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var areaAdapter: AreaAdapter
    private lateinit var recipeAdapter: RecipeAdapter
    private var listCategory = mutableListOf<Category>()
    private var shortListCategory = mutableListOf<Category>()

    private var listArea = mutableListOf<Area>()
    private var shortListArea = mutableListOf<Area>()

    private var isLoadingMore = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
        event()
        loadData()
        observeData()
    }

    private fun setup() {
        categoryAdapter = CategoryAdapter(mutableListOf(), object : CategoryListener {
            override fun onClickItem(category: Category) {
                val fragment = ResultByCategoryFragment()
                val bundle = Bundle()
                bundle.putString("category", Gson().toJson(category))
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right
                    )
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        })

        areaAdapter = AreaAdapter(mutableListOf(), object : AreaListener {
            override fun OnClickItem(area: Area) {
                val fragment = ResultByTagFragment()
                val bundle = Bundle()
                bundle.putString("area", area.name)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right
                    )
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        })

        recipeAdapter = RecipeAdapter(mutableListOf(), object : RecipeListener {
            override fun OnClickItem(recipe: Recipe) {
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

            override fun OnFavoriteClick(recipe: Recipe) {
                homeViewModel.toggleFavorite(recipe)
            }
        })

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(context, 4)
            addItemDecoration(GridSpacingItemDecoration(4, 3))
            adapter = categoryAdapter
            setHasFixedSize(true)
        }

        binding.rvAreas.apply {
            val column = homeViewModel.caculatorColumn(context)
            layoutManager = GridLayoutManager(context, column)
            addItemDecoration(GridSpacingItemDecoration(column, 3))
            adapter = areaAdapter
            setHasFixedSize(true)
        }

        binding.rvRecipes.apply {
            layoutManager = GridLayoutManager(context, 2)
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            adapter = recipeAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun event() {
        binding.edtSearch.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right
                )
                .replace(R.id.container, SearchFragment())
                .addToBackStack(null)
                .commit()
        }

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

        binding.btnFavorite.setOnClickListener {
            val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right
                )
            fragmentTransaction.replace(R.id.container, FavoriteFragment())
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

        binding.tvSeeAllCategories.setOnClickListener {
            categoryAdapter.updateData(listCategory)
            binding.tvHide.isVisible = true
            binding.tvSeeAllCategories.isVisible = false
        }

        binding.tvHide.setOnClickListener {
            categoryAdapter.updateData(shortListCategory)
            binding.tvHide.isVisible = false
            binding.tvSeeAllCategories.isVisible = true
        }

        binding.tvSeeAllAreas.setOnClickListener {
            areaAdapter.updateData(listArea)
            binding.tvSeeAllAreas.isVisible = false
            binding.tvHideAreas.isVisible = true
        }

        binding.tvHideAreas.setOnClickListener {
            areaAdapter.updateData(shortListArea)
            binding.tvSeeAllAreas.isVisible = true
            binding.tvHideAreas.isVisible = false
        }
    }


    private fun loadData() {
        homeViewModel.getListCategory()
        homeViewModel.getListArea()
        homeViewModel.loadFavorites()
        homeViewModel.getInfUser()
        setUpMessageTime()
    }

    fun setUpMessageTime() {
        val periodRes = when (homeViewModel.getDayPeriod()) {
            HomeViewModel.DayPeriod.MORNING -> R.string.day_period_morning
            HomeViewModel.DayPeriod.AFTERNOON -> R.string.day_period_afternoon
            HomeViewModel.DayPeriod.EVENING -> R.string.day_period_evening
            HomeViewModel.DayPeriod.NIGHT -> R.string.day_period_night
        }
        binding.txtGreeting.text = getString(
            R.string.greeting_template,
            getString(periodRes)
        )
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    homeViewModel.lisCategory
                        .filter { it.isNotEmpty() }
                        .collect { data ->
                            listCategory = data as MutableList<Category>
                            shortListCategory.addAll(listCategory.subList(0, 8))
                            categoryAdapter.updateData(shortListCategory)
                            binding.tvCategoryCount.text = getString(R.string.stats_count_plus, data.size)
                        }
                }

                launch {
                    homeViewModel.listArea
                        .filter { it.isNotEmpty() }
                        .collect { data ->
                            listArea = data as MutableList<Area>
                            shortListArea.addAll(listArea.subList(0, 8))
                            areaAdapter.updateData(shortListArea)
                            binding.tvCuisneCount.text = getString(R.string.stats_count_plus, data.size)
                        }
                }

                launch {
                    homeViewModel.listRecipe
                        .filter { it.isNotEmpty() }
                        .distinctUntilChanged()
                        .collect { data ->
                            recipeAdapter.updateData(data)
                            binding.tvRecipeCount.text = getString(R.string.stats_count_plus, data.size)
                        }
                }
                launch {
                    homeViewModel.userName.collect { binding.txtUserName.text = it }
                    homeViewModel.imgUrl.collect {
                        Glide.with(binding.imgAvatar)
                            .load(it)
                            .into(binding.imgAvatar)
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
