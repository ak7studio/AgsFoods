package com.ak7studio.agsfood


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShiftSelectionActivity : AppCompatActivity() {

    private lateinit var morningShiftButton: Button
    private lateinit var eveningShiftButton: Button
    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shift_selection)

        morningShiftButton = findViewById(R.id.buttonMorningShift)
        eveningShiftButton = findViewById(R.id.buttonEveningShift)

        // Get the phone number passed from the LoginActivity
        phoneNumber = intent.getStringExtra("phoneNumber")

        morningShiftButton.setOnClickListener {
            navigateToDataEntryScreen("Morning")
        }

        eveningShiftButton.setOnClickListener {
            navigateToDataEntryScreen("Evening")
        }
    }

    private fun navigateToDataEntryScreen(shift: String) {
        if (phoneNumber != null) {
            val intent = Intent(this, DataEntryActivity::class.java)
            intent.putExtra("phoneNumber", phoneNumber)
            intent.putExtra("shift", shift)
            startActivity(intent)
            finish() // Prevent going back to the shift selection screen after choosing a shift
        } else {
            Toast.makeText(this, "Error: Phone number not available. Please log in again.", Toast.LENGTH_LONG).show()
            // Optionally, navigate back to the login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}