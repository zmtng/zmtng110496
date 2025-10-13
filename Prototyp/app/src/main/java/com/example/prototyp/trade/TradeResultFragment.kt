package com.example.prototyp.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.prototyp.AppDatabase
import com.example.prototyp.databinding.FragmentTradeResultBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TradeResultFragment : Fragment() {
    private var _binding: FragmentTradeResultBinding? = null
    private val binding get() = _binding!!

    // Enum zur Unterscheidung der Vergleichsart
    enum class ComparisonType { THEY_HAVE, YOU_HAVE }

    private val comparisonId: Int by lazy { requireArguments().getInt(ARG_ID) }
    private val comparisonType: ComparisonType by lazy {
        requireArguments().getSerializable(ARG_TYPE) as ComparisonType
    }

    private val viewModel: TradeFinderViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        TradeFinderViewModelFactory(db.tradeDao(), db.externalCollectionDao(), db.externalWishlistDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTradeResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapter wird jetzt ohne Parameter erstellt
        val tradeAdapter = TradeCardAdapter()
        binding.rvTradeResults.adapter = tradeAdapter

        when (comparisonType) {
            ComparisonType.THEY_HAVE -> {
                binding.tvResultTitle.text = "Sie haben, was du willst:"
                // Die Zuweisung funktioniert jetzt, da quantityTextTemplate eine öffentliche Variable ist
                tradeAdapter.quantityTextTemplate = "Sie haben: %d / Du willst: %d"
                observeData(viewModel.getTheyHave(comparisonId))
            }
            ComparisonType.YOU_HAVE -> {
                binding.tvResultTitle.text = "Du hast, was sie wollen:"
                // Die Zuweisung funktioniert jetzt
                tradeAdapter.quantityTextTemplate = "Du hast: %d / Sie wollen: %d"
                observeData(viewModel.getYouHave(comparisonId))
            }
        }
    }

    private fun observeData(dataFlow: kotlinx.coroutines.flow.Flow<List<TradeDao.TradeCard>>) {
        viewLifecycleOwner.lifecycleScope.launch {
            dataFlow.collectLatest { results ->
                (binding.rvTradeResults.adapter as TradeCardAdapter).submitList(results)
                binding.tvEmptyResult.isVisible = results.isEmpty()
                binding.rvTradeResults.isVisible = results.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "comparison_id"
        private const val ARG_TYPE = "comparison_type"

        fun newInstance(id: Int, type: ComparisonType) = TradeResultFragment().apply {
            arguments = bundleOf(
                ARG_ID to id,
                ARG_TYPE to type
            )
        }
    }
}