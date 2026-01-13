package com.example.billreminder.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.billreminder.R
import com.example.billreminder.databinding.ActivityStatsBinding
import com.example.billreminder.ui.viewmodel.BillViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val viewModel: BillViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupChartStyle()
        loadChartData()
    }

    private fun setupChartStyle() {
        binding.pieChart.apply {
            description.isEnabled = false

            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL

                setDrawInside(false)
                isWordWrapEnabled = true

                xEntrySpace = 10f
                yEntrySpace = 5f
                textSize = 12f

                textColor = ContextCompat.getColor(context, R.color.text_primary)
            }

            setExtraOffsets(30f, 10f, 30f, 10f)

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setEntryLabelColor(Color.TRANSPARENT)
            setEntryLabelTextSize(0f)

            animateY(1400)
        }
    }

    private fun loadChartData() {
        viewModel.categoryStats.observe(this) { stats ->
            val entries = ArrayList<PieEntry>()
            val colors = ArrayList<Int>()
            var totalSum = 0.0

            stats.forEach { stat ->
                if (stat.totalAmount > 0) {
                    // --- ZMIANA 1: TŁUMACZENIE NAZWY ---
                    // Tutaj używamy funkcji pomocniczej, żeby zamienić tekst z bazy na tekst z pliku językowego
                    val translatedName = getLocalizedCategoryName(stat.category)

                    // Używamy translatedName zamiast stat.category
                    entries.add(PieEntry(stat.totalAmount.toFloat(), translatedName))
                    totalSum += stat.totalAmount

                    val colorRes = when (stat.category) {
                        "Subscriptions", "Subskrypcje" -> R.color.bill_subscriptions
                        "Utilities", "Rachunki (Prąd/Gaz)" -> R.color.bill_utilities
                        "Insurance", "Ubezpieczenia" -> R.color.bill_insurance
                        "Rent/Mortgage", "Czynsz/Kredyt" -> R.color.bill_rent
                        else -> R.color.bill_other
                    }
                    colors.add(ContextCompat.getColor(this, colorRes))
                }
            }

            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = colors

                dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

                dataSet.valueLinePart1OffsetPercentage = 80f
                dataSet.valueLinePart1Length = 0.4f
                dataSet.valueLinePart2Length = 0.4f
                dataSet.valueLineWidth = 1.5f
                dataSet.valueLineColor = ContextCompat.getColor(this, R.color.text_secondary)

                dataSet.valueTextColor = ContextCompat.getColor(this, R.color.text_primary)
                dataSet.valueTextSize = 14f
                dataSet.sliceSpace = 3f

                val data = PieData(dataSet)

                data.setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.0f", value)
                    }
                })

                binding.pieChart.data = data
                binding.pieChart.invalidate()
            } else {
                binding.pieChart.clear()
            }

            updateTotalSumText(totalSum)
        }
    }

    // --- ZMIANA 2: FUNKCJA TŁUMACZĄCA ---
    // Ta funkcja sprawdza nazwę z bazy i zwraca odpowiedni string z zasobów (PL lub EN)
    private fun getLocalizedCategoryName(rawName: String): String {
        return when (rawName) {
            "Subscriptions", "Subskrypcje" -> getString(R.string.cat_subscriptions)
            "Utilities", "Rachunki (Prąd/Gaz)" -> getString(R.string.cat_utilities)
            "Insurance", "Ubezpieczenia" -> getString(R.string.cat_insurance)
            "Rent/Mortgage", "Czynsz/Kredyt" -> getString(R.string.cat_rent)
            "Other", "Inne" -> getString(R.string.cat_other)
            else -> rawName
        }
    }

    private fun updateTotalSumText(sum: Double) {
        val currentLang = AppCompatDelegate.getApplicationLocales().get(0)?.language
        val formatLocale = if (currentLang == "pl") Locale("pl", "PL") else Locale.US
        val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)

        binding.tvTotalExpenses.text = getString(R.string.total_summary, currencyFormat.format(sum))
    }
}