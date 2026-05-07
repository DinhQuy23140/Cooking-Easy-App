package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.MealSimpleAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentResultByTagBinding
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.ui.viewmodel.ResultByTagViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
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

@AndroidEntryPoint
class ResultByTagFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentResultByTagBinding
    private lateinit var mealSimpleAdapter: MealSimpleAdapter
    private var area: String = ""
    private val resultByTagViewModel: ResultByTagViewModel by viewModels()
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
    private var isLoadingMore = false
    private lateinit var listRecipe: List<Recipe>

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
        setUpRecyclerView()
        setUpListeners()
        observe()
    }

    private fun setUpListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
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
                val keyWord = p0.toString()
                binding.btnClear.isVisible = keyWord.isNotEmpty()
                filterRecipes(keyWord)
            }

        })

        binding.btnClear.setOnClickListener {
            binding.edtSearch.text = null
            binding.btnClear.isVisible = false
            mealSimpleAdapter.updateData(listRecipe)
        }
    }

    private fun filterRecipes(string: String) {
        if (string.isEmpty()) {
            mealSimpleAdapter.updateData(listRecipe)
        } else {
            val filteredList = resultByTagViewModel.recipeByArea.value.filter {
                it.strMeal.contains(string, ignoreCase = true)
            }
            mealSimpleAdapter.updateData(filteredList)
        }
        val count = if (string.isEmpty()) listRecipe.size
        else resultByTagViewModel.recipeByArea.value.filter {
            it.strMeal.contains(string, ignoreCase = true)
        }.size
        binding.txtResultCount.text = getString(R.string.recipes_found_count, count)
        binding.layoutEmpty.isVisible = count == 0
        binding.rvRecipesByTag.isVisible = count > 0
    }

    private fun setUpRecyclerView() {
        binding.rvRecipesByTag.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            mealSimpleAdapter = MealSimpleAdapter(mutableListOf<Recipe>(), object : RecipeListener {
                override fun OnClickItem(recipe: Recipe) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        resultByTagViewModel.getRecipeById(recipe.idMeal.toString())
                            .onSuccess { fullRecipe ->
                                showRecipePreviewDialog(fullRecipe)
                            }
                    }
                }

                override fun OnFavoriteClick(recipe: Recipe) {
                    resultByTagViewModel.toggleFavorite(recipe)
                }

                override fun onClickInf(recipe: Recipe) = Unit
            })
            adapter = mealSimpleAdapter
        }
    }

    fun getInstance() {
        val bundle = arguments
        area = bundle?.getString("area") ?:  ""
    }

    fun loadData() {
        binding.tvAreaName.text = getString(R.string.area_label, area)
        resultByTagViewModel.getRecipesByArea(area)
    }

    fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                resultByTagViewModel.recipeByArea.collect {
                    Log.d("Data area: ", it.size.toString())
                    mealSimpleAdapter.updateData(it)
                    listRecipe = it
                    binding.txtResultCount.text = getString(R.string.recipes_found_count, it.size)
                }
            }
        }
    }

    private fun showRecipePreviewDialog(recipe: Recipe) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recipe_preview, null)
        val imgPreview = dialogView.findViewById<ImageView>(R.id.imgRecipePreview)
        val tvName = dialogView.findViewById<TextView>(R.id.tvRecipeNamePreview)
        val tvMeta = dialogView.findViewById<TextView>(R.id.tvRecipeMetaPreview)
        val tvInstruction = dialogView.findViewById<TextView>(R.id.tvInstructionPreview)

        tvName.text = recipe.strMeal.ifEmpty { getString(R.string.data_not_found) }
        tvMeta.text = getString(
            R.string.dialog_recipe_preview_meta,
            recipe.strCategory.ifEmpty { "-" },
            recipe.strArea.ifEmpty { "-" }
        )
        tvInstruction.text = recipe.strInstructions
            .trim()
            .ifEmpty { getString(R.string.data_not_found) }
            .let { if (it.length > 240) "${it.take(237)}..." else it }

        Glide.with(this)
            .load(recipe.strMealThumb)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_cooking)
            .into(imgPreview)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.view_detail) { _, _ ->
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
            .show()
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