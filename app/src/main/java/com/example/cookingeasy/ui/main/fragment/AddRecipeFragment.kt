package com.example.cookingeasy.ui.main.fragment

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.data.remote.firebase.AuthDataSource
import com.example.cookingeasy.data.repository.AuthRepositoryImp
import com.example.cookingeasy.data.repository.RecipeUploadRepositoryImp
import com.example.cookingeasy.databinding.FragmentAddRecipeBinding
import com.example.cookingeasy.databinding.ItemIngredientInputBinding
import com.example.cookingeasy.databinding.ItemInstructionInputBinding
import com.example.cookingeasy.ui.main.viewmodel.AddRecipeViewModel
import com.example.cookingeasy.ui.main.viewmodel.AddRecipeViewModel.AddRecipeState
import com.example.cookingeasy.ui.viewmodelFactory.AddRecipeViewModelFactory
import kotlinx.coroutines.launch

class AddRecipeFragment : Fragment() {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!
    private var mealImg: String = ""

    private val viewModel: AddRecipeViewModel by viewModels {
        AddRecipeViewModelFactory(
            AuthRepositoryImp(),
            RecipeUploadRepositoryImp(requireContext().contentResolver)
        )
    }

    // ─── Image picker ────────────────────────────────────────────────
    private val imageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { setMealImage(it) }
    }

    // ─── Video picker ────────────────────────────────────────────────
    private val videoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { setVideoPreview(it) }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeState()
        observeIngredients()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── Setup ───────────────────────────────────────────────────────

    private fun setupClickListeners() {

        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.layoutPickImage.setOnClickListener {
            imageLauncher.launch("image/*")
        }

        binding.layoutPickVideo.setOnClickListener {
            videoLauncher.launch("video/*")
        }

        binding.btnRemoveVideo.setOnClickListener {
            viewModel.removeVideo()
            showVideoPlaceholder()
        }

        binding.btnAddIngredient.setOnClickListener {
            addIngredientRow()
        }

        binding.btnAddStep.setOnClickListener {
            addInstructionRow()
        }

        binding.btnSaveDraft.setOnClickListener {
            collectIngredients()
            viewModel.saveDraft(
                mealImg = mealImg,
                mealName     = binding.edtMealName.text.toString().trim(),
                category     = binding.edtCategory.text.toString().trim(),
                area         = binding.edtArea.text.toString().trim(),
                tags         = binding.edtTags.text.toString().trim(),
                youtubeLink  = binding.edtYoutube.text.toString().trim(),
                instructions = collectInstructions()
            )
        }

        binding.btnPublish.setOnClickListener {
            collectIngredients()
            viewModel.publish(
                mealImg = mealImg,
                mealName     = binding.edtMealName.text.toString().trim(),
                category     = binding.edtCategory.text.toString().trim(),
                area         = binding.edtArea.text.toString().trim(),
                tags         = binding.edtTags.text.toString().trim(),
                youtubeLink  = binding.edtYoutube.text.toString().trim(),
                instructions = collectInstructions()
            )
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is AddRecipeState.Idle       -> Unit
                    is AddRecipeState.Loading    -> showLoading(true)
                    is AddRecipeState.SavedDraft -> {
                        showLoading(false)
                        showMessage("Draft saved!")
                        viewModel.resetState()
                    }
                    is AddRecipeState.Published  -> {
                        showLoading(false)
                        showMessage("Recipe published!")
                        parentFragmentManager.popBackStack()
                    }
                    is AddRecipeState.Error      -> {
                        showLoading(false)
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun observeIngredients() {
        lifecycleScope.launch {
            viewModel.ingredientCount.collect { count ->
                binding.tvIngredientCount.text = count
            }
        }
    }

    // ─── Meal Image ──────────────────────────────────────────────────

    private fun setMealImage(uri: Uri) {
        viewModel.setMealImage(uri)
        mealImg = uriToBase64(uri)
        binding.layoutImagePlaceholder.visibility = View.GONE
        binding.imgMeal.visibility = View.VISIBLE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.imgMeal)
    }

    private fun uriToBase64(uri: Uri): String {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // ─── Video ───────────────────────────────────────────────────────

    private fun setVideoPreview(uri: Uri) {
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)
        viewModel.setVideoUri(uri, fileName, fileSize)

        binding.layoutVideoPlaceholder.visibility = View.GONE
        binding.imgVideoThumb.visibility          = View.VISIBLE
        binding.icPlayOverlay.visibility          = View.VISIBLE
        binding.layoutVideoInfo.visibility        = View.VISIBLE
        binding.tvVideoName.text = fileName
        binding.tvVideoSize.text = fileSize

        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.imgVideoThumb)
    }

    private fun showVideoPlaceholder() {
        binding.layoutVideoPlaceholder.visibility = View.VISIBLE
        binding.imgVideoThumb.visibility          = View.GONE
        binding.icPlayOverlay.visibility          = View.GONE
        binding.layoutVideoInfo.visibility        = View.GONE
    }

    // ─── Ingredients ─────────────────────────────────────────────────

    private fun addIngredientRow() {
        val rowBinding = ItemIngredientInputBinding.inflate(
            layoutInflater, binding.layoutIngredients, false
        )
        rowBinding.btnRemoveIngredient.setOnClickListener {
            binding.layoutIngredients.removeView(rowBinding.root)
            updateIngredientCountFromViews()
        }
        binding.layoutIngredients.addView(rowBinding.root)
        updateIngredientCountFromViews()
    }

    private fun collectIngredients() {
        val container = binding.layoutIngredients
        for (i in 0 until container.childCount) {
            val row     = container.getChildAt(i) ?: continue
            val name    = row.findViewById<EditText>(R.id.edtIngredientName)
                ?.text?.toString()?.trim() ?: continue
            val measure = row.findViewById<EditText>(R.id.edtIngredientMeasure)
                ?.text?.toString()?.trim() ?: ""
            if (name.isNotEmpty()) {
                viewModel.addIngredient(name, measure)
            }
        }
    }

    private fun updateIngredientCountFromViews() {
        val count = binding.layoutIngredients.childCount
        binding.tvIngredientCount.text = "$count ${if (count == 1) "item" else "items"}"
    }

    // ─── Instructions ─────────────────────────────────────────────────

    private fun addInstructionRow() {
        val rowBinding = ItemInstructionInputBinding.inflate(
            layoutInflater, binding.layoutInstructions, false
        )
        rowBinding.tvStepNumber.text = (binding.layoutInstructions.childCount + 1).toString()

        rowBinding.btnRemoveStep.setOnClickListener {
            binding.layoutInstructions.removeView(rowBinding.root)
            reorderStepNumbers()
            updateStepCount()
        }
        binding.layoutInstructions.addView(rowBinding.root)
        updateStepCount()
    }

    private fun collectInstructions(): String {
        val container = binding.layoutInstructions
        val steps = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val step = container.getChildAt(i)
                ?.findViewById<EditText>(R.id.edtInstructionStep)
                ?.text?.toString()?.trim() ?: continue
            if (step.isNotEmpty()) steps.add("${i + 1}. $step")
        }
        return steps.joinToString("\n\n")
    }

    private fun reorderStepNumbers() {
        val container = binding.layoutInstructions
        for (i in 0 until container.childCount) {
            container.getChildAt(i)
                ?.findViewById<TextView>(R.id.tvStepNumber)
                ?.text = (i + 1).toString()
        }
    }

    private fun updateStepCount() {
        val count = binding.layoutInstructions.childCount
        binding.tvStepCount.text = "$count ${if (count == 1) "step" else "steps"}"
    }

    // ─── File helpers ─────────────────────────────────────────────────

    private fun getFileName(uri: Uri): String {
        var name = "video"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
        }
        return name
    }

    private fun getFileSize(uri: Uri): String {
        var size = 0L
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && index >= 0) size = cursor.getLong(index)
        }
        return when {
            size >= 1_048_576 -> "${size / 1_048_576} MB"
            size >= 1_024     -> "${size / 1_024} KB"
            else              -> "$size B"
        }
    }

    // ─── UI Helpers ──────────────────────────────────────────────────

    private fun showLoading(isLoading: Boolean) {
        binding.btnPublish.isEnabled   = !isLoading
        binding.btnSaveDraft.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        //Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e("Upload error: ", message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}