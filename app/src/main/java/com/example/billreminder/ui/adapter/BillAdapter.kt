package com.example.billreminder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
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

                // --- POPRAWKA WALUTY "NA TWARDO" ---
                // Sprawdzamy, jaki język jest aktualnie ustawiony w aplikacji
                val currentLang = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language

                // Jeśli polski -> wymuś Polskę (zł), w przeciwnym razie USA ($)
                val formatLocale = if (currentLang == "pl") Locale("pl", "PL") else Locale.US
                val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)

                tvBillAmount.text = currencyFormat.format(bill.amount)
                // ------------------------------------

                // Data też musi korzystać z tego samego Locale
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", formatLocale)
                tvBillDate.text = if (currentLang == "pl")
                    "Termin: ${dateFormat.format(Date(bill.dueDate))}"
                else
                    "Due: ${dateFormat.format(Date(bill.dueDate))}"

                // Kolory kategorii (z obsługą polskich nazw)
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