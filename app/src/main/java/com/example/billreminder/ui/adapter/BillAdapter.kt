package com.example.billreminder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billreminder.R
import com.example.billreminder.data.model.Bill
import com.example.billreminder.databinding.ItemBillBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class BillAdapter(private val onItemClick: (Bill) -> Unit) : ListAdapter<Bill, BillAdapter.BillViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class BillViewHolder(private val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(bill: Bill) {
            binding.apply {
                tvBillName.text = bill.name

                // --- 1. NAPRAWA LOGIKI JĘZYKA I WALUTY ---
                // Pobieramy ustawienia języka z aplikacji
                val appLocales = AppCompatDelegate.getApplicationLocales()

                // Jeśli lista jest pusta (start apki), bierzemy język systemu telefonu.
                // Jeśli nie jest pusta (użytkownik wybrał w menu), bierzemy ten wybrany.
                val currentLocale = if (!appLocales.isEmpty) appLocales.get(0) else Locale.getDefault()

                // Sprawdzamy czy to polski
                val isPolish = currentLocale?.language == "pl"

                // Ustawiamy formatowanie (PLN dla polskiego, USD dla reszty)
                val formatLocale = if (isPolish) Locale("pl", "PL") else Locale.US
                val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)
                tvBillAmount.text = currencyFormat.format(bill.amount)
                // -----------------------------------------

                // 2. Formatowanie Daty
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", formatLocale)

                // 3. Sprawdzanie terminów
                val today = System.currentTimeMillis()

                if (bill.dueDate < today) {
                    // Termin minął! Kolor CZERWONY
                    tvBillDate.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_red_dark))

                    tvBillDate.text = if (isPolish)
                        "! ZALEGŁE: ${dateFormat.format(Date(bill.dueDate))}"
                    else
                        "! OVERDUE: ${dateFormat.format(Date(bill.dueDate))}"

                } else {
                    // Termin w przyszłości - Kolor szary
                    tvBillDate.setTextColor(ContextCompat.getColor(root.context, R.color.text_secondary))

                    tvBillDate.text = if (isPolish)
                        "Termin: ${dateFormat.format(Date(bill.dueDate))}"
                    else
                        "Due: ${dateFormat.format(Date(bill.dueDate))}"
                }

                // 4. Kolory kategorii
                val colorRes = when (bill.category) {
                    "Subscriptions", "Subskrypcje" -> R.color.bill_subscriptions
                    "Utilities", "Rachunki (Prąd/Gaz)" -> R.color.bill_utilities
                    "Insurance", "Ubezpieczenia" -> R.color.bill_insurance
                    "Rent/Mortgage", "Czynsz/Kredyt" -> R.color.bill_rent
                    else -> R.color.bill_other
                }
                viewCategoryIndicator.setBackgroundColor(ContextCompat.getColor(root.context, colorRes))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Bill>() {
        override fun areItemsTheSame(oldItem: Bill, newItem: Bill) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bill, newItem: Bill) = oldItem == newItem
    }
}