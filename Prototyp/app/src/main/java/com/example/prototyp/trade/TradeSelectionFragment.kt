package com.example.prototyp.trade

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.prototyp.AppDatabase
import com.example.prototyp.R
import com.example.prototyp.databinding.FragmentTradeSelectionBinding
import com.example.prototyp.deckBuilder.DeckDao
import com.example.prototyp.externalCollection.ExternalCollection
import com.example.prototyp.externalWishlist.ExternalWishlist
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TradeSelectionFragment : Fragment() {

    private var _binding: FragmentTradeSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TradeSelectionViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        TradeSelectionViewModelFactory(db.externalCollectionDao(), db.externalWishlistDao(), db.deckDao())
    }

    private enum class TradeMode { WANT, OFFER }
    private var currentMode = TradeMode.WANT

    // NEW: Member variables to hold the full data objects
    private var externalCollections: List<ExternalCollection> = emptyList()
    private var externalWishlists: List<ExternalWishlist> = emptyList()
    private var decks: List<DeckDao.DeckWithCardCount> = emptyList()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTradeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggles()
        setupSpinners()
        setupClickListeners()
        updateMode(TradeMode.WANT) // Set initial state
    }

    private fun setupToggles() {
        binding.cardModeWant.setOnClickListener {
            updateMode(TradeMode.WANT)
        }
        binding.cardModeOffer.setOnClickListener {
            updateMode(TradeMode.OFFER)
        }
    }

    private fun updateMode(newMode: TradeMode) {
        currentMode = newMode
        binding.sectionWant.isVisible = newMode == TradeMode.WANT
        binding.sectionOffer.isVisible = newMode == TradeMode.OFFER

        // CORRECTED: Update card strokes to show selection using theme attribute
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        binding.cardModeWant.strokeColor = if (newMode == TradeMode.WANT) primaryColor else Color.TRANSPARENT
        binding.cardModeOffer.strokeColor = if (newMode == TradeMode.OFFER) primaryColor else Color.TRANSPARENT
    }

    private fun setupSpinners() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExternalCollections.collectLatest { collections ->
                this@TradeSelectionFragment.externalCollections = collections
                setupSpinner(binding.spinnerWantExternalCollection, collections.map { it.name })
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExternalWishlists.collectLatest { wishlists ->
                this@TradeSelectionFragment.externalWishlists = wishlists
                setupSpinner(binding.spinnerOfferExternalWishlist, wishlists.map { it.name })
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allDecks.collectLatest { decksWithCount ->
                this@TradeSelectionFragment.decks = decksWithCount
                setupSpinner(binding.spinnerOfferDeck, decksWithCount.map { it.deck.name })
            }
        }
    }

    private fun <T> setupSpinner(spinner: Spinner, items: List<T>) {
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)
    }

    private fun setupClickListeners() {
        binding.chipGroupOfferSource.setOnCheckedChangeListener { _, checkedId ->
            binding.spinnerOfferDeck.isVisible = checkedId == R.id.chip_offer_deck
        }

        binding.fabStartTrade.setOnClickListener {
            when (currentMode) {
                TradeMode.WANT -> startWantTrade()
                TradeMode.OFFER -> startOfferTrade()
            }
        }
    }

    private fun startWantTrade() {
        // CORRECTED: Get object by position
        val selectedPosition = binding.spinnerWantExternalCollection.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= externalCollections.size) {
            Toast.makeText(requireContext(), "Bitte eine externe Sammlung auswählen", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedCollection = externalCollections[selectedPosition]
        navigateToResult("WANT", selectedCollection.id, -1, "Sie haben, was du willst:")
    }

    private fun startOfferTrade() {
        // CORRECTED: Get object by position
        val selectedWishlistPosition = binding.spinnerOfferExternalWishlist.selectedItemPosition
        if (selectedWishlistPosition < 0 || selectedWishlistPosition >= externalWishlists.size) {
            Toast.makeText(requireContext(), "Bitte eine externe Wunschliste auswählen", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedWishlist = externalWishlists[selectedWishlistPosition]

        if (binding.chipOfferCollection.isChecked) {
            navigateToResult("OFFER_COLLECTION", selectedWishlist.id, -1, "Du hast, was sie wollen (aus Sammlung):")
        } else if (binding.chipOfferDeck.isChecked) {
            val selectedDeckPosition = binding.spinnerOfferDeck.selectedItemPosition
            if (selectedDeckPosition < 0 || selectedDeckPosition >= decks.size) {
                Toast.makeText(requireContext(), "Bitte ein Deck auswählen", Toast.LENGTH_SHORT).show()
                return
            }
            val selectedDeck = decks[selectedDeckPosition]
            navigateToResult("OFFER_DECK", selectedWishlist.id, selectedDeck.deck.id, "Du hast, was sie wollen (aus Deck '${selectedDeck.deck.name}'):")
        }
    }

    private fun navigateToResult(tradeType: String, list1Id: Int, list2Id: Int, title: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TradeResultFragment.newInstance(tradeType, list1Id, list2Id, title))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

