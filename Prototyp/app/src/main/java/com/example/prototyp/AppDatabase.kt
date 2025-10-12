package com.example.prototyp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.prototyp.deckBuilder.Deck
import com.example.prototyp.deckBuilder.DeckCard
import com.example.prototyp.deckBuilder.DeckDao
import com.example.prototyp.externalCollection.*
import com.example.prototyp.wishlist.WishlistDao
import com.example.prototyp.wishlist.WishlistEntry
import com.example.prototyp.externalWishlist.*
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Database(
    entities = [
        CollectionEntry::class,
        MasterCard::class,
        CardSet::class,
        Deck::class,
        DeckCard::class,
        WishlistEntry::class,
        ExternalCollection::class,
        ExternalCollectionCard::class,
        ExternalWishlist::class,
        ExternalWishlistCard::class
    ],
    version = 16, // Du kannst die Version beibehalten, da sich die Struktur nicht ändert
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun masterCardDao(): MasterCardDao
    abstract fun deckDao(): DeckDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun externalCollectionDao(): ExternalCollectionDao
    abstract fun externalWishlistDao(): ExternalWishlistDao

    companion object {
        // --- NEU: Das "Bereit"-Signal ---
        private val _isReady = MutableStateFlow(false)
        val isReady = _isReady.asStateFlow()
        // -----------------------------

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "riftbound.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
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
                                // --- NEU: Signal senden, dass die DB bereit ist ---
                                _isReady.value = true
                                // --------------------------------------------------
                            } catch (e: Exception) {
                                e.printStackTrace()
                                _isReady.value = true // Auch im Fehlerfall fortfahren
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun loadCardsFromAssets(context: Context, assetName: String): List<MasterCard> {
            // Unveränderte Lade-Logik...
            context.assets.open(assetName).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                    fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val out = mutableListOf<MasterCard>()
                    val headerLine = br.readLine() ?: return emptyList()
                    val delimiter = if (headerLine.count { it == ';' } > headerLine.count { it == ',' }) ';' else ','
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
                    fun idx(vararg synonyms: String): Int {
                        val wanted = synonyms.map(::normalize)
                        return headerNorm.indexOfFirst { hn -> wanted.any { it == hn } }
                    }
                    val iSetCode   = idx("set_code","setcode","code","set")
                    val iSetName   = idx("set_name","setname","name","settitle","setlabel")
                    val iCardNo    = idx("card_number","cardnumber","number","no","nr")
                    val iCardName  = idx("card_name","cardname","name","cardtitle","title")
                    val iColor     = idx("color","colour")
                    if (iSetCode < 0 || iSetName < 0 || iCardNo < 0 || iCardName < 0) {
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
                        val color     = get(iColor).ifBlank { "U" }
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

