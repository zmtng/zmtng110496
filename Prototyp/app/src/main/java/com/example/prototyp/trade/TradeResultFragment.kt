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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prototyp.AppDatabase
import com.example.prototyp.databinding.FragmentTradeResultBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TradeResultFragment : Fragment() {

    private var _binding: FragmentTradeResultBinding? = null
    private val binding get() = _binding!!

    private val tradeType: String by lazy { requireArguments().getString(ARG_TRADE_TYPE)!! }
    private val list1Id: Int by lazy { requireArguments().getInt(ARG_LIST1_ID) }
    private val list2Id: Int by lazy { requireArguments().getInt(ARG_LIST2_ID) }
    private val title: String by lazy { requireArguments().getString(ARG_TITLE)!! }

    private val viewModel: TradeResultViewModel by viewModels {
        TradeResultViewModelFactory(AppDatabase.getInstance(requireContext()).tradeDao(), tradeType, list1Id, list2Id)
    }

    private lateinit var tradeAdapter: TradeCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTradeResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Tausch-Ergebnis"
        binding.tvResultTitle.text = title

        tradeAdapter = TradeCardAdapter().apply {
            setTradeType(tradeType)
        }
        binding.rvTradeResults.adapter = tradeAdapter
        binding.rvTradeResults.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tradeResults.collectLatest { results ->
                binding.tvEmptyResult.isVisible = results.isEmpty()
                tradeAdapter.submitList(results)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRADE_TYPE = "trade_type"
        private const val ARG_LIST1_ID = "list1_id"
        private const val ARG_LIST2_ID = "list2_id"
        private const val ARG_TITLE = "title"

        fun newInstance(tradeType: String, list1Id: Int, list2Id: Int, title: String) = TradeResultFragment().apply {
            arguments = bundleOf(
                ARG_TRADE_TYPE to tradeType,
                ARG_LIST1_ID to list1Id,
                ARG_LIST2_ID to list2Id,
                ARG_TITLE to title
            )
        }
    }
}
