package com.example.billreminder.data.repository

import androidx.lifecycle.LiveData
import com.example.billreminder.data.dao.BillDao
import com.example.billreminder.data.dao.HistoryDao // <-- NOWE
import com.example.billreminder.data.model.Bill
import com.example.billreminder.data.model.PaymentHistory // <-- NOWE

// Dodajemy historyDao do konstruktora
class BillRepository(private val billDao: BillDao, private val historyDao: HistoryDao) {

    val allBills: LiveData<List<Bill>> = billDao.getAllBills()

    // --- NOWE: Dostęp do historii ---
    val allHistory: LiveData<List<PaymentHistory>> = historyDao.getAllHistory()

    val recurringBills: LiveData<List<Bill>> = billDao.getRecurringBills()

    // Metody dla rachunków
    fun getCategoryStats() = billDao.getCategoryStats()
    suspend fun insert(bill: Bill) = billDao.insert(bill)
    suspend fun update(bill: Bill) = billDao.update(bill)
    suspend fun delete(bill: Bill) = billDao.delete(bill)

    // Jeśli kiedyś dodasz wyszukiwanie, tu będzie metoda searchBills

    // --- NOWE: Metoda zapisu do historii ---
    suspend fun addToHistory(payment: PaymentHistory) {
        historyDao.insert(payment)
    }
}