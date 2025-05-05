package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvWeather: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val fetchDataBtn = findViewById<Button>(R.id.buttonFetchData)
        val profileBtn = findViewById<Button>(R.id.buttonProfile)
        val updateSaleBtn = findViewById<Button>(R.id.buttonUpdateSale)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "AgsFoods"

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

        fetchDataBtn.setOnClickListener {
            navigateToFetchDataScreen()
        }

        profileBtn.setOnClickListener {
            navigateToUserProfileScreen()
        }

        updateSaleBtn.setOnClickListener {
            navigateToDataEntryScreen()
        }
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
}
