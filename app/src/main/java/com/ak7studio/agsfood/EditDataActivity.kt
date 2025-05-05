package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class EditDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KioskDataAdapter
    private val dataList = mutableListOf<KioskData>()
    private var userRole: String? = null

    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/")
    private val dataRef = database.getReference("kiosk_data")

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_data)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Show hamburger icon
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_16) // Add ic_menu.png/svg in res/drawable

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
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Navigate to profile
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

        /*// Handle hamburger click (optional)
        toolbar.setNavigationOnClickListener {
            // You can open a drawer or show a Toast for now
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }*/

        recyclerView = findViewById(R.id.recyclerViewDataList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        userRole = intent.getStringExtra("userRole")

        fetchAllKioskData()
    }

    private fun fetchAllKioskData() {
        dataRef.get().addOnSuccessListener { snapshot ->
            dataList.clear()
            for (child in snapshot.children) {
                val data = child.getValue(KioskData::class.java)
                data?.id = child.key
                data?.let { dataList.add(it) }
            }
            if (dataList.isNotEmpty()) {
                adapter = KioskDataAdapter(dataList, userRole ?: "cashier") { dataToEdit ->
                    openEditScreen(dataToEdit)
                }
                recyclerView.adapter = adapter
                recyclerView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.GONE
                Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to fetch data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openEditScreen(data: KioskData) {
        val intent = Intent(this, EditDataActivity::class.java) // Rename if needed
        intent.putExtra("dataId", data.id)
        intent.putExtra("timestamp", data.timestamp)
        intent.putExtra("phone", data.phone)
        intent.putExtra("shift", data.shift)
        intent.putExtra("sales", data.sales)
        intent.putExtra("expenses", data.expenses)
        intent.putExtra("userRole", userRole)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}

