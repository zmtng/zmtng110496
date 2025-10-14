package com.example.prototyp.gametools

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val STARTING_LIFE = 0

class LifeCounterViewModel : ViewModel() {

    private val _p1Life = MutableStateFlow(STARTING_LIFE)
    val p1Life = _p1Life.asStateFlow()

    private val _p2Life = MutableStateFlow(STARTING_LIFE)
    val p2Life = _p2Life.asStateFlow()

    private val _p1MightLeft = MutableStateFlow(0)
    val p1MightLeft = _p1MightLeft.asStateFlow()

    private val _p1MightRight = MutableStateFlow(0)
    val p1MightRight = _p1MightRight.asStateFlow()

    private val _p2MightLeft = MutableStateFlow(0)
    val p2MightLeft = _p2MightLeft.asStateFlow()

    private val _p2MightRight = MutableStateFlow(0)
    val p2MightRight = _p2MightRight.asStateFlow()

    // Funktionen zur Lebenspunkte-Änderung
    fun incrementP1Life() { _p1Life.value++ }
    fun decrementP1Life() { _p1Life.value-- }
    fun incrementP2Life() { _p2Life.value++ }
    fun decrementP2Life() { _p2Life.value-- }

    // Funktionen zur Might-Änderung
    fun incrementP1MightLeft() { _p1MightLeft.value++ }
    fun decrementP1MightLeft() { if (_p1MightLeft.value > 0) _p1MightLeft.value-- }
    fun incrementP1MightRight() { _p1MightRight.value++ }
    fun decrementP1MightRight() { if (_p1MightRight.value > 0) _p1MightRight.value-- }
    fun incrementP2MightLeft() { _p2MightLeft.value++ }
    fun decrementP2MightLeft() { if (_p2MightLeft.value > 0) _p2MightLeft.value-- }
    fun incrementP2MightRight() { _p2MightRight.value++ }
    fun decrementP2MightRight() { if (_p2MightRight.value > 0) _p2MightRight.value-- }

    // Reset-Funktion
    fun reset() {
        _p1Life.value = STARTING_LIFE
        _p2Life.value = STARTING_LIFE
        _p1MightLeft.value = 0
        _p1MightRight.value = 0
        _p2MightLeft.value = 0
        _p2MightRight.value = 0
    }
}