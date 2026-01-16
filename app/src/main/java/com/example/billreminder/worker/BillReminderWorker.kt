package com.example.billreminder.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.billreminder.R
import com.example.billreminder.data.database.BillDatabase
import com.example.billreminder.ui.MainActivity
import java.text.NumberFormat
import java.util.*

class BillReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = BillDatabase.getDatabase(applicationContext)
        val dao = database.billDao()

        // 1. Pobieramy wszystkie rachunki (najbezpieczniejsza opcja)
        val allBills = dao.getAllBillsSync()

        val now = System.currentTimeMillis()
        // 3 dni w milisekundach
        val threeDaysInMillis = 3L * 24 * 60 * 60 * 1000L

        // 2. Filtrujemy w kodzie (to daje nam pełną kontrolę)
        val upcomingBills = allBills.filter { bill ->
            val diff = bill.dueDate - now
            // Warunek: Termin jest w ciągu 3 dni LUB termin minął (diff < 0)
            diff <= threeDaysInMillis
        }

        if (upcomingBills.isNotEmpty()) {
            upcomingBills.forEach { bill ->
                showNotification(bill.id.toInt(), bill.name, bill.amount)
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Int, name: String, amount: Double) {
        // Sprawdzenie uprawnień (wymagane od Android 13)
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Kliknięcie w powiadomienie otwiera apkę
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Formatowanie waluty (tu używamy domyślnej systemowej, bo Worker działa w tle)
        val currencyFormat = NumberFormat.getCurrencyInstance()
        val formattedAmount = currencyFormat.format(amount)

        val builder = NotificationCompat.Builder(applicationContext, "bill_channel_id")
            // --- TU JEST ZMIANA NA PROFESJONALNĄ IKONĘ ---
            .setSmallIcon(R.drawable.ic_card_outlined)
            // ---------------------------------------------
            .setContentTitle(applicationContext.getString(R.string.notification_title, name))
            .setContentText(applicationContext.getString(R.string.notification_content, formattedAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Powiadomienie znika po kliknięciu

        NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
    }
}