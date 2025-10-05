package com.example.prototyp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prototyp.data.db.CardDao
import com.example.yourapp.data.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

class AddCardFragment : Fragment(R.layout.fragment_add_card) {

    // DAOs
    private lateinit var cardDao: CardDao
    private lateinit var masterDao: MasterCardDao

    // UI
    private lateinit var inputSet: MaterialAutoCompleteTextView
    private lateinit var inputNumber: TextInputEditText
    private lateinit var inputName: AutoCompleteTextView
    private lateinit var inputQuantity: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // Name-Suggestions
    private data class NameSuggestion(
        val cardName: String,
        val setCode: String,
        val setName: String,
        val cardNumber: Int
    ) {
        override fun toString(): String =
            "$cardName — $setName ($setCode) • #$cardNumber"
    }

    private lateinit var nameAdapter: ArrayAdapter<NameSuggestion>
    private var suggestJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        cardDao = db.cardDao()
        masterDao = db.masterCardDao()

        inputSet = view.findViewById(R.id.inputSet)
        inputNumber = view.findViewById(R.id.inputNumber)
        inputName = view.findViewById(R.id.inputName)
        inputQuantity = view.findViewById(R.id.inputQuantity)
        btnSave = view.findViewById(R.id.btnSaveCard)
        btnCancel = view.findViewById(R.id.btnCancel)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val c = masterDao.count()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Master rows: $c", Toast.LENGTH_SHORT).show()
            }
        }

        setupNameAutosuggest()
        setupNumberAutoFill()
        setupButtons()

        parentFragmentManager.setFragmentResultListener("scan_result", viewLifecycleOwner) { _, b ->
            val scanned = b.getString("card_name").orEmpty()
            if (scanned.isNotBlank()) {
                inputName.setText(scanned)
                // Wenn ein Set bereits gewählt ist, versuche Nummer zu finden
                tryFillNumberFromSetAndName()
            }
        }

        view.findViewById<FloatingActionButton>(R.id.fabScan).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CameraFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // -------------------- Autosuggest (Name) --------------------

    private fun setupNameAutosuggest() {
        nameAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<NameSuggestion>()
        )
        inputName.setAdapter(nameAdapter)
        inputName.threshold = 2

        inputName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim().orEmpty()
                suggestJob?.cancel()
                if (q.length < 2) {
                    nameAdapter.clear()
                    return
                }
                suggestJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    delay(200) // debounce
                    val rows = masterDao.search(q) // nutzt deine bestehende Query
                    val suggestions = rows
                        .asSequence()
                        .map { NameSuggestion(it.cardName, it.setCode, it.setName, it.cardNumber) }
                        .distinct() // gleiche Zeilen vermeiden
                        .take(50)
                        .toList()

                    withContext(Dispatchers.Main) {
                        nameAdapter.clear()
                        nameAdapter.addAll(suggestions)
                        if (inputName.hasFocus()) inputName.showDropDown()
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputName.setOnItemClickListener { parent, _, position, _ ->
            val s = parent.getItemAtPosition(position) as? NameSuggestion ?: return@setOnItemClickListener
            // Felder vorbefüllen
            inputName.setText(s.cardName, false)
            inputNumber.setText(s.cardNumber.toString())
            inputSet.setText("${s.setName} (${s.setCode})", false)
        }
    }

    // -------------------- Autofill Name/Nummer ↔ Set --------------------

    private fun setupNumberAutoFill() {
        inputNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tryFillNameFromSetAndNumber()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun tryFillNumberFromSetAndName() {
        val set = currentSetCodeOrNull() ?: return
        val name = inputName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val num = masterDao.getNumberForName(set, name)
            withContext(Dispatchers.Main) { if (num != null) inputNumber.setText(num.toString()) }
        }
    }

    private fun tryFillNameFromSetAndNumber() {
        val set = currentSetCodeOrNull() ?: return
        val number = inputNumber.text?.toString()?.toIntOrNull() ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val card = masterDao.getBySetAndNumber(set, number)
            withContext(Dispatchers.Main) { card?.let { inputName.setText(it.cardName, false) } }
        }
    }

    private fun currentSetCodeOrNull(): String? {
        val text = inputSet.text?.toString()?.trim().orEmpty()
        // erwartet Format: "Setname (CODE)"
        val m = Regex("\\(([^)]+)\\)$").find(text) ?: return null
        return m.groupValues.getOrNull(1)
    }

    // -------------------- Speichern / Abbrechen --------------------

    private fun setupButtons() {
        btnSave.setOnClickListener {
            val setCode = currentSetCodeOrNull()
            val number = inputNumber.text?.toString()?.toIntOrNull()
            val cardName = inputName.text?.toString()?.trim().orEmpty()
            val quantity = inputQuantity.text?.toString()?.toIntOrNull()

            if (setCode.isNullOrEmpty() || number == null || number <= 0 || cardName.isEmpty() || quantity == null || quantity <= 0) {
                Toast.makeText(requireContext(), "Bitte gültige Eingaben machen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val master = masterDao.getBySetAndNumber(setCode, number)
                if (master == null || !master.cardName.equals(cardName, ignoreCase = true)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Karte nicht in der Masterliste gefunden", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Farbe aus Master übernehmen
                val color = master.color.ifBlank { "R" }

                // In Sammlung hochzählen / einfügen
                cardDao.upsertBySetAndNumber(setCode, number, delta = quantity, color = color)

                withContext(Dispatchers.Main) { parentFragmentManager.popBackStack() }
            }
        }

        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
    }
}
