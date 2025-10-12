package com.example.prototyp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.prototyp.data.db.CardDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.prototyp.deckBuilder.Deck
import com.example.prototyp.deckBuilder.DeckCard
import com.example.prototyp.deckBuilder.DeckDao
import com.example.prototyp.externalCollection.*
import com.example.prototyp.wishlist.WishlistDao
import com.example.prototyp.wishlist.WishlistEntry
import com.example.prototyp.externalWishlist.*
import com.example.prototyp.statistics.*
import kotlinx.coroutines.flow.MutableStateFlow

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
        ExternalWishlistCard::class,
        PriceHistory::class,
        TotalValueHistory::class
    ],
    version = 18, // Version updated
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun masterCardDao(): MasterCardDao
    abstract fun deckDao(): DeckDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun externalCollectionDao(): ExternalCollectionDao
    abstract fun externalWishlistDao(): ExternalWishlistDao
    abstract fun priceHistoryDao(): PriceHistoryDao

    abstract fun totalValueHistoryDao(): TotalValueHistoryDao

    companion object {
        val isReady = MutableStateFlow(false)

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "riftbound.db")
                .addMigrations(MIGRATION_16_17, MIGRATION_17_18) // New migration added
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // This logic runs only when the DB is created for the first time
                        CoroutineScope(Dispatchers.IO).launch {
                            isReady.value = false
                            val appDb = getInstance(context)
                            loadMasterCards(appDb, context)
                            isReady.value = true
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // This logic runs every time the DB is opened
                        CoroutineScope(Dispatchers.IO).launch {
                            isReady.value = false
                            val appDb = getInstance(context)
                            val count = appDb.masterCardDao().count()
                            if (count == 0) {
                                loadMasterCards(appDb, context)
                            }
                            isReady.value = true
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        // New migration to create the price_history table
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `price_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `setCode` TEXT NOT NULL,
                        `cardNumber` INTEGER NOT NULL,
                        `price` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `total_value_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `totalValue` REAL NOT NULL
                    )
                """)
            }
        }

        private suspend fun loadMasterCards(appDb: AppDatabase, context: Context) {
            try {
                val items = loadCardsFromAssets(context, "master_cards.csv")
                if (items.isNotEmpty()) {
                    appDb.masterCardDao().insertAll(items)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun loadCardsFromAssets(context: Context, assetName: String): List<MasterCard> {
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

