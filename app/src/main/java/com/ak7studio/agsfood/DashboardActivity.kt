package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val fetchDataBtn = findViewById<Button>(R.id.buttonFetchData)
        val profileBtn = findViewById<Button>(R.id.buttonProfile)
        val updateSaleBtn = findViewById<Button>(R.id.buttonUpdateSale)
        val menuBtn = findViewById<ImageButton>(R.id.buttonMenu)


        fetchDataBtn.setOnClickListener {
//            startActivity(Intent(this, EditDataActivity::class.java))
            navigateToFetchDataScreen()
        }

        profileBtn.setOnClickListener {
            // Start profile activity or show profile info
        }

        updateSaleBtn.setOnClickListener {
            // Navigate to ShiftSelectionActivity
//            startActivity(Intent(this, ShiftSelectionActivity::class.java))
            navigateToDataEntryScreen()
        }

        menuBtn.setOnClickListener {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToShiftSelectionScreen() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val phoneNumber = prefs.getString("phoneNumber", null)
        val intent = Intent(this, ShiftSelectionActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        startActivity(intent)
    }

    private fun navigateToDataEntryScreen() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val phoneNumber = prefs.getString("phoneNumber", null)
        val intent = Intent(this, DataEntryActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        startActivity(intent)
    }

    private fun navigateToFetchDataScreen() {
//        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
//        val phoneNumber = prefs.getString("phoneNumber", null)
        val intent = Intent(this, EditDataActivity::class.java)
//        intent.putExtra("phoneNumber", phoneNumber)
        startActivity(intent)
    }
}
