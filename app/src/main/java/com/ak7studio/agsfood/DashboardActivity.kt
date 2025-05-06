package com.ak7studio.agsfood

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvWeather: TextView

    private lateinit var tvTodayExpenses: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var layoutAddNew: LinearLayout
    private lateinit var radioGroupShiftMain: RadioGroup
    private val expensesList = mutableListOf<ExpenseItem>()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "AgsFoods"
        // Show hamburger icon
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_16)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Hamburger icon opens the drawer
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_fetchSalesExpense -> {
                    navigateToFetchDataScreen()
                    true
                }
                R.id.nav_updateSalesExpense -> {
                    navigateToDataEntryScreen()
                    true
                }
                R.id.nav_updateprofile -> {
                    // Navigate to profile
                    navigateToUserProfileScreen()
                    true
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // Show user name as subtitle
        val userName = getUserNameFromPrefs()
        toolbar.subtitle = getUserNameFromPrefs(true)

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
        expenseAdapter = ExpenseAdapter(expensesList,
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val etItemName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val radioGroupShift = dialogView.findViewById<RadioGroup>(R.id.radioGroupShift)

        AlertDialog.Builder(this)
            .setTitle("Add Expense Item")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = etItemName.text.toString().trim()
                val qtyText = etQuantity.text.toString().trim()
                val priceText = etPrice.text.toString().trim()
                val shift = when (radioGroupShift.checkedRadioButtonId) {
                    R.id.radioMorning -> "M"
                    R.id.radioEvening -> "E"
                    else -> "M"
                }

                if (name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val quantity = qtyText.toIntOrNull()
                val price = priceText.toDoubleOrNull()
                if (quantity == null || price == null) {
                    Toast.makeText(this, "Invalid quantity or price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

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
                            loadExpenses() // Refresh the list
                        } else {
                            Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEditDialog(item: ExpenseItem, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val etItemName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val radioGroupShift = dialogView.findViewById<RadioGroup>(R.id.radioGroupShift)

        etItemName.setText(item.itemName)
        etQuantity.setText(item.quantity.toString())
        etPrice.setText(item.price.toString())
        if (item.shift == "M") radioGroupShift.check(R.id.radioMorning) else radioGroupShift.check(R.id.radioEvening)

        AlertDialog.Builder(this)
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Update") { dialog, _ ->
                val name = etItemName.text.toString().trim()
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
        if(isrole){
            return prefs.getString("role", "Cashier") ?: "Cashier"
        }
        return prefs.getString("name", "User") ?: "User"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy, h:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun navigateToDataEntryScreen() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val phoneNumber = prefs.getString("phoneNumber", null)
        val intent = Intent(this, DataEntryActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        startActivity(intent)
    }

    private fun navigateToFetchDataScreen() {
        val intent = Intent(this, EditDataActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToUserProfileScreen() {
        val intent = Intent(this, UserProfileActivity::class.java)
        startActivity(intent)
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
                val totalExpenses = todayExpenses.sumOf { it.price }
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
