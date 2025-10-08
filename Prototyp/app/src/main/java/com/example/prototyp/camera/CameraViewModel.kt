package com.example.prototyp.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {

    private val _scannedText = MutableStateFlow<String?>(null)
    val scannedText = _scannedText.asStateFlow()

    fun setScannedText(text: String) {
        _scannedText.value = text
    }

    fun consumeScannedText() {
        _scannedText.value = null
    }
}