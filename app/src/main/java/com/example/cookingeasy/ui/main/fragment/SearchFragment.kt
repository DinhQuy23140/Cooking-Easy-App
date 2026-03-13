package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentSearchBinding
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.SearchViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var recipeAdapter: RecipeAdapter
    private var isLoadingMore = false
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupEvents()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(mutableListOf(), object : RecipeListener {
            override fun OnClickItem(recipe: Recipe) {
                // navigate to detail
            }
            override fun OnFavoriteClick(boolean: Boolean) {
                // handle favorite
            }
        })

        binding.rvSearchResult.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = recipeAdapter
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResult.collect { result ->
                        val hasKeyword = binding.edtSearchRecipe.text?.isNotEmpty() == true
                        binding.layoutLoading.isVisible = false
                        binding.layoutInitial.isVisible = !hasKeyword
                        binding.layoutResult.isVisible = result.isNotEmpty()
                        binding.layoutEmpty.isVisible = result.isEmpty() && hasKeyword
                        binding.txtResult.text = "${result.size} found"
                        if (result.isNotEmpty()) recipeAdapter.updateData(result)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.layoutLoading.isVisible = true
                            binding.layoutResult.isVisible = false
                            binding.layoutEmpty.isVisible = false
                            binding.layoutInitial.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun setupEvents() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnClear.setOnClickListener {
            binding.edtSearchRecipe.setText("")
            resetToInitialState()
        }

        binding.ivScan.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations( // ← set TRƯỚC replace
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.container, ScanFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.edtSearchRecipe.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim()
                binding.btnClear.isVisible = keyword.isNotEmpty()

                if (keyword.isEmpty()) {
                    resetToInitialState()
                    return
                }

                // Debounce 300ms — chờ user ngừng gõ mới gọi API
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    binding.layoutLoading.isVisible = true
                    binding.layoutInitial.isVisible = false
                    binding.layoutEmpty.isVisible = false
                    viewModel.searchRecipes(keyword)
                }
            }
        })

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

    private fun resetToInitialState() {
        searchJob?.cancel()
        binding.layoutInitial.isVisible = true
        binding.layoutResult.isVisible = false
        binding.layoutLoading.isVisible = false
        binding.layoutEmpty.isVisible = false
        binding.btnClear.isVisible = false
    }

    companion object {
        fun newInstance() = SearchFragment()
    }
}
