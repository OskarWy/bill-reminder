package com.example.billreminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_history")
data class PaymentHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billName: String,
    val amount: Double,
    val paymentDate: Long, // Data, kiedy kliknąłeś "Opłacone"
    val billCategory: String
)