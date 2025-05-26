package com.ak7studio.agsfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleForecastActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var spinnerFilter: Spinner

    private val saleForecastList = mutableListOf<SaleForecastEntry>()
    private val itemList = mutableListOf<String>() // populate from config or static list
    private val unitList = mutableListOf<String>() // populate from config or static list
    private lateinit var adapter: SaleForecastAdapter

    override fun getCurrentNavItemId(): Int = R.id.nav_forecast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_sale_forecast, contentFrameLayout, true)

        recyclerView = findViewById(R.id.rvSaleForecast)
        recyclerView.layoutManager = LinearLayoutManager(this)  // IMPORTANT
        fabAdd = findViewById(R.id.fabAddForecast)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        adapter = SaleForecastAdapter(saleForecastList) { saleForecastEntry ->
            showForecastDetailDialog(saleForecastEntry)
        }
        recyclerView.adapter = adapter

        loadSaleForecasts()

        fabAdd.setOnClickListener {
            showForecastDetailDialog(null) // null for new entry
        }

        // Spinner filter setup (optional)
        val filterAdapter = ArrayAdapter.createFromResource(
            this, R.array.filter_options, android.R.layout.simple_spinner_item
        )
        spinnerFilter.adapter = filterAdapter
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // TODO: Implement filter logic if needed
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        SaleForecastSheetsHelper.fetchConfig { items, units ->
            runOnUiThread {
                itemList.clear()
                itemList.addAll(items)
                unitList.clear()
                unitList.addAll(units)
                // Now you can notify adapter or update UI if needed
            }
        }

    }

    // Load data from Google Sheets and update UI
    private fun loadSaleForecasts() {
        SaleForecastSheetsHelper.fetchSaleForecastEntries { entries ->
            runOnUiThread {
                saleForecastList.clear()
                saleForecastList.addAll(entries)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // Show dialog to add/edit sale forecast
    private fun showForecastDetailDialog(
        forecast: SaleForecastEntry?, // summary info
        details: List<ForecastItem> = emptyList() // full detail list for this forecast
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sale_forecast_detail, null)
        val rgShift = dialogView.findViewById<RadioGroup>(R.id.rgShift)
        val rvItems = dialogView.findViewById<RecyclerView>(R.id.rvForecastItems)
        val btnAddRow = dialogView.findViewById<Button>(R.id.btnAddRow)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDialogTotal)

        // Pre-populate items from config or hardcoded
        val items = details.toMutableList()

        // Set LayoutManager (important!)
        rvItems.layoutManager = LinearLayoutManager(this)

        val forecastItemAdapter = ForecastItemAdapter(items, itemList, unitList) {
            tvTotal.text = items.sumOf { it.totalPrice }.toString()
        }
        rvItems.adapter = forecastItemAdapter

        btnAddRow.setOnClickListener {
            items.add(ForecastItem(itemList.firstOrNull() ?: "", 0, unitList.firstOrNull() ?: "", 0, 0))
            forecastItemAdapter.notifyItemInserted(items.size - 1)
            tvTotal.text = items.sumOf { it.totalPrice }.toString()
        }
        tvTotal.text = items.sumOf { it.totalPrice }.toString()

        // Pre-select shift if editing
        if (forecast != null) {
            if (forecast.shift.equals("Morning", true)) rgShift.check(R.id.rbMorning)
            else rgShift.check(R.id.rbEvening)
        } else {
            rgShift.check(R.id.rbMorning)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(if (forecast == null) "Add Sale Forecast" else "Edit Sale Forecast")
            .setPositiveButton("Save") { _, _ ->
                val shift = if (rgShift.checkedRadioButtonId == R.id.rbMorning) "Morning" else "Evening"
                val date = forecast?.date ?: SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(
                    Date()
                )
                val totalSales = items.sumOf { it.totalPrice }
                val total = totalSales

                val entry = SaleForecastEntry(
                    rowId = forecast?.rowId ?: 0,
                    date = date,
                    shift = shift,
                    sales = totalSales,
                    total = total
                )
                // Save both summary and details!
                saveEntryWithDetails(entry, items, isNew = (forecast == null))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveEntryWithDetails(
        entry: SaleForecastEntry,
        details: List<ForecastItem>,
        isNew: Boolean
    ) {
        // Save summary entry first
        saveEntry(entry, isNew)

        // Save details list to Google Sheets (you need to implement this in your helper)
        SaleForecastSheetsHelper.saveForecastDetails(entry.date, entry.shift, details) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Details saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Save or update entry in Google Sheets
    private fun saveEntry(entry: SaleForecastEntry, isNew: Boolean) {
        if (isNew) {
            SaleForecastSheetsHelper.addSaleForecastEntry(entry) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Sale forecast added", Toast.LENGTH_SHORT).show()
                        loadSaleForecasts()
                    } else {
                        Toast.makeText(this, "Failed to add sale forecast", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            SaleForecastSheetsHelper.updateSaleForecastEntry(entry) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Sale forecast updated", Toast.LENGTH_SHORT).show()
                        loadSaleForecasts()
                    } else {
                        Toast.makeText(this, "Failed to update sale forecast", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Extension functions for conversions if needed
    fun SaleForecastEntry.toSaleForecast(): SaleForecast = SaleForecast(
        date = this.date,
        shift = this.shift,
        sales = this.sales,
        total = this.total,
        details = listOf() // load details separately if needed
    )

    fun SaleForecast.toSaleForecastEntry(rowId: Int = 0): SaleForecastEntry = SaleForecastEntry(
        rowId = rowId,
        date = this.date,
        shift = this.shift,
        sales = this.sales,
        total = this.total
    )
}
