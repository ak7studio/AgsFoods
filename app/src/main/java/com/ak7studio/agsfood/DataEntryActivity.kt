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
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Locale

data class KioskData(
    var id: String? = null,  // Firebase key
    var date: String? = null,
    var timestamp: String? = null,
    var phone: String? = null,
    var shift: String? = null,
    var sales: Double? = null,
    var expenses: Double? = null
)

data class DayEntry(
    val date: String,
    val morning: KioskData? = null,
    val evening: KioskData? = null


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
//    private lateinit var adapter: KioskDataAdapter
    private val dataList = mutableListOf<KioskData>()
    private var userRole: String = "cashier" // Or fetch dynamically
    private lateinit var radioGroupShift: RadioGroup
    private lateinit var cancelButton: Button
    private lateinit var selectDateButton: Button
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_entry)

        selectDateButton = findViewById(R.id.buttonSelectDate)

        // Set default date to today
        val calendar = Calendar.getInstance()
        updateSelectedDate(calendar)

        // Show DatePickerDialog when button is clicked
        selectDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                updateSelectedDate(calendar)
            }, year, month, day)
            // Restrict to today or earlier
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }


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




        // Get the phone number and shift passed from the ShiftSelectionActivity
        phoneNumber = intent.getStringExtra("phoneNumber")

        recyclerView = findViewById(R.id.recyclerViewDataList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        submitDataButton.setOnClickListener {
            val totalSales = totalSalesEditText.text.toString().trim()
            val expenses = expensesEditText.text.toString().trim()

            // When saving/submitting data:
            shift = when (radioGroupShift.checkedRadioButtonId) {
                R.id.radioMorning -> "Morning"
                R.id.radioEvening -> "Evening"
                else -> "Morning" // Default
            }

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
                if (selectedDate == null) {
                    Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show()
                    return
                }

                // --- Date check: Prevent future dates ---
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selected = sdf.parse(selectedDate!!)
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val today = todayCal.time

                if (selected != null && selected.after(today)) {
                    Toast.makeText(this, "Selected date cannot be in the future.", Toast.LENGTH_SHORT).show()
                    return
                }
                // --- End date check ---

                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
                val data = HashMap<String, Any>()
                data["date"] = selectedDate!!
                data["timestamp"] = timestamp
                data["phone"] = phone
                data["shift"] = currentShift
                data["sales"] = totalSales
                data["expenses"] = expenses

                // Reference to nested structure
                val baseRef = dataRef.child(phone).child(selectedDate!!).child(currentShift)

                // Overwrite the latest (current) data
                baseRef.child("currentData").setValue(data)
                    .addOnSuccessListener {
                        // Add to history (append)
                        baseRef.child("history").push().setValue(data)
                            .addOnSuccessListener {
                                Toast.makeText(this@DataEntryActivity, "Data submitted successfully", Toast.LENGTH_SHORT).show()
                                val kioskData = KioskData(
                                    id = null, // You can set this to the push key if needed
                                    date = selectedDate,
                                    timestamp = timestamp,
                                    phone = phone,
                                    shift = currentShift,
                                    sales = totalSales,
                                    expenses = expenses
                                )
                                openEditScreen(kioskData)
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(this@DataEntryActivity, "Failed to update history: ${error.message}", Toast.LENGTH_LONG).show()
                            }
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
            putExtra("date", data.date)
            putExtra("timestamp", data.timestamp)
            putExtra("phone", data.phone)
            putExtra("shift", data.shift)
            putExtra("sales", data.sales)
            putExtra("expenses", data.expenses)
            putExtra("userRole", userRole)
        }
        startActivity(intent)
    }

    private fun updateSelectedDate(calendar: Calendar) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(calendar.time)
        selectDateButton.text = selectedDate
    }


}