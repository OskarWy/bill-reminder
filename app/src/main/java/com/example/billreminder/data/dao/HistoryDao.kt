package com.example.billreminder.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.billreminder.data.model.PaymentHistory

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(payment: PaymentHistory)

    // Pobiera historię posortowaną od najnowszych wpłat
    @Query("SELECT * FROM payment_history ORDER BY paymentDate DESC")
    fun getAllHistory(): LiveData<List<PaymentHistory>>

    // Opcja czyszczenia całej historii (na przyszłość)
    @Query("DELETE FROM payment_history")
    suspend fun clearHistory()
}