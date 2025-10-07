package com.example.prototyp.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.prototyp.R
import com.example.prototyp.MasterCardDao

import com.example.prototyp.AppDatabase
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.databinding.FragmentWishlistBinding

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WishlistViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        // ##### HINWEIS: Die Factory-Parameter-Reihenfolge muss zum ViewModel-Konstruktor passen #####
        WishlistViewModelFactory(db.wishlistDao(), db.masterCardDao(), db.cardDao())
    }
    private lateinit var wishlistAdapter: WishlistAdapter

    private val colorMap = mapOf("R" to "Rot", "B" to "Blau", "G" to "Grün", "Y" to "Gelb", "P" to "Lila", "O" to "Orange", "U" to "Grau", "M" to "Mehrfarbig")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        setupObservers()
        setupFilters()
    }

    private fun setupAdapter() {
        // ##### KORRIGIERT: Adapter-Initialisierung mit allen benötigten Lambdas #####
        // Der WishlistAdapter erwartet drei Funktionen im Konstruktor: onIncrement, onDecrement, onMoveToCollection
        wishlistAdapter = WishlistAdapter(
            onIncrement = { card -> viewModel.incrementQuantity(card) },
            onDecrement = { card -> viewModel.decrementQuantity(card) },
            onMoveToCollection = { card ->
                // Hier wird jetzt die korrigierte ViewModel-Funktion aufgerufen.
                viewModel.transferToCollection(card, 1) // Verschiebt 1 Exemplar
                Toast.makeText(requireContext(), "'${card.cardName}' zur Sammlung hinzugefügt", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvWishlist.adapter = wishlistAdapter
        binding.rvWishlist.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.fabAddCardToWishlist.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardToWishlistFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.wishlistCards.collectLatest { cards ->
                    wishlistAdapter.submitList(cards)
                }
            }
        }
    }

    private fun setupFilters() {
        binding.searchViewWishlist.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            // Farb-Spinner
            val colorsFromDb = viewModel.getFilterColors()
            val colorItemsForSpinner = colorsFromDb.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            binding.spinnerColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorItemsForSpinner)
            binding.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    val selection = if (position == 0) null else colorsFromDb[position - 1]
                    viewModel.setColorFilter(selection?: "")
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            // Set-Spinner
            val setsFromDb = viewModel.getFilterSets()
            val setItemsForSpinner = setsFromDb.toMutableList().apply { add(0, "Alle Sets") }
            binding.spinnerSet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, setItemsForSpinner)
            binding.spinnerSet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    val selection = if (position == 0) null else setsFromDb[position - 1]
                    viewModel.setSetFilter(selection?: "")
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class WishlistViewModelFactory(
    private val wishlistDao: WishlistDao,
    private val masterDao: MasterCardDao,
    private val cardDao: CardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WishlistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WishlistViewModel(wishlistDao, masterDao, cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
