package com.example.billreminder.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.billreminder.R
import com.example.billreminder.data.model.Bill
import com.example.billreminder.databinding.ActivityAddEditBillBinding
import java.text.SimpleDateFormat
import java.util.*

class AddEditBillActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBillBinding
    private var selectedDateInMillis: Long = System.currentTimeMillis()
    private var editBillId: Long = 0L

    private val frequencyValues = arrayOf(0, 1, 3, 6, 12)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBillBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOWE: Obsługa strzałki cofania na Toolbarze
        binding.toolbar.setNavigationOnClickListener {
            finish() // Zamyka ekran i wraca do poprzedniego
        }

        setupSpinners()
        setupDatePicker()

        if (intent.hasExtra(EXTRA_BILL)) {
            // Edycja: Zmieniamy tytuł na Toolbarze
            binding.toolbar.title = getString(R.string.title_edit_bill)

            val bill = intent.getParcelableExtra<Bill>(EXTRA_BILL)
            bill?.let {
                editBillId = it.id
                binding.etName.setText(it.name)
                binding.etAmount.setText(it.amount.toString())
                binding.etNotes.setText(it.notes)
                selectedDateInMillis = it.dueDate
                updateDateText()

                val categories = resources.getStringArray(R.array.categories_array)
                val catIndex = categories.indexOf(it.category)
                if (catIndex >= 0) binding.spinnerCategory.setSelection(catIndex)

                val freqIndex = frequencyValues.indexOf(it.frequency)
                if (freqIndex >= 0) binding.spinnerFrequency.setSelection(freqIndex)
            }
        } else {
            // Dodawanie: Tytuł domyślny
            binding.toolbar.title = getString(R.string.btn_add_bill) // Lub inny string np. "Nowy rachunek"
            updateDateText()
        }

        binding.btnSave.setOnClickListener { saveBill() }
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            this, R.array.categories_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        }

        val freqOptions = arrayOf(
            getString(R.string.freq_once),
            getString(R.string.freq_monthly),
            getString(R.string.freq_quarterly),
            getString(R.string.freq_half_year),
            getString(R.string.freq_yearly)
        )

        ArrayAdapter(
            this, android.R.layout.simple_spinner_item, freqOptions
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFrequency.adapter = adapter
        }
    }

    private fun setupDatePicker() {
        binding.btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDateInMillis
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val newCalendar = Calendar.getInstance()
                    newCalendar.set(year, month, day)
                    selectedDateInMillis = newCalendar.timeInMillis
                    updateDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateText() {
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = format.format(Date(selectedDateInMillis))
    }

    private fun saveBill() {
        val name = binding.etName.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val notes = binding.etNotes.text.toString().trim()

        val freqIndex = binding.spinnerFrequency.selectedItemPosition
        val frequency = frequencyValues[freqIndex]

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_name_required)
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = getString(R.string.error_amount_required)
            return
        }

        val bill = Bill(
            id = editBillId,
            name = name,
            amount = amount,
            dueDate = selectedDateInMillis,
            category = category,
            notes = notes,
            frequency = frequency
        )

        val data = Intent().apply { putExtra(EXTRA_BILL, bill) }
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val EXTRA_BILL = "com.example.billreminder.EXTRA_BILL"
    }
}