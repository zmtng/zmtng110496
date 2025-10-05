package com.example.prototyp.wishlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.AddCardFragment
import com.example.prototyp.CardAdapter
import com.example.prototyp.CollectionRow
import com.example.prototyp.MasterCardDao
import com.example.prototyp.R
import com.example.prototyp.data.db.CollectionItem
import com.example.prototyp.data.db.WishlistDao
import com.example.yourapp.data.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private lateinit var wishlistDao: WishlistDao
    private lateinit var masterDao: MasterCardDao

    private var nameByKey: Map<Pair<String, Int>, String> = emptyMap()
    private var setNameByCode: Map<String, String> = emptyMap()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WishlistAdapter
    private val rows = mutableListOf<WishlistRow>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.Companion.getInstance(requireContext())
        wishlistDao = db.wishlistDao()
        masterDao = db.masterCardDao()

        // RecyclerView

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CardAdapter(
            rows,
            onIncrement = { row ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    wishlistDao.addQuantity(row.setCode, row.number, +1)
                }
            },
            onDecrement = { row ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    // Menge um 1 reduzieren
                    wishlistDao.addQuantity(row.setCode, row.number, -1)

                    // prüfen, ob 0 -> löschen
                    val updated = wishlist.getByKey(row.setCode, row.number)
                    if (updated == null || updated.quantity <= 0) {
                        wishlist.deleteByKey(row.setCode, row.number)
                    }
                }
            }
        )
        recyclerView.adapter = adapter

        // "+ Karte" → AddCardFragment öffnen
        view.findViewById<FloatingActionButton>(R.id.btnAddCard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardFragment())
                .addToBackStack(null)
                .commit()
        }


        // 1) Master-Cache laden, 2) danach Sammlung beobachten
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) Masterdaten im IO-Thread aufbauen
            withContext(Dispatchers.IO) {
                val all = masterDao.search("") // LIKE '%%' → alle Masterkarten
                nameByKey = all.associate { (it.setCode to it.cardNumber) to it.cardName }
                setNameByCode = all.asSequence()
                    .map { it.setCode to it.setName }
                    .distinct()
                    .toMap()
            }

            // 2) Beobachten (läuft auf Main; Mapping nutzt den Cache)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                wishlist.observeCollectionRaw().collectLatest { list: List<CollectionItem> ->
                    val ui = list.map { ci ->
                        val key = ci.setCode to ci.cardNumber
                        CollectionRow(
                            setCode = ci.setCode,
                            setName = setNameByCode[ci.setCode],       // kann null sein
                            number = ci.cardNumber,
                            name = nameByKey[key] ?: "(unbekannt)", // Fallback
                            quantity = ci.quantity,
                            price = ci.price,
                            color = ci.color
                        )
                    }
                    rows.clear()
                    rows.addAll(ui)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}