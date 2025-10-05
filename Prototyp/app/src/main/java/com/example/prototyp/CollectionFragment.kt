package com.example.prototyp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.data.db.CollectionItem
import com.example.yourapp.data.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionFragment : Fragment(R.layout.fragment_collection) {

    private lateinit var cardDao: CardDao
    private lateinit var masterDao: MasterCardDao

    private var nameByKey: Map<Pair<String, Int>, String> = emptyMap()
    private var setNameByCode: Map<String, String> = emptyMap()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private val rows = mutableListOf<CollectionRow>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        cardDao = db.cardDao()
        masterDao = db.masterCardDao()

        // RecyclerView

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CardAdapter(
            rows,
            onIncrement = { row ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    cardDao.addQuantity(row.setCode, row.number, +1)
                }
            },
            onDecrement = { row ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    // Menge um 1 reduzieren
                    cardDao.addQuantity(row.setCode, row.number, -1)

                    // prüfen, ob 0 -> löschen
                    val updated = cardDao.getByKey(row.setCode, row.number)
                    if (updated == null || updated.quantity <= 0) {
                        cardDao.deleteByKey(row.setCode, row.number)
                    }
                }
            }
        )
        recyclerView.adapter = adapter

        // "+ Karte" → AddCardFragment öffnen
        view.findViewById< FloatingActionButton>(R.id.btnAddCard).setOnClickListener {
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
                cardDao.observeCollectionRaw().collectLatest { list: List<CollectionItem> ->
                    val ui = list.map { ci ->
                        val key = ci.setCode to ci.cardNumber
                        CollectionRow(
                            setCode  = ci.setCode,
                            setName  = setNameByCode[ci.setCode],       // kann null sein
                            number   = ci.cardNumber,
                            name     = nameByKey[key] ?: "(unbekannt)", // Fallback
                            quantity = ci.quantity,
                            price    = ci.price,
                            color    = ci.color
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
