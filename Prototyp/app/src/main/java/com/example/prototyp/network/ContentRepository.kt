package com.example.prototyp.data.repository

import android.util.Log
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Verantwortlich für das Abrufen und Verwalten der Master-Kartendaten.
 */
class ContentRepository(private val masterCardDao: MasterCardDao) {

    /**
     * Ruft die neuesten Kartendaten von der Riot API ab, transformiert sie
     * und aktualisiert die lokale MasterCard-Datenbank.
     *
     * @param apiKey Dein persönlicher Riot API Key.
     */
    suspend fun updateMasterCardsFromApi(apiKey: String) {
        withContext(Dispatchers.IO) {
            try {
                // 1. API-Call durchführen
                val contentDto = ApiClient.riftboundContentApi.getContent(apiKey)

                // 2. Set-Informationen für schnellen Zugriff in eine Map umwandeln (z.B. "OGN" -> "Origins")
                val setMap = contentDto.sets.associateBy({ it.id }, { it.name })

                // 3. API-Kartendaten in das Format der lokalen MasterCard-Datenbank umwandeln
                val masterCards = contentDto.cards.mapNotNull { cardDto ->
                    val idParts = cardDto.id.split('-')
                    if (idParts.size != 2) return@mapNotNull null

                    val setCode = idParts[0]
                    val cardNumber = idParts[1].toIntOrNull() ?: return@mapNotNull null

                    MasterCard(
                        setCode = setCode,
                        cardNumber = cardNumber,
                        cardName = cardDto.name,
                        setName = setMap[cardDto.set] ?: "Unbekanntes Set",
                        color = mapColors(cardDto.colors)
                    )
                }

                // 4. Alte Daten löschen und neue Daten in die Datenbank einfügen
                if (masterCards.isNotEmpty()) {
                    // masterCardDao.clearAll() // Optional: Eine Funktion zum Leeren der Tabelle
                    masterCardDao.insertAll(masterCards)
                    Log.d("ContentRepository", "${masterCards.size} Karten erfolgreich aktualisiert.")
                }

            } catch (e: Exception) {
                // Fehlerbehandlung, falls der API-Call fehlschlägt
                Log.e("ContentRepository", "Fehler beim Aktualisieren der MasterCards.", e)
            }
        }
    }

    /**
     * Wandelt die Farbliste der API in den einzelnen Farb-Code der App um.
     * R, B, G, Y, P, O -> Einzelne Farben
     * M -> Mehrfarbig
     * U -> Universell/Grau
     */
    private fun mapColors(colors: List<String>?): String {
        if (colors.isNullOrEmpty()) return "U"
        return when (colors.size) {
            0 -> "U"
            1 -> when (colors.first().uppercase()) {
                "RED" -> "R"
                "BLUE" -> "B"
                "GREEN" -> "G"
                "YELLOW" -> "Y"
                "PURPLE" -> "P"
                "ORANGE" -> "O"
                else -> "U"
            }
            else -> "M" // Mehrfarbig
        }
    }
}