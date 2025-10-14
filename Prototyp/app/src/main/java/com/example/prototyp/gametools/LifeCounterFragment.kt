package com.example.prototyp.gametools

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.prototyp.R
import com.example.prototyp.databinding.FragmentLifeCounterBinding
import com.example.prototyp.databinding.ViewMightCounterBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LifeCounterFragment : Fragment(R.layout.fragment_life_counter) {

    private var _binding: FragmentLifeCounterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LifeCounterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLifeCounterBinding.bind(view)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Lebenspunkte
        binding.btnP1LifePlus.setOnClickListener { viewModel.incrementP1Life() }
        binding.btnP1LifeMinus.setOnClickListener { viewModel.decrementP1Life() }
        binding.btnP2LifePlus.setOnClickListener { viewModel.incrementP2Life() }
        binding.btnP2LifeMinus.setOnClickListener { viewModel.decrementP2Life() }
        binding.btnReset.setOnClickListener { viewModel.reset() }

        // Might Counter
        setupMightCounterListeners(binding.mightP1Left, viewModel::incrementP1MightLeft, viewModel::decrementP1MightLeft)
        setupMightCounterListeners(binding.mightP1Right, viewModel::incrementP1MightRight, viewModel::decrementP1MightRight)
        setupMightCounterListeners(binding.mightP2Left, viewModel::incrementP2MightLeft, viewModel::decrementP2MightLeft)
        setupMightCounterListeners(binding.mightP2Right, viewModel::incrementP2MightRight, viewModel::decrementP2MightRight)
    }

    private fun setupMightCounterListeners(
        mightBinding: ViewMightCounterBinding,
        onIncrement: () -> Unit,
        onDecrement: () -> Unit
    ) {
        mightBinding.btnMightPlus.setOnClickListener { onIncrement() }
        mightBinding.btnMightMinus.setOnClickListener { onDecrement() }
    }

    private fun observeViewModel() {
        // Lebenspunkte
        observeFlow(viewModel.p1Life) { binding.tvP1Life.text = it.toString() }
        observeFlow(viewModel.p2Life) { binding.tvP2Life.text = it.toString() }

        // Might Counter
        observeFlow(viewModel.p1MightLeft) { binding.mightP1Left.tvMightValue.text = it.toString() }
        observeFlow(viewModel.p1MightRight) { binding.mightP1Right.tvMightValue.text = it.toString() }
        observeFlow(viewModel.p2MightLeft) { binding.mightP2Left.tvMightValue.text = it.toString() }
        observeFlow(viewModel.p2MightRight) { binding.mightP2Right.tvMightValue.text = it.toString() }
    }

    private fun <T> observeFlow(flow: kotlinx.coroutines.flow.StateFlow<T>, action: (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest { value ->
                action(value)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}