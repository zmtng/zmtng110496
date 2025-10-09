package com.example.prototyp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
import java.io.File

class HomeViewModel(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao
    ) : ViewModel() {

    private val _totalCollectionValue = MutableStateFlow<Double?>(null)
    val totalCollectionValue = _totalCollectionValue.asStateFlow()

    // Ein Flow, um dem Fragment Nachrichten zu senden (z.B. "Export erfolgreich")
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    fun updateTotalValue() {
        viewModelScope.launch(Dispatchers.IO) {
            val allItems = cardDao.getCollectionForExport()
            val totalValue = allItems.sumOf { entry ->
                (entry.price ?: 0.0) * entry.quantity
            }
            _totalCollectionValue.value = totalValue
        }
    }

    /**
     * Exportiert die Sammlung in eine vom Nutzer gewählte Datei (Uri).
     */
    /*fun exportCollection(uri: Uri, context: Context) {
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
    }*/

    /**
     * Importiert eine Sammlung aus einer vom Nutzer gewählten CSV-Datei (Uri).
     */
    fun importCollection(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val validatedEntries = mutableListOf<CollectionEntry>()
                var notFoundCount = 0

                // CSV-Daten zuerst einlesen
                val rows = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().toList()
                    }
                } ?: emptyList()

                // Jetzt die Liste innerhalb der Coroutine verarbeiten
                rows.forEach { row ->
                    val setCode = row["setCode"]
                    val cardNumber = row["cardNumber"]?.toIntOrNull()

                    if (setCode != null && cardNumber != null) {
                        // Suspend-Funktion kann jetzt aufgerufen werden
                        val masterCard = masterDao.getBySetAndNumber(setCode, cardNumber)

                        if (masterCard != null) {
                            val entry = CollectionEntry(
                                setCode = setCode,
                                cardNumber = cardNumber,
                                quantity = row["quantity"]?.toIntOrNull() ?: 1,
                                price = row["price"]?.toDoubleOrNull(),
                                color = masterCard.color, // Korrekte Farbe von der DB
                                personalNotes = row["personalNotes"],
                                generalNotes = row["generalNotes"]
                            )
                            validatedEntries.add(entry)
                        } else {
                            notFoundCount++
                            Log.w("Import", "Karte nicht in Master-DB gefunden: Set=$setCode, Nummer=$cardNumber")
                        }
                    }
                }

                if (validatedEntries.isNotEmpty()) {
                    cardDao.overrideCollection(validatedEntries)
                    var message = "${validatedEntries.size} Karten erfolgreich importiert!"
                    if (notFoundCount > 0) {
                        message += " ($notFoundCount Karten nicht in der DB gefunden und übersprungen.)"
                    }
                    _userMessage.value = message
                } else {
                    _userMessage.value = "Import fehlgeschlagen: Keine gültigen Karten in der CSV-Datei gefunden."
                }
            } catch (e: Exception) {
                _userMessage.value = "Import fehlgeschlagen: ${e.message}"
                Log.e("Import", "Fehler beim Import", e)
            }
        }
    }

    suspend fun createCollectionCsvForSharing(context: Context): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Daten aus der DB holen
                val collection = cardDao.getCollectionForExport()
                if (collection.isEmpty()) {
                    _userMessage.value = "Sammlung ist leer. Nichts zu exportieren."
                    return@withContext null
                }

                // 2. Temporäre Datei im Cache-Verzeichnis erstellen
                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                val file = File(exportDir, "sammlung_export.csv")

                // 3. CSV-Daten in die Datei schreiben
                file.outputStream().use { outputStream ->
                    csvWriter().open(outputStream) {
                        writeRow("setCode", "cardNumber", "quantity", "price", "color", "personalNotes", "generalNotes")
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

                // 4. Eine sichere Uri über den FileProvider für die Datei holen
                return@withContext FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

            } catch (e: Exception) {
                _userMessage.value = "Export fehlgeschlagen: ${e.message}"
                Log.e("Export", "Fehler beim Erstellen der CSV", e)
                return@withContext null
            }
        }
    }
}