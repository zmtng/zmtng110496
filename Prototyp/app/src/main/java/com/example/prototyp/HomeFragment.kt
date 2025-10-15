package com.example.prototyp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.example.prototyp.wishlist.WishlistFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.prototyp.externalCollection.*
import com.example.prototyp.externalWishlist.ExternalWishlistDao
import com.example.prototyp.externalWishlist.ExternalWishlistOverviewFragment
import com.example.prototyp.statistics.SetCompletionStat
import com.example.prototyp.statistics.StatisticsFragment
import com.example.prototyp.wishlist.WishlistDao
import kotlin.math.abs

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        HomeViewModelFactory(
            database.cardDao(),
            database.masterCardDao(),
            database.wishlistDao(),
            database.externalWishlistDao()
        )
    }

    private var importType: ImportType? = null
    enum class ImportType { COLLECTION, WISHLIST }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            when (importType) {
                ImportType.COLLECTION -> showImportCollectionTargetDialog(it)
                ImportType.WISHLIST -> showImportWishlistTargetDialog(it)
                null -> Toast.makeText(requireContext(), "Import-Typ unklar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private lateinit var dashboardAdapter: DashboardAdapter



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupDashboard()

        viewModel.loadDashboardItems(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userMessage.collectLatest { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.onUserMessageShown()
                        }
                    }
                }

                launch {
                    viewModel.dashboardItems.collectLatest { items ->
                        dashboardAdapter.submitList(items)
                    }
                }

                launch {
                    viewModel.setCompletionStats.collectLatest { stats ->
                        updateCompletionProgressBar(stats)
                    }
                }
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

    private fun setupDashboard() {
        dashboardAdapter = DashboardAdapter { item ->
            // Klick-Logik für alle Kacheln
            if (item.destination != null) {
                // Navigation zu einem Fragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, item.destination.newInstance())
                    .addToBackStack(null)
                    .commit()
            } else {
                // Ausführung einer Aktion
                when (item.id) {
                    "calculate_value" -> viewModel.updateTotalValue()
                    "info" -> showInfoDialog()
                    "import_collection" -> {
                        importType = ImportType.COLLECTION
                        importLauncher.launch("*/*")
                    }
                    "export_collection" -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.createCollectionCsvForSharing(requireContext())?.let { uri ->
                                shareCsv(uri, "Sammlung teilen via...")
                            }
                        }
                    }
                    "import_wishlist" -> {
                        importType = ImportType.WISHLIST
                        importLauncher.launch("*/*")
                    }
                    "export_wishlist" -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.createWishlistCsvForSharing(requireContext())?.let { uri ->
                                shareCsv(uri, "Wunschliste teilen via...")
                            }
                        }
                    }
                }
            }
        }

        binding.rvDashboard.apply {
            adapter = dashboardAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                viewModel.onDashboardItemsMoved(fromPosition, toPosition)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewModel.saveDashboardOrder(requireContext())
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvDashboard)
    }

    private fun updateCompletionProgressBar(stats: List<SetCompletionStat>) {
        // Container leeren, bevor er neu befüllt wird
        binding.completionProgressBarContainer.removeAllViews()

        if (stats.isEmpty()) return

        // Gesamtzahlen berechnen
        val totalOwned = stats.sumOf { it.ownedUniqueCards }
        val totalPossible = stats.sumOf { it.totalCardsInSet }
        val totalPercentage = if (totalPossible > 0) (totalOwned.toFloat() / totalPossible) * 100 else 0f

        // Text aktualisieren
        binding.tvTotalCompletionPercentage.text = "$totalOwned / $totalPossible Karten (${String.format("%.1f", totalPercentage)}%)"

        // Für jedes Set einen farbigen Balken-Teil erstellen
        stats.forEach { stat ->
            if (stat.ownedUniqueCards > 0) {
                val segment = View(requireContext())

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    stat.ownedUniqueCards.toFloat()
                )

                segment.layoutParams = params
                segment.setBackgroundColor(getColorForSet(stat.setName))
                binding.completionProgressBarContainer.addView(segment)
            }
        }

        // Einen grauen Balken für den fehlenden Rest hinzufügen
        val remainingCards = totalPossible - totalOwned
        if (remainingCards > 0) {
            val remainingSegment = View(requireContext())

            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                remainingCards.toFloat() // This is the weight
            )

            remainingSegment.layoutParams = params
            remainingSegment.setBackgroundColor(Color.LTGRAY)
            binding.completionProgressBarContainer.addView(remainingSegment)
        }
    }

    private fun getColorForSet(setName: String): Int {
        val hue = abs(setName.hashCode()) % 360f // Farbton (0-359)
        val saturation = 0.7f // Sättigung (0-1)
        val value = 0.9f // Helligkeit (0-1)
        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showImportWishlistTargetDialog(uri: Uri) {
        val options = arrayOf("In meine Wunschliste (überschreiben)", "Als neue externe Wunschliste")
        AlertDialog.Builder(requireContext())
            .setTitle("Wunschliste importieren nach...")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.importWishlistFromCsv(
                        uri,
                        requireContext(),
                        HomeViewModel.WishlistImportTarget.OWN_WISHLIST
                    )
                    1 -> showNameInputDialogForExternalWishlist(uri)
                }
            }
            .show()
    }

    private fun showNameInputDialogForExternalWishlist(uri: Uri) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Name für externe Wunschliste")
            .setView(input)
            .setPositiveButton("Importieren") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    viewModel.importWishlistFromCsv(
                        uri,
                        requireContext(),
                        HomeViewModel.WishlistImportTarget.EXTERNAL_WISHLIST,
                        name
                    )
                }
            }
            .setNegativeButton("Abbrechen", null)
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
                        "• Wert berechnen: Ruft die aktuellen Preise (Preis-Trend von Cardmarket) für alle Karten deiner Sammlung ab und zeigt den Gesamtwert an.\n\n" +
                        "• Import & Export: Importiere oder exportiere deine Sammlung und Wunschliste als CSV-Datei. Ideal für Backups oder den Austausch mit Freunden.\n\n" +
                        "--- SAMMLUNG ---\n\n" +
                        "• Karten hinzufügen: Nutze den Plus-Button (+), um neue Karten über die Suchfunktion zu deiner Sammlung hinzuzufügen.\n\n" +
                        "• Preise aktualisieren: Mit dem Aktualisieren-Button (Pfeil im Kreis) kannst du die Preise für deine gesamte Sammlung auf den neuesten Stand bringen.\n\n" +
                        "• Notizen & Details: Tippe eine Karte in der Liste an, um persönliche Notizen hinzuzufügen oder den Einzelpreis der Karte manuell abzurufen.\n\n" +
                        "--- DECKS ---\n\n" +
                        "• Deck erstellen & Importieren: Erstelle eigene Decks von Grund auf oder importiere sie direkt aus 'Piltover's Archive' über den TTS-Code.\n\n" +
                        "• Status-Anzeige: In der Deckansicht siehst du auf einen Blick, wie viele Exemplare einer Karte du besitzt (grüne Pille) und ob sie auf deiner Wunschliste steht (gelbe Pille).\n\n" +
                        "• Zur Wunschliste: Mit dem Stern-Symbol kannst du eine fehlende Karte direkt auf deine Wunschliste setzen.\n\n" +
                        "--- WUNSCHLISTE ---\n\n" +
                        "• In Sammlung übertragen: Tippe auf den 'In Sammlung'-Button, um eine Karte von deiner Wunschliste in deine Sammlung zu verschieben.\n\n" +
                        "--- EXTERNE LISTEN ---\n\n" +
                        "• Sammlungen & Wunschlisten importieren: Unter 'Externe Sammlungen' und 'Externe Wunschlisten' kannst du Listen von Freunden als CSV importieren, um sie anzusehen und zu vergleichen.\n\n" +
                        "--- TRADE-FINDER ---\n\n" +
                        "• Tauschgeschäfte finden: Dieses mächtige Werkzeug vergleicht deine Listen mit den importierten Listen deiner Freunde und zeigt dir potenzielle Tauschmöglichkeiten an:\n" +
                        "  - 'Sie haben, was du willst': Vergleicht eine externe Sammlung mit deiner Wunschliste.\n" +
                        "  - 'Du hast, was sie wollen': Vergleicht eine externe Wunschliste mit deiner Sammlung.\n\n" +
                        "--- STATISTIKEN ---\n\n" +
                        "• Wertentwicklung: Verfolge den Gesamtwert deiner Sammlung über die Zeit in einem Liniendiagramm.\n\n" +
                        "• Sammlungs-Analyse: Erhalte Einblicke in deine Sammlung mit Diagrammen zur Farb- und Set-Verteilung sowie einer Liste deiner wertvollsten Karten.\n\n" +
                        "--- ALLGEMEIN ---\n\n" +
                        "• Löschen: Du kannst fast alles (Karten in Listen, Decks, externe Listen) durch langes Gedrückthalten und eine anschließende Bestätigung dauerhaft löschen."
            )
            .setPositiveButton("Verstanden", null)
            .show()
    }

    private fun shareCsv(fileUri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, title))
    }

    private fun showImportCollectionTargetDialog(uri: Uri) {
        val options = arrayOf("In meine Sammlung (überschreiben)", "Als neue externe Sammlung")
        AlertDialog.Builder(requireContext())
            .setTitle("Sammlung importieren nach...")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.importCollection(uri, requireContext())
                    1 -> showNameInputDialogForExternal(uri) // <-- KORREKTER, DIREKTER AUFRUF
                }
            }
            .show()
    }
}

class HomeViewModelFactory(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao,
    private val externalWishlistDao: ExternalWishlistDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(cardDao, masterDao, wishlistDao, externalWishlistDao) as T // <-- NEU
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
