package com.example.prototyp

import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment

// Repräsentiert eine einzelne Kachel auf dem Home-Dashboard
data class DashboardItem(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    // Diese Zeile ändern: Das Fragezeichen macht die Destination optional.
    val destination: Class<out Fragment>?
)