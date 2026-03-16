package com.example.cookingeasy.ui.main.fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
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
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
    private lateinit var binding: FragmentHomeBinding

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var areaAdapter: AreaAdapter
    private lateinit var recipeAdapter: RecipeAdapter

    private var isLoadingMore = false // ← tránh gọi loadNextPage nhiều lần

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
                    .replace(R.id.container, RecipeDetailFragment())
                    .addToBackStack(null)
                    .commit()
            }
            override fun OnFavoriteClick(boolean: Boolean) { }
        })

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(context, 4)
            addItemDecoration(GridSpacingItemDecoration(4, 3))
            adapter = categoryAdapter
            setHasFixedSize(true)
        }

        binding.rvAreas.apply {
            layoutManager = GridLayoutManager(context, 3)
            addItemDecoration(GridSpacingItemDecoration(3, 3))
            adapter = areaAdapter
            setHasFixedSize(true)
        }

        binding.rvRecipes.apply {
            layoutManager = GridLayoutManager(context, 2)
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            adapter = recipeAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

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

                // Khi scroll gần cuối (còn 200px)
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

    private fun loadData() {
        homeViewModel.getListCategory()
        homeViewModel.getListArea()
        homeViewModel.getRecipes()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.lisCategory
                        .filter { it.isNotEmpty() }
                        .collect { data ->
                            categoryAdapter.updateData(data)
                            binding.tvCategoryCount.text = data.size.toString() + "+"
                        }
                }
                launch {
                    homeViewModel.listArea
                        .filter { it.isNotEmpty() }
                        .collect { data ->
                            areaAdapter.updateData(data)
                            binding.tvCuisneCount.text = data.size.toString() + "+"
                        }
                }
                launch {
                    homeViewModel.listRecipe
                        .filter { it.isNotEmpty() }
                        .distinctUntilChanged()
                        .collect { data ->
                            recipeAdapter.updateData(data)
                            binding.tvRecipeCount.text = data.size.toString() + "+"
                        }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}