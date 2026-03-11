package com.example.cookingeasy.ui.main.fragment

import android.annotation.SuppressLint
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.R
import com.example.cookingeasy.databinding.FragmentScanBinding
import com.example.cookingeasy.domain.model.ScanResult
import com.example.cookingeasy.ui.viewmodel.ScanUiState
import com.example.cookingeasy.ui.viewmodel.ScanViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ScanFragment : Fragment() {

    private val viewModel: ScanViewModel by viewModels()
    private lateinit var binding: FragmentScanBinding

    // Camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null

    // Gallery picker
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadBitmapAndScan(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentScanBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCamera()
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener { captureAndScan() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ScanUiState.Idle -> showIdle()
                    is ScanUiState.Loading -> showLoading()
                    is ScanUiState.Success -> showResult(state.result)
                    is ScanUiState.Error -> showError(state.message)
                }
            }
        }
    }

    // --- Camera ---
    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureAndScan() {
        val capture = imageCapture ?: return
        capture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    viewModel.scanImage(bitmap)
                }
                override fun onError(exception: ImageCaptureException) {
                    showError(exception.message ?: "Capture failed")
                }
            }
        )
    }

    private fun loadBitmapAndScan(uri: Uri) {
        val bitmap =
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
        viewModel.scanImage(bitmap)
    }

    // --- UI states ---
    private fun showIdle() {
        binding.progressBar.isVisible = false
        binding.tvDishName.isVisible = false
    }

    @SuppressLint("SetTextI18n")
    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.tvDishName.text = "Analyzing..."
        binding.tvDishName.isVisible = true
    }

    private fun showResult(result: ScanResult) {
        binding.progressBar.isVisible = false
        val ingredients: String = Gson().toJson(result.ingredients)
        val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()
        val resultScanFragment = ResultScanFragment()
        val bundle = Bundle()
        bundle.putString("ingredients", ingredients)
        resultScanFragment.arguments = bundle
        fragmentTransaction.replace(R.id.container, resultScanFragment)
        fragmentTransaction.commit()
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_SHORT).show()
    }
}