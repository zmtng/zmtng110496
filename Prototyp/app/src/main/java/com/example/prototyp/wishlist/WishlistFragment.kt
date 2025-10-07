package com.example.prototyp.wishlist

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.R
import com.example.prototyp.MasterCardDao
import com.example.prototyp.wishlist.WishlistDao
import com.example.prototyp.wishlist.AddCardToWishlistFragment
import com.example.prototyp.wishlist.WishlistAdapter
import com.example.prototyp.wishlist.WishlistViewModel
import com.example.prototyp.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private val viewModel: WishlistViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        WishlistViewModelFactory(db.wishlistDao(), db.masterCardDao())
    }
    private lateinit var wishlistAdapter: WishlistAdapter

    private val colorMap = mapOf(
        "R" to "Rot",
        "B" to "Blau",
        "G" to "Grün",
        "Y" to "Gelb",
        "P" to "Lila",
        "O" to "Orange",
        "U" to "Grau",
        "M" to "Mehrfarbig"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapter mit Klick-Funktionen für die Buttons initialisieren
        wishlistAdapter = WishlistAdapter(
            onIncrement = { card -> viewModel.incrementQuantity(card) },
            onDecrement = { card -> viewModel.decrementQuantity(card) }
        )
        val rv = view.findViewById<RecyclerView>(R.id.rvWishlist)
        rv.adapter = wishlistAdapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        // Listener für den "Karte hinzufügen"-Button
        view.findViewById<FloatingActionButton>(R.id.fabAddCardToWishlist).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardToWishlistFragment())
                .addToBackStack(null)
                .commit()
        }

        // Beobachtet die (gefilterte) Liste vom ViewModel und aktualisiert die UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wishlistCards.collectLatest { cards ->
                wishlistAdapter.submitList(cards)
            }
        }

        setupFilters(view)
    }

    // ##### HINZUGEFÜGT: Neue Funktion zur Einrichtung der Filter #####
    private fun setupFilters(view: View) {
        val searchView = view.findViewById<SearchView>(R.id.searchViewWishlist)
        val colorSpinner = view.findViewById<Spinner>(R.id.spinnerColor)
        val setSpinner = view.findViewById<Spinner>(R.id.spinnerSet)

        // SearchView-Listener (bleibt gleich)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        // Spinner-Listener und Befüllung
        lifecycleScope.launch {

            val colorCodes = viewModel.getFilterColors()

            val colorNamesForSpinner = colorCodes.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorNamesForSpinner)
            colorSpinner.adapter = colorAdapter

            colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        viewModel.setColorFilter("")
                    } else {

                        val selectedName = colorNamesForSpinner[position]
                        val colorCodeToSend = colorMap.entries.find { it.value == selectedName }?.key ?: ""
                        viewModel.setColorFilter(colorCodeToSend)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val sets = viewModel.getFilterSets().toMutableList().apply { add(0, "Alle Sets") }

            val setAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sets)
            setSpinner.adapter = setAdapter
            setSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selection = if (position == 0) "" else sets[position]
                    viewModel.setSetFilter(selection)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }
}

class WishlistViewModelFactory(
    private val wishlistDao: WishlistDao,
    private val masterDao: MasterCardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WishlistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WishlistViewModel(wishlistDao, masterDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}