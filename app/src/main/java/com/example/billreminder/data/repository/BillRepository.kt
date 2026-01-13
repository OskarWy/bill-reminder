package com.example.billreminder.data.repository

import androidx.lifecycle.LiveData
import com.example.billreminder.data.dao.BillDao
import com.example.billreminder.data.dao.CategoryStat
import com.example.billreminder.data.model.Bill

class BillRepository(private val billDao: BillDao) {
    val allBills: LiveData<List<Bill>> = billDao.getAllBills()

    fun getCategoryStats(): LiveData<List<CategoryStat>> = billDao.getCategoryStats()

    suspend fun insert(bill: Bill) = billDao.insert(bill)
    suspend fun update(bill: Bill) = billDao.update(bill)
    suspend fun delete(bill: Bill) = billDao.delete(bill)
}