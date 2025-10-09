package com.example.prototyp.externalCollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.prototyp.AppDatabase
import com.example.prototyp.databinding.FragmentExternalCollectionOverviewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.prototyp.R

class ExternalCollectionOverviewFragment : Fragment() {

    private var _binding: FragmentExternalCollectionOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExternalCollectionOverviewViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        ExternalCollectionOverviewViewModelFactory(db.externalCollectionDao(), db.masterCardDao())
    }
    private lateinit var collectionAdapter: ExternalCollectionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExternalCollectionOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Externe Sammlungen"

        collectionAdapter = ExternalCollectionAdapter(
            onClick = { collection ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ExternalCollectionDetailFragment.newInstance(collection.id))
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { collection ->
                showDeleteConfirmationDialog(collection)
            }
        )

        binding.rvExternalCollections.adapter = collectionAdapter
        binding.rvExternalCollections.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allCollections.collectLatest { collections ->
                collectionAdapter.submitList(collections)
            }
        }
    }

    private fun showDeleteConfirmationDialog(collection: ExternalCollection) {
        AlertDialog.Builder(requireContext())
            .setTitle("Sammlung löschen")
            .setMessage("Möchtest du '${collection.name}' wirklich endgültig löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteCollection(collection)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}