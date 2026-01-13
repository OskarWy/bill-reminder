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
import androidx.core.app.ActivityCompat
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

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar) // Ważne dla menu języka!

        // Jeśli nie dodałeś Toolbar w XML, system użyje domyślnego ActionBara.
        // Jeśli masz customowy layout bez toolbara, kod poniżej może wymagać korekty w activity_main.xml.
        // Zakładamy standardowy AppBarLayout z poprzedniego kodu.

        setupRecyclerView()
        setupViewModel()
        setupNotifications()
        checkPermissions()

        binding.fabAddBill.setOnClickListener {
            val intent = Intent(this, AddEditBillActivity::class.java)
            addEditLauncher.launch(intent)
        }
    }

    // --- MENU JĘZYKA ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_stats -> {
                // NOWE: Uruchamiamy ekran statystyk
                val intent = Intent(this, StatsActivity::class.java)
                startActivity(intent)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setAppLocale(language: String) {
        val appLocale = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
    // -------------------

    private fun setupRecyclerView() {
        adapter = BillAdapter { bill ->
            val intent = Intent(this, AddEditBillActivity::class.java)
            intent.putExtra(AddEditBillActivity.EXTRA_BILL, bill)
            addEditLauncher.launch(intent)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // LOGIKA SWIPE (PRZESUWANIA) - TUTAJ JEST MAGIA ODNAWIANIA
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val bill = adapter.currentList[position]

                if (bill.frequency > 0) {
                    // RACHUNEK CYKLICZNY - Pytamy czy odnowić
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.dialog_recurring_title))
                        .setMessage(getString(R.string.dialog_recurring_msg))
                        .setPositiveButton(getString(R.string.action_renew, bill.frequency)) { _, _ ->
                            renewBill(bill)
                            adapter.notifyItemChanged(position) // Odśwież widok
                        }
                        .setNegativeButton(getString(R.string.action_delete)) { _, _ ->
                            billViewModel.delete(bill)
                        }
                        .setNeutralButton(getString(R.string.cancel)) { _, _ ->
                            adapter.notifyItemChanged(position) // Cofnij przesunięcie
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    // RACHUNEK JEDNORAZOWY - Usuwamy
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.confirm_delete))
                        .setPositiveButton(getString(R.string.yes)) { _, _ -> billViewModel.delete(bill) }
                        .setNegativeButton(getString(R.string.no)) { _, _ -> adapter.notifyItemChanged(position) }
                        .show()
                }
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun renewBill(bill: Bill) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = bill.dueDate
        // Dodaj liczbę miesięcy z pola frequency
        calendar.add(Calendar.MONTH, bill.frequency)

        // Stwórz kopię rachunku z nową datą
        val updatedBill = bill.copy(dueDate = calendar.timeInMillis)

        // Zapisz w bazie (nadpisz stary)
        billViewModel.update(updatedBill)
        Toast.makeText(this, getString(R.string.msg_renewed), Toast.LENGTH_SHORT).show()
    }

    private fun setupViewModel() {
        billViewModel.allBills.observe(this) { bills ->
            adapter.submitList(bills)
            binding.tvEmptyState.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
        }

        billViewModel.monthlySummary.observe(this) { (sum, count) ->
            // --- POPRAWKA WALUTY "NA TWARDO" ---
            val currentLang = AppCompatDelegate.getApplicationLocales().get(0)?.language

            // Jeśli polski -> wymuś Polskę (zł), w przeciwnym razie USA ($)
            val formatLocale = if (currentLang == "pl") Locale("pl", "PL") else Locale.US
            val currencyFormat = NumberFormat.getCurrencyInstance(formatLocale)

            binding.tvMonthlySummary.text = getString(R.string.monthly_summary, currencyFormat.format(sum), count)
            // ------------------------------------
        }
    }

    private fun setupNotifications() {
        createNotificationChannel()
        val workRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BillReminderWork", ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("bill_channel_id", name, importance).apply {
                description = getString(R.string.channel_desc)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}