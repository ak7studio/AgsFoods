package com.ak7studio.agsfood

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout

class DashboardActivity : BaseActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvWeather: TextView

    private lateinit var tvTodayExpenses: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: DailyExpenseAdapter
    private lateinit var layoutAddNew: LinearLayout
    private lateinit var radioGroupShiftMain: RadioGroup
    private val expensesList = mutableListOf<ExpenseItem>()
    private lateinit var itemNameHelper: ItemNameStorageHelper


    override fun getCurrentNavItemId(): Int = R.id.nav_dashboard

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_dashboard, contentFrameLayout, true)

        itemNameHelper = ItemNameStorageHelper(this)

        val userName = getUserNameFromPrefs()

        // Initialize TextViews
        tvWelcome = findViewById(R.id.tvWelcome)
        tvDateTime = findViewById(R.id.tvDateTime)
        tvWeather = findViewById(R.id.tvWeather)

        tvWelcome.text = "Welcome, $userName"
        tvDateTime.text = getCurrentDateTime()
        tvWeather.text = "Weather: 28°C, Clear" // Replace with real data if you want

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//---------------------------------------------------------------------------

        recyclerView = findViewById(R.id.recyclerView)
        layoutAddNew = findViewById(R.id.layoutAddNew)
        radioGroupShiftMain = findViewById(R.id.radioGroupShift)
        recyclerView.layoutManager = LinearLayoutManager(this)
        expenseAdapter = DailyExpenseAdapter(expensesList,
            onEdit = { item, pos -> showEditDialog(item, pos) },
            onDelete = { item, pos -> confirmDelete(item, pos) }
        )
        recyclerView.adapter = expenseAdapter

        radioGroupShiftMain.setOnCheckedChangeListener { _, _ ->
            loadExpenses()
        }
        loadExpenses();
        findViewById<Button>(R.id.btnShowAddDialog).setOnClickListener {
            showAddItemDialog()
        }

//---------------------------------------------------------------------

        tvTodayExpenses = findViewById(R.id.tvTodayExpenses)
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_daily_expense_entry, null)
        val autoCompleteItemName = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val radioGroupShift = dialogView.findViewById<RadioGroup>(R.id.radioGroupShift)
        val btnAddNewItem = dialogView.findViewById<ImageButton>(R.id.btnAddNewItem)
        val tvItemNameError = dialogView.findViewById<TextView>(R.id.tvItemNameError)

        // Load items from file
        val itemsList = itemNameHelper.loadItemNames()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, itemsList)
        autoCompleteItemName.setAdapter(adapter)
        autoCompleteItemName.isFocusable = true
        autoCompleteItemName.isFocusableInTouchMode = true
        autoCompleteItemName.threshold = 1

        // Validation function for item names (allow letters, numbers, spaces, some punctuation)
        fun isValidItemName(name: String): Boolean {
            val regex = "^[a-zA-Z0-9 .,'-]+$"  // adjust pattern as needed
            return name.matches(regex.toRegex())
        }

        // Add New Item button logic
        btnAddNewItem.setOnClickListener {
            tvItemNameError.visibility = View.GONE
            val newItem = autoCompleteItemName.text.toString().trim()
            when {
                newItem.isEmpty() -> {
                    tvItemNameError.text = "Please enter an item name"
                    tvItemNameError.visibility = View.VISIBLE
                }
                !isValidItemName(newItem) -> {
                    tvItemNameError.text = "Invalid characters in item name"
                    tvItemNameError.visibility = View.VISIBLE
                }
                itemsList.contains(newItem) -> {
                    tvItemNameError.text = "Item already exists"
                    tvItemNameError.visibility = View.VISIBLE
                }
                else -> {
                    itemsList.add(newItem)
                    itemNameHelper.saveItemNames(itemsList)
                    adapter.clear()
                    adapter.addAll(itemsList)
                    adapter.notifyDataSetChanged()
                    tvItemNameError.text = "Item added to list"
                    tvItemNameError.setTextColor(getColor(android.R.color.holo_green_dark))
                    tvItemNameError.visibility = View.VISIBLE
                    autoCompleteItemName.setText(newItem)
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Expense Item")
            .setView(dialogView)
            .setPositiveButton("Add", null) // We'll override this later
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addButton.setOnClickListener {
                tvItemNameError.visibility = View.GONE
                val name = autoCompleteItemName.text.toString().trim()
                val qtyText = etQuantity.text.toString().trim()
                val priceText = etPrice.text.toString().trim()
                val shift = when (radioGroupShift.checkedRadioButtonId) {
                    R.id.radioMorning -> "M"
                    R.id.radioEvening -> "E"
                    else -> "M"
                }

                when {
                    name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty() -> {
                        tvItemNameError.text = "Please fill all fields"
                        tvItemNameError.setTextColor(getColor(android.R.color.holo_red_dark))
                        tvItemNameError.visibility = View.VISIBLE
                    }
                    !itemsList.contains(name) -> {
                        tvItemNameError.text = "Select an item from the list or add new via '+'"
                        tvItemNameError.setTextColor(getColor(android.R.color.holo_red_dark))
                        tvItemNameError.visibility = View.VISIBLE
                    }
                    qtyText.toIntOrNull() == null || priceText.toDoubleOrNull() == null -> {
                        tvItemNameError.text = "Invalid quantity or price"
                        tvItemNameError.setTextColor(getColor(android.R.color.holo_red_dark))
                        tvItemNameError.visibility = View.VISIBLE
                    }
                    else -> {
                        val quantity = qtyText.toInt()
                        val price = priceText.toDouble()
                        val newItem = ExpenseItem(
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            shift = shift,
                            itemName = name,
                            quantity = quantity,
                            price = price
                        )
                        GoogleSheetsHelper.addExpense(newItem) { success ->
                            runOnUiThread {
                                if (success) {
                                    Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
                                    loadExpenses()
                                } else {
                                    Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }


    private fun showEditDialog(item: ExpenseItem, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_daily_expense_entry, null)
        val autoCompleteItemName = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val radioGroupShift = dialogView.findViewById<RadioGroup>(R.id.radioGroupShift)

        autoCompleteItemName.setText(item.itemName)
        etQuantity.setText(item.quantity.toString())
        etPrice.setText(item.price.toString())
        if (item.shift == "M") radioGroupShift.check(R.id.radioMorning) else radioGroupShift.check(R.id.radioEvening)

        AlertDialog.Builder(this)
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Update") { dialog, _ ->
                val name = autoCompleteItemName.text.toString().trim()
                val qty = etQuantity.text.toString().toIntOrNull()
                val price = etPrice.text.toString().toDoubleOrNull()
                val shift = if (radioGroupShift.checkedRadioButtonId == R.id.radioMorning) "M" else "E"

                if (name.isEmpty() || qty == null || price == null) {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedItem = item.copy(itemName = name, quantity = qty, price = price, shift = shift)

                // Update in Google Sheets
                GoogleSheetsHelper.updateExpense(updatedItem) { success ->
                    runOnUiThread {
                        if (success) {
                            expensesList[position] = updatedItem
                            expenseAdapter.notifyItemChanged(position)
                            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun confirmDelete(item: ExpenseItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete ${item.itemName}?")
            .setPositiveButton("Delete") { dialog, _ ->
                GoogleSheetsHelper.deleteExpense(item) { success ->
                    runOnUiThread {
                        if (success) {
                            expensesList.removeAt(position)
                            expenseAdapter.notifyItemRemoved(position)
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getUserNameFromPrefs(isrole:Boolean = false): String {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        /*if(isrole){
            return prefs.getString("role", "Cashier") ?: "Cashier"
        }*/
        return prefs.getString("username", "User") ?: "User"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy, h:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadExpenses() {
        var shiftfilter = when (radioGroupShiftMain.checkedRadioButtonId) {
            R.id.radioMorning -> "M"
            R.id.radioEvening -> "E"
            R.id.radioBoth -> "B"
            else -> "M"
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        GoogleSheetsHelper.fetchExpenses { expenses ->
            runOnUiThread {
                expensesList.clear()
                val filtered = when (shiftfilter) {
                    "M" -> expenses.filter { parseUtcToLocalDate(it.date) == today && it.shift == "M" }
                    "E" -> expenses.filter { parseUtcToLocalDate(it.date) == today && it.shift == "E" }
                    "B" -> expenses.filter { parseUtcToLocalDate(it.date) == today && (it.shift == "M" || it.shift == "E") }
                    else -> expenses.filter { parseUtcToLocalDate(it.date) == today && it.shift == "M" }
                }
                expensesList.addAll(filtered)
                expenseAdapter.notifyDataSetChanged()
                val todayExpenses = filtered;
                val totalExpenses = todayExpenses.sumOf { it.price }.toInt()
                tvTodayExpenses.text = "Today's Expenses: ₹$totalExpenses"
            }
        }
    }

    fun parseUtcToLocalDate(utcString: String): String {
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            utcFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = utcFormat.parse(utcString)
            val localFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            localFormat.timeZone = TimeZone.getDefault() // Your device's timezone (IST)
            localFormat.format(date!!)
        } catch (e: Exception) {
            utcString // fallback
        }
    }


}
