package com.ak7studio.agsfood

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroceryMerchantActivity : BaseActivity() {

    private val groceryEntries = mutableListOf<GroceryEntry>()
    private lateinit var adapter: GroceryEntryAdapter
//    private lateinit var drawerLayout: DrawerLayout
//    private lateinit var navView: NavigationView

    override fun getCurrentNavItemId(): Int = R.id.nav_updateGroceryExpense

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_grocery_merchant)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_grocery_merchant, contentFrameLayout, true)

        /*val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Show hamburger icon
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_16) // Add ic_menu.png/svg in res/drawable

        drawerLayout = findViewById(R.id.grocery_drawer_layout)
        navView = findViewById(R.id.grocery_nav_view)

        // Hamburger icon opens the drawer
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.nav_fetchSalesExpense -> {
                    startActivity(Intent(this, EditDataActivity::class.java))
                    true
                }
                R.id.nav_updateGroceryExpense -> {
                    startActivity(Intent(this, GroceryMerchantActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, UserProfileActivity::class.java))
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
        }*/

        adapter = GroceryEntryAdapter(
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
            etAmount.setText(it.amount.toString())
            etAddAmount.setText(it.addAmount.toString())
            etCash.setText(it.cash.toString())
            etBalance.setText(it.balance.toString())
            etUpiPayment.setText(it.upiPayment.toString())
            etComments.setText(it.comments)
        }

        fun updateBalanceAndError() {
            tvError.visibility = View.GONE
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val addAmount = etAddAmount.text.toString().toDoubleOrNull() ?: 0.0
            val cash = etCash.text.toString().toDoubleOrNull() ?: 0.0
            val upi = etUpiPayment.text.toString().toDoubleOrNull() ?: 0.0
            val totalAmount = amount + addAmount
            val totalPaid = cash + upi
            val balance = totalAmount - totalPaid

            // Only show balance if cash or upi is paid
            etBalance.setText(if (cash > 0.0 || upi > 0.0) balance.toString() else "")

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
                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(calendar.time)
                etDate.setText(dateStr)
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
                val balance = totalAmount - totalPaid

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
                if (cash == 0.0 && upi == 0.0) {
                    tvError.text = "Please enter either cash or UPI payment"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                val newEntry = GroceryEntry(
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
                                groceryEntries.add(0, newEntry)
                                adapter.notifyItemInserted(0)
                                dialog.dismiss()
                            } else {
                                tvError.text = "Failed to add entry. Please try again."
                                tvError.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    GrocerySheetsHelper.updateGroceryEntry(newEntry) { success ->
                        runOnUiThread {
                            if (success) {
                                groceryEntries[position] = newEntry
                                adapter.notifyItemChanged(position)
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
                            groceryEntries.removeAt(position)
                            adapter.notifyItemRemoved(position)
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
