package com.example.prototyp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.databinding.FragmentHomeBinding
import com.example.prototyp.deckBuilder.DeckOverviewFragment
import com.example.prototyp.prefs.ThemePrefs
import com.example.prototyp.wishlist.WishlistFragment
import com.example.prototyp.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        HomeViewModelFactory(database.cardDao())
    }

    // Launcher for the Export-Dialog
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportCollection(it, requireContext())
        }
    }

    // Launcher for the Import-Dialog
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCollection(it, requireContext())
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // ##### ALT: Der Listener für den Optionen-Button wird nicht mehr gebraucht. #####
        // binding.btnOptions.setOnClickListener { anchorView ->
        //     showOptionsMenu(anchorView)
        // }

        // ##### NEU: Listener für die neuen Kachel-Buttons #####
        setupClickListeners()


        // Beobachter für das ViewModel (bleiben unverändert)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Beobachter für User-Nachrichten
                launch {
                    viewModel.userMessage.collectLatest { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.onUserMessageShown()
                        }
                    }
                }

                // Beobachter für Gesamtwert (aktualisiert jetzt die neue TextView)
                launch {
                    viewModel.totalCollectionValue.collectLatest { value ->
                        if (value != null) {
                            binding.tvTotalValue.text = String.format("%.2f €", value)
                        } else {
                            binding.tvTotalValue.text = "-,-- €"
                        }
                    }
                }
            }
        }
    }

    // ##### NEU: Eine Funktion, um die Click-Listener zu organisieren #####
    private fun setupClickListeners() {
        binding.cardCollection.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CollectionFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardDecks.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DeckOverviewFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardWishlist.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, WishlistFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardCalculateValue.setOnClickListener {
            viewModel.updateTotalValue()
            Toast.makeText(requireContext(), "Gesamtwert wird berechnet...", Toast.LENGTH_SHORT).show()
        }

        binding.cardExport.setOnClickListener {
            exportLauncher.launch("sammlung_export.csv")
        }

        binding.cardImport.setOnClickListener {
            importLauncher.launch("*/*")
        }

        binding.cardToggleTheme.setOnClickListener {
            val themePrefs = ThemePrefs(requireContext())
            val newNightMode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
            themePrefs.theme = newNightMode
            AppCompatDelegate.setDefaultNightMode(newNightMode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HomeViewModelFactory(private val cardDao: CardDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
