package com.example.prototyp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.prototyp.data.db.CardDao
import com.example.prototyp.databinding.FragmentHomeBinding
import com.example.prototyp.externalCollection.ExternalCollectionOverviewViewModel
import com.example.prototyp.externalCollection.ExternalCollectionOverviewViewModelFactory
import com.example.prototyp.externalWishlist.ExternalWishlistDao
import com.example.prototyp.statistics.SetCompletionStat
import com.example.prototyp.statistics.TotalValueHistoryDao
import com.example.prototyp.wishlist.WishlistDao
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        HomeViewModelFactory(
            database.cardDao(),
            database.masterCardDao(),
            database.wishlistDao(),
            database.externalWishlistDao(),
            database.totalValueHistoryDao()
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
        setupClickListeners() // NEU
        viewModel.loadDashboardItems(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // User Messages beobachten
                launch {
                    viewModel.userMessage.collectLatest { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.onUserMessageShown()
                        }
                    }
                }

                // Dashboard Items beobachten
                launch {
                    viewModel.dashboardItems.collectLatest { items ->
                        dashboardAdapter.submitList(items)
                    }
                }

                // Den neuen kombinierten State beobachten
                launch {
                    viewModel.overviewStats.collectLatest { stats ->
                        updateOverviewCard(stats)
                        updateCompletionProgressBar(stats.setCompletionStats)
                    }
                }
            }
        }
    }

    // Klick-Listener an einem Ort bündeln
    private fun setupClickListeners() {
        binding.fabAddCardToCollection.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardToCollectionFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // Funktion zum Aktualisieren der neuen Übersichtskarte
    private fun updateOverviewCard(stats: HomeOverviewStats) {
        binding.overviewCard.tvTotalValue.text = String.format("%.2f €", stats.totalCollectionValue)
        binding.overviewCard.tvWishlistValue.text = String.format("%.2f €", stats.totalWishlistValue)
        binding.overviewCard.tvTotalCardCount.text = "${stats.totalCardCount} Karten (Gesamt)"

        val valueChangeText = binding.overviewCard.tvTotalValueChange
        when {
            stats.valueChange > 0.005 -> {
                valueChangeText.text = String.format("+%.2f €", stats.valueChange)
                valueChangeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_green))
            }
            stats.valueChange < -0.005 -> {
                valueChangeText.text = String.format("%.2f €", stats.valueChange)
                valueChangeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_red))
            }
            else -> {
                valueChangeText.text = "±0,00 €"
                valueChangeText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
        }
    }

    private fun setupDashboard() {
        dashboardAdapter = DashboardAdapter { item ->
            if (item.destination != null) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, item.destination.newInstance())
                    .addToBackStack(null)
                    .commit()
            } else {
                when (item.id) {
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
        val container = binding.overviewCard.completionProgressBarContainer
        container.removeAllViews()

        if (stats.isEmpty()) return

        val totalOwned = stats.sumOf { it.ownedUniqueCards }
        val totalPossible = stats.sumOf { it.totalCardsInSet }

        // Text in der Übersichtskarte aktualisieren
        binding.overviewCard.tvTotalCompletionPercentage.text = "$totalOwned / $totalPossible Uniques"

        stats.forEach { stat ->
            if (stat.ownedUniqueCards > 0) {
                val segment = View(requireContext())
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, stat.ownedUniqueCards.toFloat())
                segment.layoutParams = params
                segment.setBackgroundColor(getColorForSet(stat.setName))
                container.addView(segment)
            }
        }

        val remainingCards = totalPossible - totalOwned
        if (remainingCards > 0) {
            val remainingSegment = View(requireContext())
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, remainingCards.toFloat())
            remainingSegment.layoutParams = params
            remainingSegment.setBackgroundColor(Color.LTGRAY)
            container.addView(remainingSegment)
        }
    }

    private fun getColorForSet(setName: String): Int {
        val hue = abs(setName.hashCode()) % 360f
        val saturation = 0.7f
        val value = 0.9f
        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Die Dialog-Funktionen bleiben wie sie sind ---
    private fun showImportWishlistTargetDialog(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_options, null)
        val optionOverwrite: MaterialCardView = dialogView.findViewById(R.id.option_overwrite)
        val optionExternal: MaterialCardView = dialogView.findViewById(R.id.option_external)
        val tvOverwriteTitle: TextView = dialogView.findViewById(R.id.tv_option_overwrite_title)
        val tvOverwriteSubtitle: TextView = dialogView.findViewById(R.id.tv_option_overwrite_subtitle)
        val tvExternalTitle: TextView = dialogView.findViewById(R.id.tv_option_external_title)
        val tvExternalSubtitle: TextView = dialogView.findViewById(R.id.tv_option_external_subtitle)

        tvOverwriteTitle.text = "In meine Wunschliste importieren"
        tvOverwriteSubtitle.text = "⚠️ Dies überschreibt deine aktuelle Wunschliste."
        tvExternalTitle.text = "Als neue externe Wunschliste anlegen"
        tvExternalSubtitle.text = "Erstellt einen neuen, separaten Eintrag."

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Wunschliste importieren nach...")
            .setView(dialogView)
            .setNegativeButton("Abbrechen", null)
            .create()

        optionOverwrite.setOnClickListener {
            viewModel.importWishlistFromCsv(
                uri,
                requireContext(),
                HomeViewModel.WishlistImportTarget.OWN_WISHLIST
            )
            dialog.dismiss()
        }
        optionExternal.setOnClickListener {
            showNameInputDialogForExternalWishlist(uri)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showNameInputDialogForExternalWishlist(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_name_input, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.text_input_layout)
        val input = dialogView.findViewById<TextInputEditText>(R.id.edit_text_name)
        textInputLayout.hint = "Name für externe Wunschliste"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Name eingeben")
            .setView(dialogView)
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

    private fun showNameInputDialogForExternal(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_name_input, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.text_input_layout)
        val input = dialogView.findViewById<TextInputEditText>(R.id.edit_text_name)
        textInputLayout.hint = "Name für externe Sammlung"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Name eingeben")
            .setView(dialogView)
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
            .setMessage("...") // Gekürzt für die Antwort, dein Text bleibt hier
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_options, null)
        val optionOverwrite: MaterialCardView = dialogView.findViewById(R.id.option_overwrite)
        val optionExternal: MaterialCardView = dialogView.findViewById(R.id.option_external)
        val tvOverwriteTitle: TextView = dialogView.findViewById(R.id.tv_option_overwrite_title)
        val tvOverwriteSubtitle: TextView = dialogView.findViewById(R.id.tv_option_overwrite_subtitle)
        val tvExternalTitle: TextView = dialogView.findViewById(R.id.tv_option_external_title)
        val tvExternalSubtitle: TextView = dialogView.findViewById(R.id.tv_option_external_subtitle)
        tvOverwriteTitle.text = "In meine Sammlung importieren"
        tvOverwriteSubtitle.text = "⚠️ Dies überschreibt deine aktuelle Sammlung."
        tvExternalTitle.text = "Als neue externe Sammlung anlegen"
        tvExternalSubtitle.text = "Erstellt einen neuen, separaten Eintrag."

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sammlung importieren nach...")
            .setView(dialogView)
            .setNegativeButton("Abbrechen", null)
            .create()

        optionOverwrite.setOnClickListener {
            viewModel.importCollection(uri, requireContext())
            dialog.dismiss()
        }
        optionExternal.setOnClickListener {
            showNameInputDialogForExternal(uri)
            dialog.dismiss()
        }
        dialog.show()
    }
}

class HomeViewModelFactory(
    private val cardDao: CardDao,
    private val masterDao: MasterCardDao,
    private val wishlistDao: WishlistDao,
    private val externalWishlistDao: ExternalWishlistDao,
    private val totalValueHistoryDao: TotalValueHistoryDao // NEU
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(cardDao, masterDao, wishlistDao, externalWishlistDao, totalValueHistoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
