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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.R
import com.example.cookingeasy.common.adapter.HistorySearchAdapter
import com.example.cookingeasy.common.adapter.RecipeAdapter
import com.example.cookingeasy.common.listener.HistorySearchListener
import com.example.cookingeasy.common.listener.RecipeListener
import com.example.cookingeasy.databinding.FragmentSearchBinding
import com.example.cookingeasy.domain.model.HistorySearch
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.ui.viewmodel.SearchViewModel
import com.example.cookingeasy.util.GridSpacingItemDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private val recipeShare: RecipeShareViewmodel by activityViewModels()
    private lateinit var recipeAdapter: RecipeAdapter
    private var isLoadingMore = false
    private var searchJob: Job? = null
    private lateinit var historySearchAdapter: HistorySearchAdapter

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
        viewModel.getListHistory()
        setupEvents()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(mutableListOf(), object : RecipeListener {
            override fun OnClickItem(recipe: Recipe) {
                recipeShare.selectedRecipe(recipe)
                openRecipeDetail()
            }
            override fun OnFavoriteClick(recipe: Recipe) {
                viewModel.toggleFavorite(recipe)
            }

            override fun onClickInf(recipe: Recipe) {
                parentFragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.container, OtherUserProfileFragment())
                    .commit()
            }
        })

        historySearchAdapter = HistorySearchAdapter(mutableListOf(), object : HistorySearchListener{
            override fun onClick(historySearch: HistorySearch) {
                binding.edtSearchRecipe.setText(historySearch.keyword)
                viewModel.searchRecipes(historySearch.keyword)
            }

        })

        binding.rvSearchResult.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = recipeAdapter
            addItemDecoration(GridSpacingItemDecoration(2, 3))
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }

        binding.rvRecentSearch.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = historySearchAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        viewModel.searchResult,
                        viewModel.isLoading
                    ) { results, loading -> results to loading }
                        .collect { (results, loading) ->
                            renderSearchContent(results, loading)
                        }
                }

                launch {
                    viewModel.historyList.collect { history ->
                        historySearchAdapter.updateData(history.toMutableList())
                        if (!hasSearchKeyword()) {
                            applyIdleVisibility()
                        }
                    }
                }
            }
        }
    }
    private fun renderSearchContent(results: List<Recipe>, loading: Boolean) {
        val hasKeyword = hasSearchKeyword()
        if (!hasKeyword) {
            applyIdleVisibility()
            recipeAdapter.updateData(results)
            return
        }

        binding.layoutLoading.isVisible = loading
        hideRecentSection()
        binding.layoutInitial.isVisible = false

        if (loading) {
            binding.layoutResult.isVisible = false
            binding.layoutEmpty.isVisible = false
            return
        }

        binding.layoutResult.isVisible = results.isNotEmpty()
        binding.layoutEmpty.isVisible = results.isEmpty()
        binding.txtResult.text = getString(R.string.format_results_found, results.size)
        recipeAdapter.updateData(results)
    }

    private fun hasSearchKeyword(): Boolean =
        binding.edtSearchRecipe.text?.trim()?.isNotEmpty() == true

    private fun applyIdleVisibility() {
        val hasHistory = viewModel.historySnapshot().isNotEmpty()
        binding.layoutRecent.isVisible = hasHistory
        binding.rvRecentSearch.isVisible = hasHistory
        binding.layoutInitial.isVisible = !hasHistory
        binding.layoutResult.isVisible = false
        binding.layoutEmpty.isVisible = false
        binding.layoutLoading.isVisible = false
    }

    private fun hideRecentSection() {
        binding.layoutRecent.isVisible = false
        binding.rvRecentSearch.isVisible = false
    }

    private fun openRecipeDetail() {
        if (!isAdded) return
        val fm = parentFragmentManager
        if (fm.isStateSaved) return
        fm.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.container, RecipeDetailFragment())
            .addToBackStack(null)
            .commit()
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

                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(DEBOUNCE_MS)
                    hideRecentSection()
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
        binding.btnClear.isVisible = false
        viewModel.searchRecipes("")
    }

    companion object {
        private const val DEBOUNCE_MS = 300L

        fun newInstance() = SearchFragment()
    }
}
