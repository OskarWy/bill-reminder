package com.example.billreminder.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billreminder.R
import com.example.billreminder.databinding.ActivityHistoryBinding
import com.example.billreminder.ui.adapter.HistoryAdapter
import com.example.billreminder.ui.viewmodel.BillViewModel

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: BillViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ustawienie Paska
        setSupportActionBar(binding.toolbarHistory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 2. KLUCZOWA ZMIANA: Wymuszamy tytuł z tłumaczeń (PL/EN)
        supportActionBar?.title = getString(R.string.title_history)

        binding.toolbarHistory.setNavigationOnClickListener { finish() }

        val adapter = HistoryAdapter()
        // Uwaga: upewnij się, że w XML masz recyclerHistory (czasem nazywa się recyclerViewHistory)
        // Jeśli będzie błąd na czerwono, zmień na binding.recyclerViewHistory
        binding.recyclerHistory.adapter = adapter
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)

        // Obserwujemy listę historii
        viewModel.historyList.observe(this) { history ->
            adapter.submitList(history)

            // Obsługa pustego widoku (też tłumaczymy tekst!)
            if (history.isEmpty()) {
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.tvEmptyHistory.text = getString(R.string.history_empty) // Tłumaczenie "Brak historii"
                binding.recyclerHistory.visibility = View.GONE
            } else {
                binding.tvEmptyHistory.visibility = View.GONE
                binding.recyclerHistory.visibility = View.VISIBLE
            }
        }
    }
}