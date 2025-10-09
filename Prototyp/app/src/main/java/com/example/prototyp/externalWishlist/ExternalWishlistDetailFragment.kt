package com.example.prototyp.externalWishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.prototyp.databinding.FragmentExternalWishlistDetailBinding

class ExternalWishlistDetailFragment : Fragment() {

    private var _binding: FragmentExternalWishlistDetailBinding? = null
    private val binding get() = _binding!!

    private val wishlistId: Int by lazy { requireArguments().getInt(ARG_WISHLIST_ID) }

    private val viewModel: ExternalWishlistDetailViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        ExternalWishlistDetailViewModelFactory(db.externalWishlistDao(), db.masterCardDao(), db.wishlistDao())
    }

    private lateinit var cardAdapter: ExternalWishlistCardAdapter
    private val colorMap = mapOf("R" to "Rot", "B" to "Blau", "G" to "Grün", "Y" to "Gelb", "P" to "Lila", "O" to "Orange", "U" to "Grau", "M" to "Mehrfarbig")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExternalWishlistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setWishlistId(wishlistId)

        binding.toolbar.title = "Wunschlisten-Details" // Titel anpassen

        cardAdapter = ExternalWishlistCardAdapter { card ->
            viewModel.addCardToOwnWishlist(card)
            Toast.makeText(requireContext(), "'${card.cardName}' zur eigenen Wunschliste hinzugefügt", Toast.LENGTH_SHORT).show()
        }
        binding.rvExternalCards.adapter = cardAdapter
        binding.rvExternalCards.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wishlistContents.collectLatest { cards ->
                cardAdapter.submitList(cards)
            }
        }
        setupFilters()
    }

    private fun setupFilters() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            val colorsFromDb = viewModel.getFilterColors()
            val colorItems = colorsFromDb.map { colorMap[it] ?: it }.toMutableList().apply { add(0, "Alle Farben") }
            binding.spinnerColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colorItems)
            binding.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setColorFilter(if (position == 0) "" else colorsFromDb[position - 1])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val setsFromDb = viewModel.getFilterSets()
            val setItems = setsFromDb.toMutableList().apply { add(0, "Alle Sets") }
            binding.spinnerSet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, setItems)
            binding.spinnerSet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setSetFilter(if (position == 0) "" else setsFromDb[position - 1])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_WISHLIST_ID = "wishlist_id"
        fun newInstance(wishlistId: Int) = ExternalWishlistDetailFragment().apply {
            arguments = bundleOf(ARG_WISHLIST_ID to wishlistId)
        }
    }
}