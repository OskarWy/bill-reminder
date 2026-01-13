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

        val now = System.currentTimeMillis()
        val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
        val future = now + threeDaysInMillis

        // Pobierz rachunki, które są przeterminowane lub płatne w ciągu 3 dni
        val upcomingBills = dao.getBillsInRange(now - (24 * 60 * 60 * 1000L), future)

        if (upcomingBills.isNotEmpty()) {
            upcomingBills.forEach { bill ->
                showNotification(bill.id.toInt(), bill.name, bill.amount)
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Int, name: String, amount: Double) {
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val currencyFormat = NumberFormat.getCurrencyInstance()
        val formattedAmount = currencyFormat.format(amount)

        val builder = NotificationCompat.Builder(applicationContext, "bill_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Upewnij się, że masz ikonę
            .setContentTitle(applicationContext.getString(R.string.notification_title, name))
            .setContentText(applicationContext.getString(R.string.notification_content, formattedAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
    }
}