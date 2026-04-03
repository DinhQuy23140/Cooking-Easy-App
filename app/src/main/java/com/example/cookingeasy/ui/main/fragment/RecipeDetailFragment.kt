package com.example.cookingeasy.ui.main.fragment

import InstructionAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.cookingeasy.domain.model.Recipe
import com.example.cookingeasy.ui.main.activity.FullscreenVideoActivity
import com.example.cookingeasy.ui.viewmodel.RecipeShareViewmodel
import com.example.cookingeasy.util.PlayerManager
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookingeasy.ui.viewmodel.HomeViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RecipeDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecipeDetailFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var player: ExoPlayer? = null
    private lateinit var recipe: Recipe
    private var isFullScreen = false
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
        // Inflate the layout for this fragment
        binding = FragmentRecipeDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
        event()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RecipeDetailFragment.
         */
        // TODO: Rename and change types and number of parameters
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
                if (data != null) {
                    recipe = data
                }
                loadData(recipe)
            }
        }
    }

    fun loadData(recipe: Recipe) {
        Glide.with(requireActivity())
            .load(recipe.strMealThumb)
            .placeholder(R.drawable.ic_cooking)
            .error(R.drawable.ic_delete)
            .into(binding.imgRecipe)

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
            Toast.makeText(requireContext(), "Url not found", Toast.LENGTH_SHORT).show()
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
}