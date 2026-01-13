package com.example.billreminder.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDate: Long,
    val category: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // 0 = Jednorazowy, 1 = Co miesiąc, 3 = Co kwartał, 6 = Co pół roku, 12 = Co rok
    val frequency: Int = 0
) : Parcelable