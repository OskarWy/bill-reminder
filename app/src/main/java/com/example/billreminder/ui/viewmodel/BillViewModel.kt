package com.example.billreminder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.billreminder.data.dao.CategoryStat
import com.example.billreminder.data.database.BillDatabase
import com.example.billreminder.data.model.Bill
import com.example.billreminder.data.repository.BillRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class BillViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BillRepository
    val allBills: LiveData<List<Bill>>

    // NOWE: LiveData dla wykresu
    val categoryStats: LiveData<List<CategoryStat>>

    // LiveData dla sumy miesięcznej
    val monthlySummary = MediatorLiveData<Pair<Double, Int>>()

    init {
        val dao = BillDatabase.getDatabase(application).billDao()
        repository = BillRepository(dao)
        allBills = repository.allBills

        // Inicjalizacja statystyk
        categoryStats = repository.getCategoryStats()

        monthlySummary.addSource(allBills) { bills ->
            monthlySummary.value = calculateMonthlyStats(bills)
        }
    }

    fun insert(bill: Bill) = viewModelScope.launch(Dispatchers.IO) { repository.insert(bill) }
    fun update(bill: Bill) = viewModelScope.launch(Dispatchers.IO) { repository.update(bill) }
    fun delete(bill: Bill) = viewModelScope.launch(Dispatchers.IO) { repository.delete(bill) }

    private fun calculateMonthlyStats(bills: List<Bill>): Pair<Double, Int> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var sum = 0.0
        var count = 0

        bills.forEach { bill ->
            calendar.timeInMillis = bill.dueDate
            if (calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear) {
                sum += bill.amount
                count++
            }
        }
        return Pair(sum, count)
    }
}