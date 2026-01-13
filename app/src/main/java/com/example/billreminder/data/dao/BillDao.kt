package com.example.billreminder.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.billreminder.data.model.Bill

// 1. Dodajemy małą klasę "pojemnik" na wynik statystyk (poza interfejsem)
data class CategoryStat(
    val category: String,
    val totalAmount: Double
)

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): LiveData<List<Bill>>

    @Query("SELECT * FROM bills WHERE dueDate >= :start AND dueDate <= :end")
    suspend fun getBillsInRange(start: Long, end: Long): List<Bill>

    // 2. NOWE: To zapytanie grupuje rachunki po kategorii i sumuje ich kwoty
    @Query("SELECT category, SUM(amount) as totalAmount FROM bills GROUP BY category")
    fun getCategoryStats(): LiveData<List<CategoryStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: Bill)

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)
}