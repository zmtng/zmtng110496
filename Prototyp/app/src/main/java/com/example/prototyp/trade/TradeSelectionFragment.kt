package com.example.prototyp.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.prototyp.AppDatabase
import com.example.prototyp.R
import com.example.prototyp.databinding.FragmentTradeSelectionBinding
import com.example.prototyp.externalCollection.ExternalCollection
import com.example.prototyp.externalWishlist.ExternalWishlist
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TradeSelectionFragment : Fragment() {
    private var _binding: FragmentTradeSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TradeFinderViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        TradeFinderViewModelFactory(db.tradeDao(), db.externalCollectionDao(), db.externalWishlistDao())
    }

    private var collections: List<ExternalCollection> = emptyList()
    private var wishlists: List<ExternalWishlist> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTradeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Spinner für externe Sammlungen befüllen
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExternalCollections.collectLatest {
                collections = it
                setupSpinner(binding.spinnerExternalCollection, it.map { c -> c.name }, "Keine Sammlungen gefunden")
                binding.btnCompareWithMyWishlist.isEnabled = it.isNotEmpty()
            }
        }

        // Spinner für externe Wunschlisten befüllen
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExternalWishlists.collectLatest {
                wishlists = it
                setupSpinner(binding.spinnerExternalWishlist, it.map { w -> w.name }, "Keine Wunschlisten gefunden")
                binding.btnCompareWithMyCollection.isEnabled = it.isNotEmpty()
            }
        }

        // Button 1: Externe Sammlung -> Meine Wunschliste
        binding.btnCompareWithMyWishlist.setOnClickListener {
            if (collections.isNotEmpty()) {
                val selectedCollection = collections[binding.spinnerExternalCollection.selectedItemPosition]
                navigateToResult(selectedCollection.id, TradeResultFragment.ComparisonType.THEY_HAVE)
            } else {
                Toast.makeText(requireContext(), "Bitte importiere zuerst eine externe Sammlung.", Toast.LENGTH_SHORT).show()
            }
        }

        // Button 2: Externe Wunschliste -> Meine Sammlung
        binding.btnCompareWithMyCollection.setOnClickListener {
            if (wishlists.isNotEmpty()) {
                val selectedWishlist = wishlists[binding.spinnerExternalWishlist.selectedItemPosition]
                navigateToResult(selectedWishlist.id, TradeResultFragment.ComparisonType.YOU_HAVE)
            } else {
                Toast.makeText(requireContext(), "Bitte importiere zuerst eine externe Wunschliste.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToResult(id: Int, type: TradeResultFragment.ComparisonType) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TradeResultFragment.newInstance(id, type))
            .addToBackStack(null)
            .commit()
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>, emptyText: String) {
        val displayItems = items.ifEmpty { listOf(emptyText) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayItems)
        spinner.adapter = adapter
        spinner.isEnabled = items.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}