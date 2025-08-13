package com.ak7studio.agsfood

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroceryMerchantActivity : BaseActivity() {

    private val groceryEntries = mutableListOf<GroceryEntry>()
    private lateinit var adapter: GroceryEntryAdapter

    override fun getCurrentNavItemId(): Int = R.id.nav_updateGroceryExpense

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_grocery_merchant, contentFrameLayout, true)

        adapter = GroceryEntryAdapter(
            context = this,
            entries = groceryEntries,
            onEdit = { entry, pos -> showGroceryDialog(entry, pos) },
            onDelete = { entry, pos -> confirmDelete(entry, pos) }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGrocery)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Change the button ID to match your layout file
        findViewById<Button>(R.id.btnAddGroceryEntry).setOnClickListener {
            showGroceryDialog(null, -1)
        }

        // Call the new load function that uses Firebase's real-time listener
        loadGroceryEntries()
    }

    private fun loadGroceryEntries() {
        Log.d("GroceryActivity", "Fetching grocery entries from Firebase...")
        // Using the new helper class to fetch data
        GroceryFirebaseHelper.fetchGroceryEntries { entries ->
            runOnUiThread {
                groceryEntries.clear()
                groceryEntries.addAll(entries)
                adapter.notifyDataSetChanged()
                Log.d("GroceryActivity", "Loaded ${entries.size} entries from Firebase.")
            }
        }
    }

    private fun showGroceryDialog(entry: GroceryEntry?, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_grocery_entry, null)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        // The add amount field has been removed as requested
        val etCash = dialogView.findViewById<EditText>(R.id.etCash)
        val etUpiPayment = dialogView.findViewById<EditText>(R.id.etUpiPayment)
        val etComments = dialogView.findViewById<EditText>(R.id.etComments)
        val tvBalance = dialogView.findViewById<TextView>(R.id.etBalance)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)
        val btnAddUpdate = dialogView.findViewById<Button>(R.id.btnSettle)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Placeholder for your role check logic. Replace this with your actual authentication check.
        val isUserCashier = true

        // Pre-fill if editing an existing entry
        entry?.let {
            etDate.setText(it.date)
            etAmount.setText(it.amount.toInt().toString())
            etCash.setText(it.cash.toInt().toString())
            etUpiPayment.setText(it.upiPayment.toInt().toString())
            etComments.setText(it.comments)
            etDate.isEnabled = false // Date is the key, so it cannot be edited
            btnAddUpdate.text = "Update"
        } ?: run {
            // New entry logic: set current date and enable date picker for cashiers
            val calendar = Calendar.getInstance()
            etDate.setText(dateFormat.format(calendar.time))

            if (isUserCashier) {
                etDate.isEnabled = true
                etDate.isFocusable = false
                etDate.isClickable = true
                etDate.setOnClickListener {
                    val datePickerDialog = DatePickerDialog(
                        this,
                        { _, year, month, dayOfMonth ->
                            calendar.set(year, month, dayOfMonth)
                            etDate.setText(dateFormat.format(calendar.time))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    // Calculate min and max dates based on your requirements
                    val today = Calendar.getInstance()
                    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -7) }
                    val firstDayOfMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

                    // Set min date to the later of the first day of the month or 7 days ago
                    val minDate = if (firstDayOfMonth.timeInMillis > sevenDaysAgo.timeInMillis) {
                        firstDayOfMonth
                    } else {
                        sevenDaysAgo
                    }
                    datePickerDialog.datePicker.minDate = minDate.timeInMillis
                    datePickerDialog.datePicker.maxDate = today.timeInMillis
                    datePickerDialog.show()
                }
            } else {
                // If not a cashier, date field is not editable for new entries
                etDate.isEnabled = false
            }

            btnAddUpdate.text = "Add"
        }

        fun updateBalance() {
            tvError.visibility = View.GONE
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val cash = etCash.text.toString().toDoubleOrNull() ?: 0.0
            val upi = etUpiPayment.text.toString().toDoubleOrNull() ?: 0.0
            // The totalAmount calculation is now just the base amount as addAmount is removed
            val totalAmount = amount
            val totalPaid = cash + upi
            val balance = totalAmount - totalPaid
            tvBalance.text = String.format("Balance: ₹%.2f", balance)

            if (totalPaid > totalAmount) {
                tvError.text = "Total paid cannot exceed total amount"
                tvError.visibility = View.VISIBLE
            }
        }

        // Add TextChangedListeners to all relevant fields
        etAmount.addTextChangedListener { updateBalance() }
        etCash.addTextChangedListener { updateBalance() }
        etUpiPayment.addTextChangedListener { updateBalance() }

        // Initial balance update
        updateBalance()

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (entry == null) "Add Grocery Entry" else "Edit Grocery Entry")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnAddUpdate.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val cash = etCash.text.toString().toDoubleOrNull() ?: 0.0
            val upi = etUpiPayment.text.toString().toDoubleOrNull() ?: 0.0
            val comments = etComments.text.toString()
            val totalAmount = amount
            val totalPaid = cash + upi

            if (amount <= 0.0) {
                tvError.text = "Please enter a valid amount."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (totalPaid > totalAmount) {
                tvError.text = "Total paid cannot exceed total amount."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val balance = totalAmount - totalPaid
            val newEntry = GroceryEntry(
                id = entry?.id ?: "", // Preserve id for update, empty string for new
                date = etDate.text.toString(),
                amount = amount,
                addAmount = 0.0, // This is now always 0.0
                cash = cash,
                balance = balance,
                upiPayment = upi,
                comments = comments
            )

            if (entry == null) {
                GroceryFirebaseHelper.addGroceryEntry(newEntry) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Entry added!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Failed to add entry. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                GroceryFirebaseHelper.updateGroceryEntry(newEntry) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            tvError.text = "Failed to update entry. Please try again."
                            tvError.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(entry: GroceryEntry, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { dialog, _ ->
                GroceryFirebaseHelper.deleteGroceryEntry(entry) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
