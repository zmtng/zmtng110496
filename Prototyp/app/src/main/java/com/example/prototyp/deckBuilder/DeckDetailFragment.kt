package com.example.prototyp.deckBuilder

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.AppDatabase
import com.example.prototyp.R
import com.example.prototyp.databinding.FragmentDeckDetailBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckDetailFragment : Fragment() {

    private var _binding: FragmentDeckDetailBinding? = null
    private val binding get() = _binding!!

    private val deckId: Int by lazy { requireArguments().getInt(ARG_DECK_ID) }

    private val viewModel: DeckDetailViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        DeckDetailViewModelFactory(db.deckDao(), db.masterCardDao(), db.wishlistDao(), db.cardDao())
    }
    private lateinit var cardAdapter: DeckCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeckDetailBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setDeckId(deckId)

        setupRecyclerView()
        setupBottomSheet()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        cardAdapter = DeckCardAdapter(
            onIncrement = { card -> viewModel.incrementCardInDeck(card) },
            onDecrement = { card -> viewModel.decrementCardInDeck(card) },
            onAddToWishlist = { card ->
                viewModel.addCardToWishlist(card)
                Toast.makeText(
                    requireContext(),
                    "'${card.cardName}' zur Wunschliste hinzugefügt",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onLongClick = { showDeleteConfirmationDialog(it) }
        )
        binding.rvDeckCards.adapter = cardAdapter
        binding.rvDeckCards.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetContainer)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        binding.analysisSummaryBar.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setupClickListeners() {
        binding.fabAddCardToDeck.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCardToDeckFragment())
                .addToBackStack(null)
                .commit()
        }

        // Find the button inside the included layout
        binding.bottomSheetContainer.findViewById<ImageButton>(R.id.btnUpdatePrices).setOnClickListener {
            viewModel.fetchAllDeckPrices()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deckContents.collectLatest { cards ->
                cardAdapter.submitList(cards)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deckStats.collectLatest { stats ->
                // Update summary bar
                binding.tvTotalCards.text = "Karten: ${stats.totalCards}"
                binding.tvDeckValue.text = String.format("%.2f €", stats.totalValue)

                // Find views inside the bottom sheet
                val bottomSheetView = binding.bottomSheetContainer
                val pieChart = bottomSheetView.findViewById<PieChartView>(R.id.pieChart)
                val tvDeckValueDetail = bottomSheetView.findViewById<TextView>(R.id.tvDeckValueDetail)
                val tvTotalCardsDetail = bottomSheetView.findViewById<TextView>(R.id.tvTotalCardsDetail)

                // Update views inside the bottom sheet
                tvDeckValueDetail.text = "Gesamtwert: ${String.format("%.2f €", stats.totalValue)}"
                tvTotalCardsDetail.text = "Gesamtkarten: ${stats.totalCards}"
                pieChart.setData(stats.colorDistribution)
                updateLegend(stats.colorDistribution)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.onUserMessageShown()
                }
            }
        }
    }

    private fun updateLegend(distribution: Map<String, Float>) {
        val legendLayout = binding.bottomSheetContainer.findViewById<LinearLayout>(R.id.legendLayout)
        legendLayout.removeAllViews()

        // Get the resolved color for text from the current theme
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val textColor = typedValue.data

        distribution.entries.sortedByDescending { it.value }.forEach { (colorCode, percentage) ->
            val legendItem = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (4 * resources.displayMetrics.density).toInt() }
            }

            val colorBox = View(requireContext()).apply {
                val size = (16 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = (8 * resources.displayMetrics.density).toInt() }
                val resourceId = getColorResource(colorCode)
                if (resourceId != 0) {
                    if (colorCode == "M") {
                        setBackgroundResource(resourceId)
                    } else {
                        setBackgroundColor(ContextCompat.getColor(context, resourceId))
                    }
                }
            }

            val textView = TextView(requireContext()).apply {
                text = "${getColorName(colorCode)}: ${String.format("%.1f%%", percentage * 100)}"
                setTextColor(textColor)
            }

            legendItem.addView(colorBox)
            legendItem.addView(textView)
            legendLayout.addView(legendItem)
        }
    }

    private fun showDeleteConfirmationDialog(card: DeckDao.DeckCardDetail) {
        AlertDialog.Builder(requireContext())
            .setTitle("Karte löschen")
            .setMessage("Möchtest du '${card.cardName}' wirklich aus dem Deck entfernen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteCard(card)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun getColorResource(colorCode: String): Int {
        return when (colorCode) {
            "R" -> R.color.card_red
            "B" -> R.color.card_blue
            "G" -> R.color.card_green
            "Y" -> R.color.card_yellow
            "P" -> R.color.card_purple
            "O" -> R.color.card_orange
            "U" -> R.color.card_grey
            "M" -> R.drawable.rainbow_gradient
            else -> 0
        }
    }

    private fun getColorName(colorCode: String): String {
        return when (colorCode) {
            "R" -> "Rot"
            "B" -> "Blau"
            "G" -> "Grün"
            "Y" -> "Gelb"
            "P" -> "Lila"
            "O" -> "Orange"
            "U" -> "Grau"
            "M" -> "Mehrfarbig"
            else -> "Unbekannt"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        fun newInstance(deckId: Int) = DeckDetailFragment().apply {
            arguments = bundleOf(ARG_DECK_ID to deckId)
        }
    }
}


class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var data: Map<String, Float> = emptyMap()

    fun setData(data: Map<String, Float>) {
        this.data = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = (width.coerceAtMost(height) / 2) * 0.85f
        val centerX = width / 2
        val centerY = height / 2

        rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        var startAngle = -90f

        val colors = data.keys.map { colorCode ->
            val resourceId = getColorResource(colorCode)
            if (colorCode == "M" || resourceId == 0) Color.GRAY else ContextCompat.getColor(context, resourceId)
        }.toIntArray()

        // Special handling for rainbow gradient
        if (data.containsKey("M")) {
            val rainbowColors = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.RED)
            paint.shader = SweepGradient(centerX, centerY, rainbowColors, null)
        }


        for ((colorCode, percentage) in data) {
            val sweepAngle = percentage * 360f

            val resourceId = getColorResource(colorCode)

            if (colorCode == "M") {
                val rainbowColors = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.RED)
                paint.shader = SweepGradient(centerX, centerY, rainbowColors, null)
            } else {
                paint.shader = null
                paint.color = if (resourceId != 0) ContextCompat.getColor(context, resourceId) else Color.GRAY
            }


            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }
    }

    private fun getColorResource(colorCode: String): Int {
        return when (colorCode) {
            "R" -> R.color.card_red
            "B" -> R.color.card_blue
            "G" -> R.color.card_green
            "Y" -> R.color.card_yellow
            "P" -> R.color.card_purple
            "O" -> R.color.card_orange
            "U" -> R.color.card_grey
            "M" -> R.drawable.rainbow_gradient
            else -> 0
        }
    }
}