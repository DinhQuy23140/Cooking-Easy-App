package com.example.cookingeasy.ui.main.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.example.cookingeasy.util.Constants
import com.example.cookingeasy.util.GridSpacingItemDecoration
import com.google.android.material.transition.MaterialFadeThrough
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
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val homeViewModel: HomeViewModel by viewModels()
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
    private var categoryAdapter: CategoryAdapter? = null
    private var areaAdapter: AreaAdapter? = null
    private var recipeAdapter: RecipeAdapter? = null
    private lateinit var binding: FragmentHomeBinding

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
        // Inflate the layout for this fragment
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
    fun setup() {
        binding.rvCategories.layoutManager = GridLayoutManager(context, 4)
        binding.rvCategories.addItemDecoration(
            GridSpacingItemDecoration(4, 3)
        )

        binding.rvAreas.layoutManager = GridLayoutManager(context, 3)
        binding.rvAreas.addItemDecoration(
            GridSpacingItemDecoration(3, 3)
        )

        binding.rvRecipes.layoutManager = GridLayoutManager(context, 2)
        binding.rvRecipes.addItemDecoration(
            GridSpacingItemDecoration(2, 3)
        )
    }

    @SuppressLint("SuspiciousIndentation")
    fun event() {
        binding.edtSearch.setOnClickListener {
            val fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            val fragment = SearchFragment()

            fragmentTransaction.replace(R.id.container, fragment).addToBackStack(null)
            fragmentTransaction.commit()
        }
    }

    fun loadData() {
        homeViewModel.getListCategory()
        homeViewModel.getListArea()
        homeViewModel.getRecipes()
    }

    fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.lisCategory.collect { data ->
                    if (!data.isEmpty()) {
                        categoryAdapter = CategoryAdapter(data, object:CategoryListener {
                            override fun onClickItem(category: Category) {
                                val bundle: Bundle = Bundle()
                                bundle.putString(Constants.KEY_SEARCH, Constants.KEY_CATEGORY)
                                val fragmentTransacsion: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                                fragmentTransacsion.replace(R.id.container, ResultByTagFragment())
                                fragmentTransacsion.addToBackStack(null)
                                fragmentTransacsion.commit()
                            }
                        })
                        binding.rvCategories.adapter = categoryAdapter
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.listArea.collect { data ->
                if (!data.isEmpty()) {
                    areaAdapter = AreaAdapter(data, object : AreaListener {
                        override fun OnClickItem(are: Area) {
                            TODO("Not yet implemented")
                        }
                    })
                    binding.rvAreas.adapter = areaAdapter
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.listRecipe.collect { data ->
                if (!data.isEmpty()) {
                    recipeAdapter = RecipeAdapter(data as MutableList<Recipe>, object : RecipeListener {
                        override fun OnClickItem(recipe: Recipe) {
                            recipeShareViewmodel.selectedRecipe(recipe)
                            val fragmentTransaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                            fragmentTransaction.replace(R.id.container, RecipeDetailFragment())
                            fragmentTransaction.addToBackStack(null).commit()
                        }

                        override fun OnFavoriteClick(boolean: Boolean) {

                        }

                    })
                    binding.rvRecipes.adapter = recipeAdapter
                }
            }
        }
    }

    companion object {
        // TODO: Rename and change types and number of parameters
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