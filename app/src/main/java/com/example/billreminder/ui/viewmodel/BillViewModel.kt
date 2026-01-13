package com.example.billreminder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.billreminder.data.dao.CategoryStat
import com.example.billreminder.data.database.BillDatabase
import com.example.billreminder.data.model.Bill
import com.example.billreminder.data.model.PaymentHistory
import com.example.billreminder.data.repository.BillRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class BillViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Zmienne
    private val repository: BillRepository

    val allBills: LiveData<List<Bill>>
    val historyList: LiveData<List<PaymentHistory>>
    val recurringBills: LiveData<List<Bill>>

    // ZMIANA: Tu nie przypisujemy w init, tylko definiujemy logikę od razu
    // To sprawia, że wykres reaguje na zmiany i filtruje daty!
    val categoryStats: LiveData<List<CategoryStat>>

    val monthlySummary = MediatorLiveData<Pair<Double, Int>>()

    // 2. Blok inicjalizujący
    init {
        val database = BillDatabase.getDatabase(application)
        repository = BillRepository(database.billDao(), database.historyDao())

        allBills = repository.allBills
        historyList = repository.allHistory
        recurringBills = repository.recurringBills

        // ZMIANA: Logika wykresu (Tylko obecny miesiąc)
        categoryStats = allBills.map { bills ->
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // 1. Wybierz tylko rachunki z TEGO miesiąca
            val filteredBills = bills.filter { bill ->
                val billCal = Calendar.getInstance()
                billCal.timeInMillis = bill.dueDate
                billCal.get(Calendar.MONTH) == currentMonth &&
                        billCal.get(Calendar.YEAR) == currentYear
            }

            // 2. Pogrupuj kategorie i zsumuj kwoty
            val statsMap = filteredBills.groupingBy { it.category }
                .fold(0.0) { sum, bill -> sum + bill.amount }

            // 3. Zamień na listę, którą rozumie wykres
            statsMap.map { entry ->
                CategoryStat(entry.key, entry.value)
            }
        }

        // Logika sumowania miesiąca (Tekst na dole ekranu)
        monthlySummary.addSource(allBills) { bills ->
            monthlySummary.value = calculateMonthlyStats(bills)
        }
    }

    // 3. Metody

    fun addToHistory(bill: Bill) = viewModelScope.launch(Dispatchers.IO) {
        val historyItem = PaymentHistory(
            billName = bill.name,
            amount = bill.amount,
            paymentDate = System.currentTimeMillis(),
            billCategory = bill.category
        )
        repository.addToHistory(historyItem)
    }

    fun insert(bill: Bill) = viewModelScope.launch(Dispatchers.IO) { repository.insert(bill) }
    fun update(bill: Bill) = viewModelScope.launch(Dispatchers.IO) { repository.update(bill) }
    fun delete(bill: Bill) = viewModelScope.launch(Dispatchers.IO) { repository.delete(bill) }

    // Obliczenia (pomocnicze dla tekstu sumy)
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