package com.ak7studio.agsfood

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
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
            groceryEntries,
            onEdit = { entry, pos -> showEditGroceryDialog(entry, pos) },
            onDelete = { entry, pos -> confirmDelete(entry, pos) }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGrocery)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnAddGroceryEntry).setOnClickListener {

            showAddGroceryEntryDialog()
        }

        loadGroceryEntries()
    }

    private fun loadGroceryEntries() {
        GrocerySheetsHelper.fetchGroceryEntries { entries ->
            runOnUiThread {
                groceryEntries.clear()
                groceryEntries.addAll(entries)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showAddGroceryEntryDialog() {
        showGroceryDialog(null, -1)
    }

    private fun showEditGroceryDialog(entry: GroceryEntry, position: Int) {
        showGroceryDialog(entry, position)
    }

    private fun showGroceryDialog(entry: GroceryEntry?, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_grocery_entry, null)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etAddAmount = dialogView.findViewById<EditText>(R.id.etAddAmount)
        val etCash = dialogView.findViewById<EditText>(R.id.etCash)
        val etBalance = dialogView.findViewById<EditText>(R.id.etBalance)
        val etUpiPayment = dialogView.findViewById<EditText>(R.id.etUpiPayment)
        val etComments = dialogView.findViewById<EditText>(R.id.etComments)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)

        // Pre-fill if editing
        entry?.let {
            etDate.setText(it.date)
            etAmount.setText(it.amount.toInt().toString())
            etAddAmount.setText(it.addAmount.toInt().toString())
            etCash.setText(it.cash.toInt().toString())
            etBalance.setText(it.balance.toInt().toString())
            etUpiPayment.setText(it.upiPayment.toInt().toString())
            etComments.setText(it.comments)
            // Disable date editing
            etDate.isEnabled = false
            etDate.isFocusable = false
            etDate.isClickable = false
        }

        fun updateBalanceAndError() {
            tvError.visibility = View.GONE
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val addAmount = etAddAmount.text.toString().toDoubleOrNull() ?: 0.0
            val cash = etCash.text.toString().toDoubleOrNull() ?: 0.0
            val upi = etUpiPayment.text.toString().toDoubleOrNull() ?: 0.0
            val totalAmount = amount + addAmount
            val totalPaid = cash + upi
            var balance = totalAmount - totalPaid
            balance = (Math.floor(balance / 10.0) * 10).toDouble()

            // Only show balance if cash or upi is paid
            etBalance.setText(if (cash > 0.0 || upi > 0.0) balance.toInt().toString() else "")

            if (totalPaid > totalAmount) {
                tvError.text = "Total paid cannot exceed total amount"
                tvError.visibility = View.VISIBLE
            }
        }

        // Add TextChangedListeners to all relevant fields
        etCash.addTextChangedListener { updateBalanceAndError() }
        etUpiPayment.addTextChangedListener { updateBalanceAndError() }
        etAmount.addTextChangedListener { updateBalanceAndError() }
        etAddAmount.addTextChangedListener { updateBalanceAndError() }

        // Date picker
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                // Format date as dd-MM-yy using Locale and default timezone
                val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
                val dateStr = dateFormat.format(calendar.time)
                etDate.setText(dateStr)
//                etDate.setText(calendar.time.toString())
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (entry == null) "Add Grocery Entry" else "Edit Grocery Entry")
            .setView(dialogView)
            .setPositiveButton(if (entry == null) "Add" else "Update", null) // We'll override this below
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                tvError.visibility = View.GONE

                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val addAmount = etAddAmount.text.toString().toDoubleOrNull() ?: 0.0
                val cash = etCash.text.toString().toDoubleOrNull() ?: 0.0
                val upi = etUpiPayment.text.toString().toDoubleOrNull() ?: 0.0
                val totalAmount = amount + addAmount
                val totalPaid = cash + upi
                var balance = totalAmount - totalPaid
                balance = (Math.floor(balance / 10.0) * 10).toDouble()

                // Validation
                if (etDate.text.isNullOrBlank()) {
                    tvError.text = "Please select a date"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                if (amount <= 0.0) {
                    tvError.text = "Please enter a valid amount"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                if (totalPaid > totalAmount) {
                    tvError.text = "Total paid cannot exceed total amount"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                val newDateNorm = etDate.text.toString()//parseDate(etDate.text.toString())
                val duplicate = groceryEntries.any {
//                    val existingDate = parseDate(it.date)
                    val existingDate = it.date.toString()
                    existingDate == newDateNorm
                }

                // Check for duplicate date if adding new entry
                if (entry == null) {
                    if (duplicate) {
                        tvError.text = "An entry with this date already exists."
                        tvError.visibility = View.VISIBLE
                        return@setOnClickListener
                    }
                }

                val newEntry = GroceryEntry(
                    rowId = entry?.rowId ?: 0, // Preserve rowId for update, 0 for new
                    date = etDate.text.toString(),
                    amount = amount,
                    addAmount = addAmount,
                    cash = cash,
                    balance = balance,
                    upiPayment = upi,
                    comments = etComments.text.toString()
                )

                if (entry == null) {
                    GrocerySheetsHelper.addGroceryEntry(newEntry) { success ->
                        runOnUiThread {
                            if (success) {
//                                groceryEntries.add(0, newEntry)
//                                adapter.notifyItemInserted(0)
                                loadGroceryEntries()
                                dialog.dismiss()
                            } else {
                                tvError.text = "Failed to add entry. Please try again."
                                tvError.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    val updatedItem = entry.copy(
                        date = newEntry.date,
                        amount = newEntry.amount,
                        addAmount = newEntry.addAmount,
                        cash = newEntry.cash,
                        balance = newEntry.balance,
                        upiPayment =  newEntry.upiPayment,
                        comments = newEntry.comments)
                    GrocerySheetsHelper.updateGroceryEntry(updatedItem) { success ->
                        runOnUiThread {
                            if (success) {
//                                groceryEntries[position] = newEntry
//                                adapter.notifyItemChanged(position)
                                loadGroceryEntries()
                                dialog.dismiss()
                            } else {
                                tvError.text = "Failed to update entry. Please try again."
                                tvError.visibility = View.VISIBLE
                            }
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
                GrocerySheetsHelper.deleteGroceryEntry(entry) { success ->
                    runOnUiThread {
                        if (success) {
//                            groceryEntries.removeAt(position)
//                            adapter.notifyItemRemoved(position)
                            loadGroceryEntries()
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
