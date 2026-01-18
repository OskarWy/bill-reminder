package com.example.billreminder.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.billreminder.R
import com.example.billreminder.data.model.Bill
import com.example.billreminder.databinding.ActivityMainBinding
import com.example.billreminder.ui.adapter.BillAdapter
import com.example.billreminder.ui.viewmodel.BillViewModel
import com.example.billreminder.worker.BillReminderWorker
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val billViewModel: BillViewModel by viewModels()
    private lateinit var adapter: BillAdapter

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bill = result.data?.getParcelableExtra<Bill>(AddEditBillActivity.EXTRA_BILL)
            bill?.let {
                if (it.id == 0L) billViewModel.insert(it) else billViewModel.update(it)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Zgoda przyznana - powiadomienia będą działać
        } else {
            // Brak zgody
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // 1. KLUCZOWA POPRAWKA: Tworzenie kanału musi być tutaj!
        createNotificationChannel()

        setupRecyclerView()
        setupViewModel()

        // 2. Najpierw pytamy o uprawnienia
        checkPermissions()

        // 3. Potem uruchamiamy harmonogram
        setupNotifications()

        binding.fabAddBill.setOnClickListener {
            val intent = Intent(this, AddEditBillActivity::class.java)
            addEditLauncher.launch(intent)
        }
    }

    // --- HARMONOGRAM POWIADOMIEŃ ---
    private fun setupNotifications() {
        // --- WERSJA FINALNA: CODZIENNIE O 8:00 RANO ---

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Ustawiamy godzinę 8:00:00
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Jeśli dzisiaj już jest po 8:00, ustaw alarm na jutro na 8:00
        if (calendar.timeInMillis < now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = calendar.timeInMillis - now

        // Tworzymy zadanie cykliczne co 24h
        val workRequest = PeriodicWorkRequestBuilder<com.example.billreminder.worker.BillReminderWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag("daily_bill_reminder") // Dodajemy tag dla porządku
            .build()

        // Kolejkujemy (UPDATE nadpisze ewentualne stare testy)
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BillReminderWork",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun createNotificationChannel() {
        // Wymagane dla Androida 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("bill_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissions() {
        // Wymagane dla Androida 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- MENU ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                true
            }
            R.id.action_language_en -> {
                setAppLocale("en")
                true
            }
            R.id.action_language_pl -> {
                setAppLocale("pl")
                true
            }
            R.id.action_subscriptions -> {
                startActivity(Intent(this, SubscriptionsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setAppLocale(language: String) {
        val appLocale = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    // --- LOGIKA LISTY ---
    private fun setupRecyclerView() {
        adapter = BillAdapter { bill ->
            val intent = Intent(this, AddEditBillActivity::class.java)
            intent.putExtra(AddEditBillActivity.EXTRA_BILL, bill)
            addEditLauncher.launch(intent)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val bill = adapter.currentList[position]

                // Cofamy swipe wizualnie, żeby nie było dziury
                adapter.notifyItemChanged(position)

                if (bill.frequency > 0) {
                    showRecurringDialog(bill)
                } else {
                    showOneTimeDialog(bill)
                }
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun showRecurringDialog(bill: Bill) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_recurring_title))
            .setMessage(getString(R.string.dialog_recurring_msg))
            .setPositiveButton(getString(R.string.action_renew, bill.frequency)) { _, _ ->
                renewBill(bill)
            }
            .setNegativeButton(getString(R.string.action_delete)) { _, _ ->
                billViewModel.delete(bill)
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .setCancelable(false)
            .show()
    }

    private fun showOneTimeDialog(bill: Bill) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_one_time_title))
            .setMessage(getString(R.string.dialog_one_time_msg))
            .setPositiveButton(getString(R.string.action_paid_history)) { _, _ ->
                billViewModel.addToHistory(bill)
                billViewModel.delete(bill)
                Toast.makeText(this, getString(R.string.msg_renewed), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.action_just_delete)) { _, _ ->
                billViewModel.delete(bill)
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .setCancelable(false)
            .show()
    }

    private fun renewBill(bill: Bill) {
        billViewModel.addToHistory(bill)

        val calendar = Calendar.getInstance()
        val today = System.currentTimeMillis()
        calendar.timeInMillis = bill.dueDate

        calendar.add(Calendar.MONTH, bill.frequency)
        while (calendar.timeInMillis < today) {
            calendar.add(Calendar.MONTH, bill.frequency)
        }

        val updatedBill = bill.copy(dueDate = calendar.timeInMillis)
        billViewModel.update(updatedBill)

        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val newDateStr = dateFormat.format(Date(calendar.timeInMillis))

        Toast.makeText(this, "${getString(R.string.msg_renewed)} -> $newDateStr", Toast.LENGTH_LONG).show()
    }

    private fun setupViewModel() {
        billViewModel.allBills.observe(this) { bills ->
            adapter.submitList(bills)
            binding.tvEmptyState.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
        }

        billViewModel.monthlySummary.observe(this) { (sum, count) ->
            val appLocales = AppCompatDelegate.getApplicationLocales()
            val currentLocale = if (!appLocales.isEmpty) appLocales.get(0) else Locale.getDefault()
            val formatLocale = if (currentLocale?.language == "pl") Locale("pl", "PL") else Locale.US
            val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)
            binding.tvMonthlySummary.text = getString(R.string.monthly_summary, currencyFormat.format(sum), count)
        }
    }
}