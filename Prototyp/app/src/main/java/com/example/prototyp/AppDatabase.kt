package com.example.prototyp

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
import com.example.prototyp.externalCollection.*
import com.example.prototyp.wishlist.WishlistDao
import com.example.prototyp.wishlist.WishlistEntry
import com.example.prototyp.externalWishlist.*


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
    version = 12,
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


        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "riftbound.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_5_6, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
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
                        // Fallback: Wenn master_cards leer ist erneut importieren
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
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `decks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL
                    )
                """)
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

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQL-Befehl zum Erstellen der neuen Tabelle für externe Sammlungen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `external_collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `external_collection_cards` (
                        `collectionId` INTEGER NOT NULL,
                        `setCode` TEXT NOT NULL,
                        `cardNumber` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `price` REAL,
                        PRIMARY KEY(`collectionId`, `setCode`, `cardNumber`),
                        FOREIGN KEY(`collectionId`) REFERENCES `external_collections`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `external_wishlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `external_wishlist_cards` (
                        `wishlistId` INTEGER NOT NULL,
                        `setCode` TEXT NOT NULL,
                        `cardNumber` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        PRIMARY KEY(`wishlistId`, `setCode`, `cardNumber`),
                        FOREIGN KEY(`wishlistId`) REFERENCES `external_wishlists`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deck_cards ADD COLUMN color TEXT NOT NULL DEFAULT 'U'")
                db.execSQL("ALTER TABLE wishlist ADD COLUMN color TEXT NOT NULL DEFAULT 'U'")
                db.execSQL("ALTER TABLE external_collection_cards ADD COLUMN color TEXT NOT NULL DEFAULT 'U'")
                db.execSQL("ALTER TABLE external_wishlist_cards ADD COLUMN color TEXT NOT NULL DEFAULT 'U'")
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
