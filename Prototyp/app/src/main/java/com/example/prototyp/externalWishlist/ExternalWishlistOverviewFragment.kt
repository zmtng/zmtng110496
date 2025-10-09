package com.example.prototyp.externalWishlist

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
import com.example.prototyp.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.prototyp.databinding.FragmentExternalWishlistOverviewBinding

class ExternalWishlistOverviewFragment : Fragment() {

    private var _binding: FragmentExternalWishlistOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExternalWishlistOverviewViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        ExternalWishlistOverviewViewModelFactory(db.externalWishlistDao())
    }
    private lateinit var wishlistAdapter: ExternalWishlistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExternalWishlistOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Externe Wunschlisten"

        wishlistAdapter = ExternalWishlistAdapter(
            onClick = { wishlist ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ExternalWishlistDetailFragment.newInstance(wishlist.id))
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { wishlist ->
                showDeleteConfirmationDialog(wishlist)
            }
        )

        binding.rvExternalWishlists.adapter = wishlistAdapter
        binding.rvExternalWishlists.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWishlists.collectLatest { wishlists ->
                wishlistAdapter.submitList(wishlists)
            }
        }
    }

    private fun showDeleteConfirmationDialog(wishlist: ExternalWishlist) {
        AlertDialog.Builder(requireContext())
            .setTitle("Wunschliste löschen")
            .setMessage("Möchtest du '${wishlist.name}' wirklich endgültig löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteWishlist(wishlist)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}