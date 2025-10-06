package com.example.prototyp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.CollectionEntry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val cardDao: CardDao) : ViewModel() {

    // Ein Flow, um dem Fragment Nachrichten zu senden (z.B. "Export erfolgreich")
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    /**
     * Exportiert die Sammlung in eine vom Nutzer gewählte Datei (Uri).
     */
    fun exportCollection(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Daten aus der DB holen
                val collection = cardDao.getCollectionForExport()

                // 2. CSV-Daten erstellen und in die Datei schreiben
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    csvWriter().open(outputStream) {
                        // Kopfzeile schreiben
                        writeRow("setCode", "cardNumber", "quantity", "price", "color", "personalNotes", "generalNotes")
                        // Datenzeilen schreiben
                        collection.forEach { entry ->
                            writeRow(
                                entry.setCode,
                                entry.cardNumber,
                                entry.quantity,
                                entry.price,
                                entry.color,
                                entry.personalNotes,
                                entry.generalNotes
                            )
                        }
                    }
                }
                _userMessage.value = "Export erfolgreich!"
            } catch (e: Exception) {
                _userMessage.value = "Export fehlgeschlagen: ${e.message}"
            }
        }
    }

    /**
     * Importiert eine Sammlung aus einer vom Nutzer gewählten CSV-Datei (Uri).
     */
    fun importCollection(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newEntries = mutableListOf<CollectionEntry>()
                // 1. CSV-Datei lesen und parsen
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                            val entry = CollectionEntry(
                                setCode = row["setCode"]!!,
                                cardNumber = row["cardNumber"]!!.toInt(),
                                quantity = row["quantity"]!!.toInt(),
                                price = row["price"]?.toDoubleOrNull(),
                                color = row["color"]!!,
                                personalNotes = row["personalNotes"],
                                generalNotes = row["generalNotes"]
                            )
                            newEntries.add(entry)
                        }
                    }
                }

                // 2. Datenbank in einer Transaktion überschreiben
                if (newEntries.isNotEmpty()) {
                    cardDao.overrideCollection(newEntries)
                    _userMessage.value = "${newEntries.size} Karten erfolgreich importiert!"
                } else {
                    _userMessage.value = "Import fehlgeschlagen: CSV-Datei ist leer oder ungültig."
                }
            } catch (e: Exception) {
                _userMessage.value = "Import fehlgeschlagen: ${e.message}"
            }
        }
    }
}