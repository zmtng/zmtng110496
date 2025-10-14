package com.example.prototyp.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Repräsentiert die Top-Level-Antwort der content-API.
 */
@JsonClass(generateAdapter = true)
data class ContentDto(
    @Json(name = "cards") val cards: List<CardDto> = emptyList(),
    @Json(name = "sets") val sets: List<SetDto> = emptyList()
    // Weitere Felder wie keywords, factions etc. könnten hier hinzugefügt werden.
)

/**
 * Repräsentiert eine einzelne Karte aus der API.
 */
@JsonClass(generateAdapter = true)
data class CardDto(
    @Json(name = "id") val id: String, // z.B. "OGN-001"
    @Json(name = "name") val name: String,
    @Json(name = "set") val set: String, // z.B. "OGN"
    @Json(name = "colors") val colors: List<String>?
)

/**
 * Repräsentiert ein Kartenset aus der API.
 */
@JsonClass(generateAdapter = true)
data class SetDto(
    @Json(name = "id") val id: String, // z.B. "OGN"
    @Json(name = "name") val name: String
)