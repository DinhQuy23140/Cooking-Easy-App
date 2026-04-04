package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentFavoriteBinding
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.FavoriteViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FavoriteFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FavoriteFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var binding: FragmentFavoriteBinding
    private val viewmodel: FavoriteViewModel by viewModels()
    private lateinit var recipeAdapter: RecipeAdapter
    private var param1: String? = null
    private var param2: String? = null

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
        binding = FragmentFavoriteBinding.inflate(layoutInflater)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FavoriteFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FavoriteFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        loadData()
        observe()
    }

    fun setUpRecyclerView() {
        binding.rvFavoriteRecipes.apply {
            recipeAdapter = RecipeAdapter(mutableListOf(), object: RecipeListener {
                override fun OnClickItem(recipe: Recipe) {

                }

                override fun OnFavoriteClick(recipe: Recipe) {
                }
            })
            adapter = recipeAdapter
            layoutManager = GridLayoutManager(context, 2)
            addItemDecoration(GridSpacingItemDecoration(2, 5))
        }
    }

    fun loadData() {
        viewmodel.getFavoriteRecipes()
    }

    fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewmodel.favoriteRecipes.collect {
                recipeAdapter.updateData(it)
            }
        }
    }
}