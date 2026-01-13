package com.example.billreminder.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billreminder.R
import com.example.billreminder.data.model.Bill
import com.example.billreminder.databinding.ActivitySubscriptionsBinding
import com.example.billreminder.ui.adapter.BillAdapter
import com.example.billreminder.ui.viewmodel.BillViewModel
import java.text.NumberFormat
import java.util.*

class SubscriptionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionsBinding
    private val viewModel: BillViewModel by viewModels()

    // Tworzymy 4 adaptery - po jednym dla każdej sekcji
    private val adapterMonthly = BillAdapter { onBillClick(it) }
    private val adapterQuarterly = BillAdapter { onBillClick(it) }
    private val adapterHalfYearly = BillAdapter { onBillClick(it) }
    private val adapterYearly = BillAdapter { onBillClick(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbarSubs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_fixed_costs)
        binding.toolbarSubs.setNavigationOnClickListener { finish() }

        // Konfiguracja list
        setupRecycler(binding.recyclerMonthly, adapterMonthly)
        setupRecycler(binding.recyclerQuarterly, adapterQuarterly)
        setupRecycler(binding.recyclerHalfYearly, adapterHalfYearly)
        setupRecycler(binding.recyclerYearly, adapterYearly)

        // Obserwowanie danych
        viewModel.recurringBills.observe(this) { bills ->
            distributeBills(bills)
            calculateMonthlyAverage(bills)
        }
    }

    private fun setupRecycler(recyclerView: androidx.recyclerview.widget.RecyclerView, adapter: BillAdapter) {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isNestedScrollingEnabled = false // Ważne dla płynnego przewijania
    }

    private fun onBillClick(bill: Bill) {
        val intent = Intent(this, AddEditBillActivity::class.java)
        intent.putExtra(AddEditBillActivity.EXTRA_BILL, bill)
        startActivity(intent)
    }

    private fun distributeBills(bills: List<Bill>) {
        // Filtrujemy listę na 4 podlisty
        val monthly = bills.filter { it.frequency == 1 }
        val quarterly = bills.filter { it.frequency == 3 }
        val halfYearly = bills.filter { it.frequency == 6 }
        val yearly = bills.filter { it.frequency == 12 }

        // Wstawiamy dane do adapterów
        adapterMonthly.submitList(monthly)
        adapterQuarterly.submitList(quarterly)
        adapterHalfYearly.submitList(halfYearly)
        adapterYearly.submitList(yearly)

        // Ukrywamy puste sekcje, żeby nie zajmowały miejsca
        binding.headerMonthly.visibility = if (monthly.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerMonthly.visibility = if (monthly.isEmpty()) View.GONE else View.VISIBLE

        binding.headerQuarterly.visibility = if (quarterly.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerQuarterly.visibility = if (quarterly.isEmpty()) View.GONE else View.VISIBLE

        binding.headerHalfYearly.visibility = if (halfYearly.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerHalfYearly.visibility = if (halfYearly.isEmpty()) View.GONE else View.VISIBLE

        binding.headerYearly.visibility = if (yearly.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerYearly.visibility = if (yearly.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun calculateMonthlyAverage(bills: List<Bill>) {
        // Obliczamy "Średni koszt miesięczny"
        // Np. rachunek roczny 1200 zł liczymy jako 100 zł/mies
        var totalMonthlyAvg = 0.0

        bills.forEach { bill ->
            val monthlyCost = when (bill.frequency) {
                1 -> bill.amount                // Miesięczny: 100%
                3 -> bill.amount / 3.0          // Kwartalny: podziel przez 3
                6 -> bill.amount / 6.0          // Półroczny: podziel przez 6
                12 -> bill.amount / 12.0        // Roczny: podziel przez 12
                else -> 0.0
            }
            totalMonthlyAvg += monthlyCost
        }

        // Formatowanie waluty
        val currentLang = AppCompatDelegate.getApplicationLocales().get(0)?.language
        val formatLocale = if (currentLang == "pl") Locale("pl", "PL") else Locale.US
        val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)

        binding.tvTotalRecurring.text = currencyFormat.format(totalMonthlyAvg)
    }
}