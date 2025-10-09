package com.example.prototyp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.example.prototyp.externalCollection.*


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        HomeViewModelFactory(database.cardDao(), database.masterCardDao())
    }

    // Launcher for the Export-Dialog
    /*private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportCollection(it, requireContext())
        }
    }*/

    // Launcher for the Import-Dialog
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            showImportTargetDialog(it) // Ruft den neuen Dialog auf
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

        binding.cardInfo.setOnClickListener {
            showInfoDialog()
        }

        binding.cardExternal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExternalCollectionOverviewFragment())
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
            // Starte die Coroutine, um die CSV zu erstellen und zu teilen
            viewLifecycleOwner.lifecycleScope.launch {
                val fileUri = viewModel.createCollectionCsvForSharing(requireContext())
                if (fileUri != null) {
                    shareCollection(fileUri)
                }
            }
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

    private fun showImportTargetDialog(uri: Uri) {
        val options = arrayOf("In meine Sammlung", "Als externe Sammlung")
        AlertDialog.Builder(requireContext())
            .setTitle("Wohin importieren?")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> viewModel.importCollection(uri, requireContext()) // Bestehende Funktion
                    1 -> showNameInputDialogForExternal(uri)
                }
            }
            .show()
    }

    private fun shareCollection(fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Sammlung teilen via..."))
    }

    private fun showNameInputDialogForExternal(uri: Uri) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Name für externe Sammlung")
            .setView(input)
            .setPositiveButton("Importieren") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    val externalViewModel: ExternalCollectionOverviewViewModel by viewModels {
                        val db = AppDatabase.getInstance(requireContext())
                        ExternalCollectionOverviewViewModelFactory(db.externalCollectionDao(), db.masterCardDao())
                    }
                    externalViewModel.importCollection(uri, requireContext(), name)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Info & Hilfe")
            .setMessage(
                "Willkommen beim Riftbound TCG Manager!\n\n" +
                        "Hier findest du eine Übersicht aller wichtigen Funktionen:\n\n" +
                        "--- ÜBERSICHT ---\n\n" +
                        "• Wert berechnen: Tippe auf diese Kachel, um die aktuellen Preise (Preis-Trend von Cardmarket) für alle Karten deiner Sammlung abzurufen und den Gesamtwert zu ermitteln.\n\n" +
                        "--- SAMMLUNG ---\n\n" +
                        "• Karten hinzufügen: Nutze den Plus-Button (+) unten rechts, um neue Karten über die Suchfunktion zu deiner Sammlung hinzuzufügen.\n\n" +
                        "• Preise aktualisieren: Mit dem Aktualisieren-Button (Pfeil im Kreis) links unten kannst du die Preise für deine gesamte Sammlung auf den neuesten Stand bringen.\n\n" +
                        "• Notizen erfassen: Tippe eine Karte in der Sammlungsliste an, um persönliche Notizen hinzuzufügen oder zu bearbeiten.\n\n" +
                        "--- DECKS ---\n\n" +
                        "• Deck erstellen: Erstelle mit dem Plus-Button (+) eigene Decks von Grund auf.\n\n" +
                        "• Deck importieren: Nutze den Import-Button (Pfeil nach unten), um Decks direkt aus 'Piltover\'s Archive' zu importieren. Exportiere dazu dein Deck auf der Webseite als \"TTS-Code\" und füge diesen hier ein.\n\n" +
                        "• Sammlungs-Status: In der Deckansicht zeigt das Hand-Symbol, ob du eine Karte besitzt (grün) oder nicht (rot).\n\n" +
                        "• Zur Wunschliste: Mit dem Stern-Symbol kannst du eine fehlende Karte direkt auf deine Wunschliste setzen. Ein ausgefüllter, gelber Stern bedeutet, die Karte ist bereits auf der Liste.\n\n" +
                        "--- WUNSCHLISTE ---\n\n" +
                        "• In Sammlung übertragen: Tippe auf den 'In Sammlung'-Button bei einer Karte in der Wunschliste, um sie in deine Sammlung zu verschieben.\n\n" +
                        "--- ALLGEMEIN ---\n\n" +
                        "• Löschen: Du kannst fast alles (Karten in Listen, Decks in der Übersicht) durch langes Gedrückthalten und eine anschließende Bestätigung dauerhaft löschen."
            )
            .setPositiveButton("Verstanden", null)
            .show()
    }
}

class HomeViewModelFactory(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao // Parameter hinzugefügt
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(cardDao, masterDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
