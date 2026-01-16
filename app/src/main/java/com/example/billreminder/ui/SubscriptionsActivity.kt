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

    private val adapterMonthly = BillAdapter { onBillClick(it) }
    private val adapterQuarterly = BillAdapter { onBillClick(it) }
    private val adapterHalfYearly = BillAdapter { onBillClick(it) }
    private val adapterYearly = BillAdapter { onBillClick(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSubs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_fixed_costs)
        binding.toolbarSubs.setNavigationOnClickListener { finish() }

        setupRecycler(binding.recyclerMonthly, adapterMonthly)
        setupRecycler(binding.recyclerQuarterly, adapterQuarterly)
        setupRecycler(binding.recyclerHalfYearly, adapterHalfYearly)
        setupRecycler(binding.recyclerYearly, adapterYearly)

        viewModel.recurringBills.observe(this) { bills ->
            distributeBills(bills)
            calculateMonthlyAverage(bills)
        }
    }

    private fun setupRecycler(recyclerView: androidx.recyclerview.widget.RecyclerView, adapter: BillAdapter) {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isNestedScrollingEnabled = false
    }

    private fun onBillClick(bill: Bill) {
        val intent = Intent(this, AddEditBillActivity::class.java)
        intent.putExtra(AddEditBillActivity.EXTRA_BILL, bill)
        startActivity(intent)
    }

    private fun distributeBills(bills: List<Bill>) {
        val monthly = bills.filter { it.frequency == 1 }
        val quarterly = bills.filter { it.frequency == 3 }
        val halfYearly = bills.filter { it.frequency == 6 }
        val yearly = bills.filter { it.frequency == 12 }

        adapterMonthly.submitList(monthly)
        adapterQuarterly.submitList(quarterly)
        adapterHalfYearly.submitList(halfYearly)
        adapterYearly.submitList(yearly)

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
        var totalMonthlyAvg = 0.0

        bills.forEach { bill ->
            val monthlyCost = when (bill.frequency) {
                1 -> bill.amount
                3 -> bill.amount / 3.0
                6 -> bill.amount / 6.0
                12 -> bill.amount / 12.0
                else -> 0.0
            }
            totalMonthlyAvg += monthlyCost
        }

        // --- POPRAWKA WALUTY ---
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLocale = if (!appLocales.isEmpty) appLocales.get(0) else Locale.getDefault()
        val isPolish = currentLocale?.language == "pl"
        val formatLocale = if (isPolish) Locale("pl", "PL") else Locale.US
        val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)
        // -----------------------

        binding.tvTotalRecurring.text = currencyFormat.format(totalMonthlyAvg)
    }
}