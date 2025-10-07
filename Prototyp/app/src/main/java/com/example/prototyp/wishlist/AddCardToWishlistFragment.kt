package com.example.prototyp.wishlist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.AppDatabase
import com.example.prototyp.R
import com.example.prototyp.deckBuilder.MasterCardSearchAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AddCardToWishlistFragment : Fragment(R.layout.fragment_add_card_to_deck) {

    private val viewModel: WishlistViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        WishlistViewModelFactory(db.wishlistDao(), db.masterCardDao(), db.cardDao())
    }
    private lateinit var searchAdapter: MasterCardSearchAdapter
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvMasterCards)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        searchAdapter = MasterCardSearchAdapter(emptyList()) { card ->
            viewModel.addCardToWishlist(card)
            // ##### HIER IST DIE ÄNDERUNG: Feedback für den Nutzer #####
            Toast.makeText(requireContext(), "'${card.cardName}' zur Wunschliste hinzugefügt", Toast.LENGTH_SHORT).show()
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = searchAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300L)
                    val results = viewModel.searchMasterCards(newText.orEmpty())
                    searchAdapter.updateData(results)
                }
                return true
            }
        })

        searchView.setQuery("", true)
    }
}