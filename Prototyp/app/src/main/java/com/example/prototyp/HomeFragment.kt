package com.example.prototyp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prototyp.databinding.FragmentHomeBinding
import com.example.prototyp.prefs.ThemePrefs
import com.google.android.material.animation.AnimationUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
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

        // Button, um zur Wunschliste zu wechseln
        view.findViewById<Button>(R.id.btnWishlist).setOnClickListener {
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
