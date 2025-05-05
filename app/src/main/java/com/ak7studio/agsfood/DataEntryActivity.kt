package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class KioskData(
    var id: String? = null,  // Firebase key
    var timestamp: String? = null,
    var phone: String? = null,
    var shift: String? = null,
    var sales: Double? = null,
    var expenses: Double? = null
)

class DataEntryActivity : AppCompatActivity() {

    private lateinit var totalSalesEditText: EditText
    private lateinit var expensesEditText: EditText
    private lateinit var submitDataButton: Button
    private var phoneNumber: String? = null
    private var shift: String? = null
    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/")
    private val dataRef = database.getReference("kiosk_data") // You can customize the node name

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KioskDataAdapter
    private val dataList = mutableListOf<KioskData>()
    private var userRole: String = "cashier" // Or fetch dynamically
    private lateinit var radioGroupShift: RadioGroup
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_entry)

        totalSalesEditText = findViewById(R.id.editTextTotalSales)
        expensesEditText = findViewById(R.id.editTextExpenses)
        submitDataButton = findViewById(R.id.buttonSubmitData)
        radioGroupShift = findViewById(R.id.radioGroupShift)
        cancelButton = findViewById(R.id.buttonCancel)
        cancelButton.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // When saving/submitting data:
        shift = when (radioGroupShift.checkedRadioButtonId) {
            R.id.radioMorning -> "Morning"
            R.id.radioEvening -> "Evening"
            else -> "Morning" // Default
        }

        // Get the phone number and shift passed from the ShiftSelectionActivity
        phoneNumber = intent.getStringExtra("phoneNumber")

        recyclerView = findViewById(R.id.recyclerViewDataList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        submitDataButton.setOnClickListener {
            val totalSales = totalSalesEditText.text.toString().trim()
            val expenses = expensesEditText.text.toString().trim()

            if (totalSales.isNotEmpty() && expenses.isNotEmpty()) {
                // Show confirmation dialog
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Confirm Submission")
                    .setMessage("Are you sure you want to submit this data?")
                    .setPositiveButton("Yes") { _, _ ->
                        saveDataToFirebase(totalSales.toDouble(), expenses.toDouble())
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                Toast.makeText(this, "Please enter both total sales and expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDataToFirebase(totalSales: Double, expenses: Double) {
        phoneNumber?.let { phone ->
            shift?.let { currentShift ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
                val data = HashMap<String, Any>()
                data["timestamp"] = timestamp
                data["phone"] = phone
                data["shift"] = currentShift
                data["sales"] = totalSales
                data["expenses"] = expenses

                val newRef = dataRef.push()
                newRef.setValue(data)
                    .addOnSuccessListener {
                        Toast.makeText(this@DataEntryActivity, "Data submitted successfully", Toast.LENGTH_SHORT).show()
                        // Prepare to open EditDataActivity with the new data
                        val kioskData = KioskData(
                            id = newRef.key,
                            timestamp = timestamp,
                            phone = phone,
                            shift = currentShift,
                            sales = totalSales,
                            expenses = expenses
                        )
                        openEditScreen(kioskData)
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this@DataEntryActivity, "Failed to submit data: ${error.message}", Toast.LENGTH_LONG).show()
                        android.util.Log.e("Firebase Database", "Error saving data", error)
                    }
            } ?: run {
                Toast.makeText(this, "Error: Shift information not available.", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "Error: Phone number not available.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openEditScreen(data: KioskData) {
        // Open a new activity or dialog to edit the selected data
        // Pass the data object via Intent or Bundle
        val intent = Intent(this, EditDataActivity::class.java).apply {
            putExtra("dataId", data.id)
            putExtra("timestamp", data.timestamp)
            putExtra("phone", data.phone)
            putExtra("shift", data.shift)
            putExtra("sales", data.sales)
            putExtra("expenses", data.expenses)
            putExtra("userRole", userRole)
        }
        startActivity(intent)
    }


}