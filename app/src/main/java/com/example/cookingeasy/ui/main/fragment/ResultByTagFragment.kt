package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MealSimpleAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentResultByTagBinding
import com.example.cookingeasy.databinding.FragmentResultScanBinding
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.ResultByTagViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultByTagFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultByTagFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentResultByTagBinding
    private lateinit var mealSimpleAdapter: MealSimpleAdapter
    private var area: String = ""
    private val resultByTagViewModel: ResultByTagViewModel by viewModels()

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
        binding = FragmentResultByTagBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getInstance()
        loadData()
        binding.rvRecipesByTag.apply {
            layoutManager = GridLayoutManager(context, 2)
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            mealSimpleAdapter = MealSimpleAdapter(mutableListOf<Recipe>(), object : RecipeListener {
                override fun OnClickItem(recipe: Recipe) {
                    TODO("Not yet implemented")
                }

                override fun OnFavoriteClick(boolean: Boolean) {
                    TODO("Not yet implemented")
                }

            })
            adapter = mealSimpleAdapter
        }
        observe()
    }

    fun getInstance() {
        val bundle = arguments
        area = bundle?.getString("area") ?:  ""
    }

    fun loadData() {
        binding.tvAreaName.text = "Area: " + area
        resultByTagViewModel.getRecipesByArea(area)
    }

    fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                resultByTagViewModel.recipeByArea.collect {
                    Log.d("Data area: ", it.size.toString())
                    mealSimpleAdapter.updateData(it)
                    binding.txtResultCount.text = it.size.toString() + " recipes found"
                }
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
         * @return A new instance of fragment ResultByTagFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ResultByTagFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}