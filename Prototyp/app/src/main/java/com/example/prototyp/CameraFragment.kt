package com.example.prototyp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.prototyp.AppDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

// --- Konfiguration ---
private const val STRICT_MASTER_MATCH = false   // später true setzen
private const val CENTER_TOP_RATIO = 0.30f
private const val CENTER_BOTTOM_RATIO = 0.70f
private const val EMIT_INTERVAL_MS = 700L
private const val FALLBACK_CONFIRM_MS = 1500L   // nach 1.5s darf fallback feuern

// State
private var lastEmitTime = 0L
private var lastAccepted: String? = null
private var firstSeenNonMasterAt: Long? = null

class CameraFragment : Fragment(R.layout.fragment_scan) {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    // ML Kit OCR
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Master-Namen (normalisiert, lowercased)
    private val masterNames: MutableSet<String> = HashSet()

    // Bestätigung & Debounce
    private var lastEmitTime = 0L
    private val emitIntervalMs = 700L
    private var lastAccepted: String? = null

    // Koroutinen für DB-Load
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 1) Masterliste laden (einmalig)
        ioScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext().applicationContext)
                val masterDao = db.masterCardDao()
                val all = masterDao.search("") // LIKE '%%' → alle Masterkarten
                masterNames.clear()
                all.forEach { mc ->
                    masterNames.add(normalize(mc.cardName))
                }
            } catch (_: Throwable) {
                // zur Not weiter ohne Filter (aber wir wollen ja strenger sein)
            }
        }

        // 2) Kamera starten
        ensureCameraPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }

    private fun ensureCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else parentFragmentManager.popBackStack()
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor, ::processFrame)
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ---------------- OCR / Auswahl-Logik ----------------

    @OptIn(ExperimentalGetImage::class)
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close(); return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val frameHeight = imageProxy.height.toFloat()

        recognizer.process(image)
            .addOnSuccessListener { vt ->
                val frameH = imageProxy.height.toFloat()
                val candidate = pickBestCenterLine(vt, frameH) ?: return@addOnSuccessListener

                val normalized = normalize(candidate)

                val now = System.currentTimeMillis()
                val inMaster = normalized in masterNames

                // 1) Streng: nur Master
                if (inMaster || STRICT_MASTER_MATCH) {
                    firstSeenNonMasterAt = null
                    maybePublish(normalized, now)
                    return@addOnSuccessListener
                }

                // 2) Debug/Fallback: nicht in Master
                if (firstSeenNonMasterAt == null) {
                    firstSeenNonMasterAt = now
                }
                // gleiche Zeile zweimal + genügend Zeit → akzeptieren
                if (lastAccepted == normalized && (now - (firstSeenNonMasterAt ?: now)) >= FALLBACK_CONFIRM_MS) {
                    maybePublish(normalized, now)
                } else {
                    lastAccepted = normalized
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    /**
     * Wählt eine Zeile, deren BoundingBox-Mittelpunkt im zentralen Band liegt:
     * Vertikal: 35% .. 65% der Bildhöhe (Center 30% Band).
     * Bevorzugt längere Einträge mit Buchstaben.
     */
    private fun pickBestCenterLine(vt: com.google.mlkit.vision.text.Text, frameHeight: Float): String? {
        val top = frameHeight * CENTER_TOP_RATIO   // 0.30
        val bottom = frameHeight * CENTER_BOTTOM_RATIO // 0.70

        return vt.textBlocks.asSequence()
            .flatMap { it.lines.asSequence() }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                val cy = box.centerY().toFloat()
                if (cy !in top..bottom) return@mapNotNull null

                val raw = line.text?.trim() ?: return@mapNotNull null
                val txt = raw.replace("\\s+".toRegex(), " ")
                if (txt.length !in 3..48) return@mapNotNull null
                if (!txt.any { it.isLetter() }) return@mapNotNull null
                // Score: Länge + Nähe zur Mitte
                val centerDist = kotlin.math.abs(cy - (frameHeight / 2f))
                val score = txt.length + ((1000f - centerDist).coerceAtLeast(0f) / 1000f)
                txt to score
            }
            .maxByOrNull { it.second }
            ?.first
    }

    /** Doppel-Bestätigung + Debounce */
    private fun maybePublish(normalizedName: String, now: Long) {
        if (now - lastEmitTime < EMIT_INTERVAL_MS) return
        lastEmitTime = now
        publishResult(normalizedName)
        lastAccepted = null
        firstSeenNonMasterAt = null
    }

    // ---------------- Utils ----------------

    private fun normalize(s: String): String =
        s.lowercase()
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('“', '"')
            .replace('”', '"')
            .replace("\\s+".toRegex(), " ")
            .trim()

    private fun publishResult(name: String) {
        val finalName = name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
        }

        parentFragmentManager.setFragmentResult("scan_result", bundleOf("card_name" to finalName))
        parentFragmentManager.popBackStack()
    }
}
