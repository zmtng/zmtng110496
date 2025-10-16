package com.example.prototyp.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.AppDatabase
import com.example.prototyp.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        StatisticsViewModelFactory(
            db.cardDao(),
            db.masterCardDao(),
            db.priceHistoryDao(),
            db.totalValueHistoryDao()
        )
    }

    private lateinit var setCompletionAdapter: SetCompletionAdapter
    private lateinit var valuableCardAdapter: ValuableCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Set Completion RecyclerView
        setCompletionAdapter = SetCompletionAdapter()
        binding.rvSetCompletion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSetCompletion.adapter = setCompletionAdapter

        // Top Valuable Cards RecyclerView
        valuableCardAdapter = ValuableCardAdapter()
        binding.rvTopValuableCards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTopValuableCards.adapter = valuableCardAdapter

        // Chart Setup and Data Loading
        setupChart()
        lifecycleScope.launch {
            viewModel.valueHistoryChartData.collectLatest { chartData: List<Entry> ->
                // The corrected line is below
                if (chartData.isNotEmpty() && view?.isShown == true) {
                    updateChart(chartData)
                }
            }
        }
    }

    private fun setupChart() {
        binding.valueHistoryChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // X-Axis Styling
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textColor = Color.GRAY
            xAxis.valueFormatter = DateAxisValueFormatter()

            // Y-Axis Styling
            axisLeft.textColor = Color.GRAY
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
        }
    }

    private fun updateChart(chartData: List<Entry>) {
        // Create a DataSet from the data points
        val dataSet = LineDataSet(chartData, "Wertverlauf").apply {
            // Style the line
            color = Color.CYAN
            valueTextColor = Color.WHITE
            setCircleColor(Color.CYAN)
            circleHoleColor = Color.CYAN
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.STEPPED
        }

        val lineData = LineData(dataSet)

        binding.valueHistoryChart.data = lineData
        binding.valueHistoryChart.invalidate()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setCompletionStats.collectLatest { stats ->
                setCompletionAdapter.submitList(stats)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topValuableCards.collectLatest { cards ->
                val top5 = cards.take(5)

                valuableCardAdapter.submitList(top5)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DateAxisValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("dd. MMM", Locale.getDefault())
        override fun getFormattedValue(value: Float): String {
            return dateFormat.format(Date(value.toLong()))
        }
    }
}