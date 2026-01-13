package com.example.billreminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.billreminder.data.dao.BillDao
import com.example.billreminder.data.dao.HistoryDao // <-- NOWE: Import nowego DAO
import com.example.billreminder.data.model.Bill
import com.example.billreminder.data.model.PaymentHistory // <-- NOWE: Import nowego modelu

// ZMIANA: Dodano PaymentHistory::class do entities oraz zmieniono version na 2
@Database(entities = [Bill::class, PaymentHistory::class], version = 2, exportSchema = false)
abstract class BillDatabase : RoomDatabase() {

    abstract fun billDao(): BillDao
    abstract fun historyDao(): HistoryDao // <-- NOWE: Dostęp do tabeli historii

    companion object {
        @Volatile
        private var INSTANCE: BillDatabase? = null

        fun getDatabase(context: Context): BillDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillDatabase::class.java,
                    "bill_database"
                )
                    .fallbackToDestructiveMigration() // <-- WAŻNE: Pozwala na zmianę wersji bazy bez błędów (czyści dane przy update)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}