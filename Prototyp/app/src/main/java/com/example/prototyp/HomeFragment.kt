package com.example.prototyp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.example.prototyp.prefs.ThemePrefs
import com.example.yourapp.data.db.AppDatabase
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

    // Launcher für den Export-Dialog
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportCollection(it, requireContext())
        }
    }

    // Launcher für den Import-Dialog
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

        viewLifecycleOwner.lifecycleScope.launch {
            // ViewModel mit unserer Factory erstellen
            val database = AppDatabase.getInstance(requireContext())
            val viewModelFactory = HomeViewModelFactory(database.cardDao())

            // Button-Listener
            view.findViewById<Button>(R.id.btnExport).setOnClickListener {
                exportLauncher.launch("sammlung_export.csv") // Dateiname-Vorschlag
            }

            view.findViewById<Button>(R.id.btnImport).setOnClickListener {
                importLauncher.launch("*/*") // Dateityp-Filter
            }
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userMessage.collectLatest { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.onUserMessageShown() // Nachricht als "gesehen" markieren
                    }
                }
            }
            val saved = ThemePrefs.modeFlow(requireContext()).first()
            binding.switchDark.isChecked = (saved == AppCompatDelegate.MODE_NIGHT_YES)

            binding.switchDark.setOnCheckedChangeListener { _, isChecked ->
                val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                viewLifecycleOwner.lifecycleScope.launch {
                    ThemePrefs.setMode(requireContext(), mode)
                    requireActivity().recreate()
                }
            }
        }


        // Button, um zur Collection zu wechseln
        view.findViewById<Button>(R.id.btnCollection).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CollectionFragment())
                .addToBackStack(null)
                .commit()
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
