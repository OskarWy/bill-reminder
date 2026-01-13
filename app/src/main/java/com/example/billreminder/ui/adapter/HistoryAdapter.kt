package com.example.billreminder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billreminder.R
import com.example.billreminder.data.model.PaymentHistory
import com.example.billreminder.databinding.ItemHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<PaymentHistory, HistoryAdapter.HistoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PaymentHistory) {
            binding.tvHistoryName.text = item.billName

            // 1. Sprawdzamy język aplikacji (PL czy EN)
            val currentLang = AppCompatDelegate.getApplicationLocales().get(0)?.language
            val formatLocale = if (currentLang == "pl") Locale("pl", "PL") else Locale.US

            // 2. Formatujemy datę (żeby było "Jan" dla EN i "Sty" dla PL)
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", formatLocale)
            val dateStr = dateFormat.format(Date(item.paymentDate))

            // 3. TŁUMACZENIE: Tu była zmiana!
            // Zamiast "Zapłacono: ...", pobieramy tekst z pliku strings.xml
            binding.tvHistoryDate.text = binding.root.context.getString(R.string.history_paid_on, dateStr)

            // 4. Formatowanie waluty (zł vs $)
            val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)
            binding.tvHistoryAmount.text = currencyFormat.format(item.amount)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PaymentHistory>() {
        override fun areItemsTheSame(oldItem: PaymentHistory, newItem: PaymentHistory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PaymentHistory, newItem: PaymentHistory) = oldItem == newItem
    }
}