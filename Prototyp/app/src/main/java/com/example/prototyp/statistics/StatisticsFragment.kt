package com.example.prototyp.statistics

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(
            AppDatabase.getInstance(requireContext()).cardDao(),
            AppDatabase.getInstance(requireContext()).masterCardDao()
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
                binding.tvTopValuableCardsTitle.visibility = if (top5.isEmpty()) View.GONE else View.VISIBLE
                valuableCardAdapter.submitList(top5)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
