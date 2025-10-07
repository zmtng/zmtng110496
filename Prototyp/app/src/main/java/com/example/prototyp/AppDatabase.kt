// AppDatabase.kt
// package ggf. anpassen:
package com.example.yourapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.prototyp.CollectionEntry
import com.example.prototyp.CardSet
import com.example.prototyp.MasterCard
import com.example.prototyp.MasterCardDao
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.prototyp.deckBuilder.Deck
import com.example.prototyp.deckBuilder.DeckCard
import com.example.prototyp.deckBuilder.DeckDao
import com.example.prototyp.wishlist.WishlistDao
import com.example.prototyp.wishlist.WishlistEntry


@Database(
    entities = [
        CollectionEntry::class,  // ← bleibt
        MasterCard::class,       // falls genutzt
        CardSet::class,         // falls genutzt
        Deck::class,
        DeckCard::class,
        WishlistEntry::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun masterCardDao(): MasterCardDao

    abstract fun deckDao(): DeckDao
    abstract fun wishlistDao(): WishlistDao


    companion object {


        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "riftbound.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_5_6, MIGRATION_7_8, MIGRATION_8_9)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate direkt nach Neuerstellung
                        val appDb = getInstance(context)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val items = loadCardsFromAssets(context, "master_cards.csv")
                                if (items.isNotEmpty()) {
                                    appDb.masterCardDao().insertAll(items)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Fallback: Wenn master_cards leer ist (z.B. nach Reinstall oder wenn onCreate schiefging), erneut importieren
                        val appDb = getInstance(context)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val count = appDb.masterCardDao().count()
                                if (count == 0) {
                                    val items = loadCardsFromAssets(context, "master_cards.csv")
                                    if (items.isNotEmpty()) {
                                        appDb.masterCardDao().insertAll(items)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                })

                .fallbackToDestructiveMigration(false)
                .build()
        }


        /**
         * Migration von Version 1 (nur Card-Tabelle) auf Version 2 (fügt MasterCard-Tabelle hinzu).
         * Passt die Namen/Spalten bei Bedarf an, falls du deine Card-Tabelle anders definiert hast.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                /*db.execSQL("ALTER TABLE master_cards ADD COLUMN color TEXT NOT NULL DEFAULT 'R'")
                db.execSQL("ALTER TABLE collection  ADD COLUMN color TEXT NOT NULL DEFAULT 'R'")*/
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE collection ADD COLUMN personalNotes TEXT")
                database.execSQL("ALTER TABLE collection ADD COLUMN generalNotes TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Erstellt die 'decks'-Tabelle
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `decks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL
                    )
                """)
                // Erstellt die 'deck_cards'-Tabelle
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deck_cards` (
                        `deckId` INTEGER NOT NULL,
                        `setCode` TEXT NOT NULL,
                        `cardNumber` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        PRIMARY KEY(`deckId`, `setCode`, `cardNumber`),
                        FOREIGN KEY(`deckId`) REFERENCES `decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                CREATE TABLE IF NOT EXISTS `wishlist` (
                    `setCode` TEXT NOT NULL,
                    `cardNumber` INTEGER NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    PRIMARY KEY(`setCode`, `cardNumber`)
                )
            """)
            }
        }

        /**
         * CSV-Lader für assets/master_cards.csv
         *
         * Erwartetes Header-Format (case-insensitive):
         * set_code,set_name,card_number,card_name
         *
         * Falls deine CSV andere Spaltennamen nutzt: unten die Keys im Zugriff anpassen.
         */
        private fun loadCardsFromAssets(context: Context, assetName: String): List<MasterCard> {
            context.assets.open(assetName).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                    fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val out = mutableListOf<MasterCard>()

                    val headerLine = br.readLine() ?: return emptyList()

                    // Trennzeichen automatisch erkennen (Komma vs. Semikolon)
                    val delimiter = if (headerLine.count { it == ';' } > headerLine.count { it == ',' }) ';' else ','

                    // CSV-Zeile mit Quote-Unterstützung splitten
                    fun splitCsv(line: String): List<String> {
                        val res = mutableListOf<String>()
                        val sb = StringBuilder()
                        var inQuotes = false
                        var i = 0
                        while (i < line.length) {
                            val c = line[i]
                            when {
                                c == '"' -> {
                                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                                    else inQuotes = !inQuotes
                                }
                                c == delimiter && !inQuotes -> { res.add(sb.toString()); sb.setLength(0) }
                                else -> sb.append(c)
                            }
                            i++
                        }
                        res.add(sb.toString())
                        return res
                    }

                    val headerCols = splitCsv(headerLine)
                    val headerNorm = headerCols.map { normalize(it) }

                    // Hilfsfunktion: spaltenindex per Synonym finden
                    fun idx(vararg synonyms: String): Int {
                        val wanted = synonyms.map(::normalize)
                        return headerNorm.indexOfFirst { hn -> wanted.any { it == hn } }
                    }

                    // Tolerante Zuordnung der Pflichtfelder
                    val iSetCode   = idx("set_code","setcode","code","set")
                    val iSetName   = idx("set_name","setname","name","settitle","setlabel")
                    val iCardNo    = idx("card_number","cardnumber","number","no","nr")
                    val iCardName  = idx("card_name","cardname","name","cardtitle","title")
                    val iColor     = idx("color","colour")

                    if (iSetCode < 0 || iSetName < 0 || iCardNo < 0 || iCardName < 0) {
                        // Header passt nicht – lieber leer zurück statt crashen
                        return emptyList()
                    }

                    br.lineSequence().forEach { raw ->
                        if (raw.isBlank()) return@forEach
                        val cols = splitCsv(raw)
                        fun get(i: Int) = cols.getOrNull(i)?.trim().orEmpty()

                        val setCode   = get(iSetCode)
                        val setName   = get(iSetName)
                        val numberStr = get(iCardNo)
                        val cardName  = get(iCardName)
                        val cardNo    = numberStr.toIntOrNull() ?: return@forEach
                        val color     = get(iColor).ifBlank { "U" } // optional mit Default

                        if (setCode.isBlank() || setName.isBlank() || cardName.isBlank()) return@forEach

                        out += MasterCard(
                            setCode   = setCode,
                            setName   = setName,
                            cardNumber= cardNo,
                            cardName  = cardName,
                            color     = color
                        )
                    }

                    return out
                }
            }
        }
    }
}
