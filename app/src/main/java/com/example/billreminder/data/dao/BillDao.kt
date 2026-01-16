package com.example.billreminder.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.billreminder.data.model.Bill

// 1. Klasa pomocnicza do statystyk
data class CategoryStat(
    val category: String,
    val totalAmount: Double
)

@Dao
interface BillDao {
    // Używane przez ViewModel (obserwuje zmiany na żywo)
    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): LiveData<List<Bill>>

    // --- TO JEST TA BRAKUJĄCA FUNKCJA DLA WORKERA ---
    // Zwraca zwykłą listę (nie LiveData), żeby Worker mógł ją przetworzyć w tle
    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBillsSync(): List<Bill>
    // ------------------------------------------------

    @Query("SELECT * FROM bills WHERE dueDate >= :start AND dueDate <= :end")
    suspend fun getBillsInRange(start: Long, end: Long): List<Bill>

    // Statystyki kategorii
    @Query("SELECT category, SUM(amount) as totalAmount FROM bills GROUP BY category")
    fun getCategoryStats(): LiveData<List<CategoryStat>>

    // Pobierz tylko cykliczne
    @Query("SELECT * FROM bills WHERE frequency > 0 ORDER BY name ASC")
    fun getRecurringBills(): LiveData<List<Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: Bill)

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)
}