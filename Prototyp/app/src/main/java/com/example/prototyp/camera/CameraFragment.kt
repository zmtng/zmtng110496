package com.example.prototyp.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.prototyp.databinding.FragmentScanBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.prototyp.AppDatabase

class CameraFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private val cameraViewModel: CameraViewModel by activityViewModels()

    private lateinit var cameraExecutor: ExecutorService
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private val masterNames = mutableSetOf<String>()
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }
    private var lastEmitTime = 0L
    private val emitIntervalMs = 700L
    // ---

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Kamera-Berechtigung wird benötigt.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        loadMasterNames()
        ensureCameraPermission()
    }

    private fun loadMasterNames() {
        ioScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext().applicationContext)
                val allCards = db.masterCardDao().search("", "", "")
                masterNames.clear()
                allCards.forEach { mc ->
                    masterNames.add(normalize(mc.cardName))
                }
                Log.d("CameraFragment", "${masterNames.size} Master-Namen geladen.")
            } catch (e: Exception) {
                Log.e("CameraFragment", "Fehler beim Laden der Master-Namen", e)
            }
        }
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor, ::processFrame)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("CameraFragment", "Kamera konnte nicht gebunden werden", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val frameHeight = imageProxy.height.toFloat()

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val candidate = pickBestCenterLine(visionText, frameHeight) ?: return@addOnSuccessListener
                val normalized = normalize(candidate)

                if (masterNames.isNotEmpty() && normalized in masterNames) {
                    val now = System.currentTimeMillis()
                    if (now - lastEmitTime >= emitIntervalMs) {
                        lastEmitTime = now
                        publishResult(candidate)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CameraFragment", "Texterkennung fehlgeschlagen.", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun pickBestCenterLine(visionText: com.google.mlkit.vision.text.Text, frameHeight: Float): String? {
        val top = frameHeight * 0.30f
        val bottom = frameHeight * 0.70f

        return visionText.textBlocks.asSequence()
            .flatMap { it.lines.asSequence() }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                val centerY = box.centerY().toFloat()
                if (centerY !in top..bottom) return@mapNotNull null

                val text = line.text.replace("\\s+".toRegex(), " ").trim()
                if (text.length < 3 || !text.any { it.isLetter() }) return@mapNotNull null

                text to text.length
            }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace("\\s+".toRegex(), " ")
            .trim()

    private fun publishResult(name: String) {
        cameraViewModel.setScannedText(name)

        activity?.runOnUiThread {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}