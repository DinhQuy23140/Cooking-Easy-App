package com.example.cookingeasy.ui.main.fragment

import InstructionAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.FragmentRecipeDetailBinding
import com.example.cookingeasy.domain.model.RecipeComment
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.main.activity.FullscreenVideoActivity
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.util.PlayerManager
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RecipeDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

@AndroidEntryPoint
class RecipeDetailFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var player: ExoPlayer? = null
    private lateinit var recipe: Recipe
    private var isFullScreen = false
    private var isSubmittingComment = false
    private lateinit var binding: FragmentRecipeDetailBinding
    private val recipeShareViewmodel: RecipeShareViewmodel by activityViewModels()
    private val viewModel: HomeViewModel by viewModels()

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
        binding = FragmentRecipeDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
        event()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RecipeDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            recipeShareViewmodel.selectRecipe.collect { data ->
                if (data == null) return@collect
                recipe = data
                loadData(recipe)
                viewModel.loadRecipeFeedback(recipe.idMeal)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipeRatingSummary.collect { summary ->
                binding.tvRatingSummary.text = getString(
                    R.string.recipe_rating_summary,
                    DecimalFormat("0.0").format(summary.average),
                    summary.total
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myRecipeRating.collect { myRating ->
                if (myRating > 0f) binding.ratingInput.rating = myRating
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipeComments.collect { comments ->
                renderComments(comments)
            }
        }
    }

    fun loadData(recipe: Recipe) {
        val thumb = recipe.strMealThumb?.trim().orEmpty()
        if (thumb.startsWith("http://", ignoreCase = true) || thumb.startsWith("https://", ignoreCase = true)) {
            Glide.with(requireActivity())
                .load(thumb)
                .placeholder(R.drawable.ic_cooking)
                .error(R.drawable.ic_delete)
                .into(binding.imgRecipe)
        } else if (thumb.isNotBlank()) {
            runCatching {
                val base64Payload = thumb.substringAfter("base64,", thumb)
                val decodedBytes = Base64.decode(base64Payload, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            }.onSuccess { bitmap ->
                if (bitmap != null) {
                    binding.imgRecipe.setImageBitmap(bitmap)
                } else {
                    binding.imgRecipe.setImageResource(R.drawable.ic_delete)
                }
            }.onFailure {
                binding.imgRecipe.setImageResource(R.drawable.ic_delete)
            }
        } else {
            binding.imgRecipe.setImageResource(R.drawable.ic_delete)
        }

        if (recipe.isFavorote) binding.btnFavorite.setImageResource(R.drawable.ic_heart_filled)
        else binding.btnFavorite.setImageResource(R.drawable.ic_heart_outline)

        binding.tvMealName.text = recipe.strMeal
        binding.tvCategory.text = recipe.strCategory
        binding.tvArea.text = recipe.strArea
        binding.tvIngredientCount.text = recipe.getIngredients().size.toString() + " ingredients"

        val layout = binding.layoutIngredients

        for (i in 1 .. 20) {
            val ingredient = recipe.getIngredient(i)
            val measure = recipe.getMeasure(i)
            if (!ingredient.isNullOrBlank()) {
                val view = layoutInflater.inflate(R.layout.item_ingredient, layout, false)
                val tvIngredient = view.findViewById<TextView>(R.id.tvIngredient)
                val tvMeasure = view.findViewById<TextView>(R.id.tvMeasure)
                tvIngredient.text = ingredient
                tvMeasure.text = measure

                layout.addView(view)
            }
        }

//        binding.tvInstructions.text = recipe.strInstructions
        setupInstructions(recipe)
        initializePlayer("https://wxvjcevcyelpobqleeti.supabase.co/storage/v1/object/public/document/2025-12-20%2021-17-00.mp4")
    }

    fun initializePlayer(videoUrl: String) {
        player = PlayerManager.getPlayer(requireContext())
        binding.playerView.player = player
        if (player?.mediaItemCount == 0) {
            val mediaItem: MediaItem = MediaItem.fromUri(videoUrl)
            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = false
            }
        }
    }

    fun event() {
        binding.btnFullscreen.setOnClickListener {
            val intent: Intent = Intent(requireContext(), FullscreenVideoActivity::class.java)
            startActivity(intent)
        }

        binding.btnYoutube.setOnClickListener {
            openYoutobeVideo(requireContext(), recipe?.strYoutube ?: "")
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite(recipe)
        }
        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingInput.rating
            viewModel.submitRecipeRating(recipe.idMeal, rating) { success ->
                if (!isAdded) return@submitRecipeRating
                Toast.makeText(
                    requireContext(),
                    if (success) getString(R.string.recipe_rating_saved) else getString(R.string.error_title),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.btnSubmitComment.setOnClickListener {
            if (isSubmittingComment) return@setOnClickListener
            val content = binding.edtComment.text?.toString().orEmpty()
            if (content.isBlank()) return@setOnClickListener
            isSubmittingComment = true
            binding.btnSubmitComment.isEnabled = false
            viewModel.submitRecipeComment(recipe.idMeal, content) { success ->
                isSubmittingComment = false
                if (!isAdded) return@submitRecipeComment
                binding.btnSubmitComment.isEnabled = true
                if (success) {
                    binding.edtComment.setText("")
                }
                Toast.makeText(
                    requireContext(),
                    if (success) getString(R.string.recipe_comment_saved) else getString(R.string.error_title),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        binding.playerView.player = null
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.player = player
    }

    fun openYoutobeVideo(context: Context, url: String) {
        if (!url.isEmpty()) {
            val videoId = getYoutobeVideoId(url)
            try {
                val intent: Intent = Intent(Intent.ACTION_VIEW, "vnd.youtube:$videoId".toUri())
                intent.setPackage("com.google.android.youtube")
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val intent: Intent = Intent(
                    Intent.ACTION_VIEW, url.toUri()
                )
                context.startActivity(intent)
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.url_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    fun getYoutobeVideoId(url:String): String? {
        val uri = Uri.parse(url)
        return when {
            uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
            uri.host?.contains("youtube.com") == true -> uri.getQueryParameter("v")
            else -> null
        }
    }

    private fun setupInstructions(recipe: Recipe) {

        val steps = recipe.parseInstructions()

        val adapter = InstructionAdapter(steps)

        binding.rvInstructions.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvInstructions.adapter = adapter
    }

    private fun renderComments(comments: List<RecipeComment>) {
        val container = binding.layoutComments
        container.removeAllViews()
        if (comments.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = getString(R.string.recipe_no_comments)
                textSize = 13f
                setTextColor(resources.getColor(R.color.textSecondary, null))
            }
            container.addView(emptyView)
            return
        }
        comments.take(10).forEach { comment ->
            val item = layoutInflater.inflate(R.layout.item_recipe_comment, container, false)
            val imgAvatar = item.findViewById<ImageView>(R.id.imgCommentAvatar)
            val tvUserName = item.findViewById<TextView>(R.id.tvCommentUserName)
            val tvNick = item.findViewById<TextView>(R.id.tvCommentNick)
            val tvTime = item.findViewById<TextView>(R.id.tvCommentTime)
            val tvContent = item.findViewById<TextView>(R.id.tvCommentContent)

            val displayName = comment.userName.ifEmpty { "User" }
            tvUserName.text = displayName
            tvNick.text = comment.userNickname
                .takeIf { it.isNotBlank() }
                ?.let { "@$it" }
                ?: "@unknown"
            tvTime.text = formatCommentTime(comment.createdAt)
            tvContent.text = comment.content

            Glide.with(this)
                .load(comment.userAvatarUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar)

            val openProfile = {
                if (comment.userId.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.other_user_profile_unavailable,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                        .replace(R.id.container, OtherUserProfileFragment.newInstance(comment.userId))
                        .addToBackStack(null)
                        .commit()
                }
            }
            imgAvatar.setOnClickListener { openProfile() }
            tvUserName.setOnClickListener { openProfile() }
            tvNick.setOnClickListener { openProfile() }

            container.addView(item)
        }
    }

    private fun formatCommentTime(createdAt: Long): String {
        if (createdAt <= 0L) return ""
        return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(createdAt))
    }
}