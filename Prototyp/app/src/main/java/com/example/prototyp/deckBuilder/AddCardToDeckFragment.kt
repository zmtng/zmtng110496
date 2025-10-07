package com.example.prototyp.ui // Passe das Paket ggf. an

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.deckBuilder.MasterCardSearchAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.prototyp.deckBuilder.DeckDetailViewModel

class AddCardToDeckFragment : Fragment(R.layout.fragment_add_card_to_deck) {

    // Greift auf das ViewModel des DeckDetailFragment zu
    private val viewModel: DeckDetailViewModel by activityViewModels()

    private lateinit var searchAdapter: MasterCardSearchAdapter
    private var searchJob: Job? = null // Job, um die Suche zu verwalten

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvMasterCards)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        // Adapter initialisieren
        searchAdapter = MasterCardSearchAdapter(emptyList()) { card ->
            viewModel.addCardToDeck(card)
            // Kurze Bestätigung, dass die Karte hinzugefügt wurde.
            // Toast.makeText(requireContext(), "'${card.cardName}' hinzugefügt", Toast.LENGTH_SHORT).show()
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = searchAdapter

        // Such-Listener einrichten
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Verhindert, dass die Tastatur sich schließt
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // ##### NEUE, ROBUSTE SUCHLOGIK #####
                searchJob?.cancel() // Alte Suche abbrechen
                searchJob = lifecycleScope.launch {
                    delay(300L) // 300ms warten, bevor die Suche startet (Debounce)
                    val results = viewModel.searchMasterCards(newText.orEmpty())
                    searchAdapter.updateData(results)
                }
                return true
            }
        })

        // ##### HINZUGEFÜGT: Führe eine initiale Suche aus, um die Liste sofort zu füllen #####
        // Wir lösen manuell die erste Suche mit leerem Text aus
        searchView.setQuery("", true)
    }
}