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
import java.text.SimpleDateFormat
import java.util.*

class EditDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DayEntryAdapter
    private val dataList = mutableListOf<KioskData>()
    private var userRole: String? = null

    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/")
    private val dataRef = database.getReference("kiosk_data")

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var spinnerFilter: Spinner
    private var allDayEntries: List<DayEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_data)

        recyclerView = findViewById(R.id.recyclerViewDataList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        spinnerFilter = findViewById(R.id.spinnerFilter)
        adapter = DayEntryAdapter(emptyList())
        recyclerView.adapter = adapter


        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val period = when (position) {
                    1 -> "week"
                    2 -> "month"
                    3 -> "year"
                    else -> "all"
                }
                applyFilter(period)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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
                R.id.nav_updateSalesExpense -> {
                    startActivity(Intent(this, DataEntryActivity::class.java))
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
        }

        userRole = intent.getStringExtra("userRole")

        fetchAllKioskData()
    }

    private fun fetchAllKioskData() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
//        val currentMonth = sdf.format(Calendar.getInstance().time)
        dataRef.get().addOnSuccessListener { snapshot ->
            val dayEntries = mutableListOf<DayEntry>()
            for (monthSnap in snapshot.children) {
                for (dateSnap in monthSnap.children) {
                    val date = dateSnap.key ?: continue
                    val morning = dateSnap.child("Morning").child("currentData").getValue(KioskData::class.java)
                    val evening = dateSnap.child("Evening").child("currentData").getValue(KioskData::class.java)
                    dayEntries.add(DayEntry(date, morning, evening))
                }
            }
            allDayEntries = dayEntries.sortedByDescending { it.date }
            applyFilter("all")
            adapter.submitList(allDayEntries)
            recyclerView.visibility = if (allDayEntries.isNotEmpty()) View.VISIBLE else View.GONE
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to fetch data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun filterByPeriod(
        entries: List<DayEntry>,
        period: String, // "all", "week", "month", "year"
        referenceDate: Date = Date()
    ): List<DayEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { time = referenceDate }

        return when (period.lowercase()) {
            "week" -> {
                val targetWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                val targetYear = calendar.get(Calendar.YEAR)
                entries.filter { entry ->
                    val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null }
                    if (entryDate != null) {
                        val entryCal = Calendar.getInstance().apply { time = entryDate }
                        entryCal.get(Calendar.WEEK_OF_YEAR) == targetWeek &&
                                entryCal.get(Calendar.YEAR) == targetYear
                    } else false
                }
            }
            "month" -> {
                val targetMonth = calendar.get(Calendar.MONTH)
                val targetYear = calendar.get(Calendar.YEAR)
                entries.filter { entry ->
                    val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null }
                    if (entryDate != null) {
                        val entryCal = Calendar.getInstance().apply { time = entryDate }
                        entryCal.get(Calendar.MONTH) == targetMonth &&
                                entryCal.get(Calendar.YEAR) == targetYear
                    } else false
                }
            }
            "year" -> {
                val targetYear = calendar.get(Calendar.YEAR)
                entries.filter { entry ->
                    val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null }
                    if (entryDate != null) {
                        val entryCal = Calendar.getInstance().apply { time = entryDate }
                        entryCal.get(Calendar.YEAR) == targetYear
                    } else false
                }
            }
            else -> entries // "all" or any other input returns all entries
        }
    }

    private fun applyFilter(period: String) {
        val filtered = filterByPeriod(allDayEntries, period)
        adapter.submitList(filtered)
        recyclerView.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
    }
}

