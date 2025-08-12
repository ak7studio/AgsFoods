package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class FetchSalesData : BaseActivity() {

    private lateinit var recyclerView: RecyclerView

    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val dataRef = database.getReference("kiosk_data")
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = database.getReference("users")

    private var allDayEntries: List<DayEntry> = emptyList()
    private var userRole: String = UserPrefs.KEY_CASHIER // Default role

    override fun getCurrentNavItemId(): Int = R.id.nav_fetchSalesExpense

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_fetch_sales, contentFrameLayout, true)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val filterSpinner = findViewById<Spinner>(R.id.filterSpinner)

        // Define filter options
        val filterOptions = listOf("Daywise", "Weekwise", "Monthwise")

        // Create ArrayAdapter for Spinner
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            filterOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        // Prevent initial selection triggering data load
        var isSpinnerInitialized = false

        // Fetch the user role first, then proceed with setting up the UI and fetching data
        getUserRoleFromFirebase { role ->
            // Trim whitespace and convert to lowercase for robust comparison
            userRole = role.trim().lowercase(Locale.getDefault())
            Toast.makeText(this, "Logged in as $userRole", Toast.LENGTH_SHORT).show()

            filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    if (!isSpinnerInitialized) {
                        isSpinnerInitialized = true
                        return
                    }

                    val selectedFilter = filterOptions[position].lowercase(Locale.getDefault())
                    fetchAllKioskData(selectedFilter) { filteredEntries ->
                        setupViewPagerWithFragments(filteredEntries, tabLayout, viewPager)
                        recyclerView.visibility = if (filteredEntries.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optionally handle no selection
                }
            }

            // Load default filter data (e.g., Daywise)
            filterSpinner.setSelection(0)
            fetchAllKioskData("Daywise") { filteredEntries ->
                setupViewPagerWithFragments(filteredEntries, tabLayout, viewPager)
                recyclerView.visibility = if (filteredEntries.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Fetches the current user's role from Firebase.
     * @param onRoleReady callback with the user's role string.
     */
    private fun getUserRoleFromFirebase(onRoleReady: (String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onRoleReady(UserPrefs.KEY_CASHIER) // Default role if not logged in
            return
        }

        usersRef.child(userId).child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.getValue(String::class.java)
                onRoleReady(role ?: UserPrefs.KEY_CASHIER) // Default role if not found
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FetchSalesData, "Failed to fetch user role: ${error.message}", Toast.LENGTH_LONG).show()
                onRoleReady(UserPrefs.KEY_CASHIER) // Default role on error
            }
        })
    }

    private fun setupViewPagerWithFragments(
        dayEntries: List<DayEntry>,
        tabLayout: TabLayout,
        viewPager: ViewPager2
    ) {
        val fragments = listOf(
            SalesFragment(dayEntries),
            ExpensesFragment(dayEntries)
        )
        val titles = listOf("Sales", "Expenses")

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    /**
     * Fetches all kiosk data and applies the requested filter.
     * @param filterType "daywise", "weekwise", or "monthwise"
     * @param onDataReady callback with filtered and/or aggregated list
     */
    private fun fetchAllKioskData(
        filterType: String,
        onDataReady: (List<DayEntry>) -> Unit
    ) {
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

            // First, filter the data based on the user's role
            val baseEntries = when (userRole) {
                UserPrefs.KEY_ADMIN.lowercase(Locale.getDefault()) -> when (filterType) {
                    "daywise", "weekwise" -> filterCurrentMonth(allDayEntries)
                    "monthwise" -> allDayEntries
                    else -> allDayEntries
                }
                UserPrefs.KEY_CASHIER.lowercase(Locale.getDefault()) -> filterLast(allDayEntries, 7) // Cashier sees last 7 days
                else -> allDayEntries // Default for other roles
            }

            // Then, apply the selected filter type to the base data
            val filteredAndAggregated = when (filterType.lowercase(Locale.getDefault())) {
                "daywise" -> baseEntries
                "weekwise" -> aggregateByWeek(baseEntries)
                "monthwise" -> aggregateByMonth(baseEntries)
                else -> baseEntries
            }

            onDataReady(filteredAndAggregated)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to fetch data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Filters entries from the current month.
    private fun filterCurrentMonth(entries: List<DayEntry>): List<DayEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        return entries.filter { entry ->
            val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null }
            entryDate != null && SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(entryDate) == currentMonth
        }
    }

    // Filters entries from the last 'days' days.
    private fun filterLast(entries: List<DayEntry>, days: Int): List<DayEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val cutoffDate = calendar.time

        return entries.filter { entry ->
            val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null }
            entryDate != null && !entryDate.before(cutoffDate)
        }
    }

    // Aggregate entries by week (weekwise)
    private fun aggregateByWeek(entries: List<DayEntry>): List<DayEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekFormat = SimpleDateFormat("dd-MM", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val weekMap = mutableMapOf<String, Pair<KioskData?, KioskData?>>()

        entries.forEach { entry ->
            val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null } ?: return@forEach
            calendar.time = entryDate

            // Set to Monday (start of week)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = calendar.time
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val weekEnd = calendar.time

            val weekKey = "${weekFormat.format(weekStart)} - ${weekFormat.format(weekEnd)}"

            val existing = weekMap[weekKey]
            val morningSum = sumKioskData(existing?.first, entry.morning)
            val eveningSum = sumKioskData(existing?.second, entry.evening)

            weekMap[weekKey] = Pair(morningSum, eveningSum)
        }

        return weekMap.map { (weekRange, dataPair) ->
            DayEntry(
                date = weekRange,
                morning = dataPair.first,
                evening = dataPair.second
            )
        }.sortedByDescending { it.date }
    }

    // Aggregate entries by month (monthwise)
    private fun aggregateByMonth(entries: List<DayEntry>): List<DayEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMM yy", Locale.getDefault())

        val monthMap = mutableMapOf<String, Triple<Date, KioskData?, KioskData?>>()

        entries.forEach { entry ->
            val entryDate = try { sdf.parse(entry.date) } catch (e: Exception) { null } ?: return@forEach
            val monthKey = monthFormat.format(entryDate)

            val existing = monthMap[monthKey]
            val morningSum = sumKioskData(existing?.second, entry.morning)
            val eveningSum = sumKioskData(existing?.third, entry.evening)

            monthMap[monthKey] = Triple(entryDate, morningSum, eveningSum)
        }

        return monthMap.values.map { (date, morningData, eveningData) ->
            DayEntry(
                date = displayFormat.format(date),
                morning = morningData,
                evening = eveningData
            )
        }.sortedByDescending {
            // Parse the month string back to a date for correct sorting
            displayFormat.parse(it.date)
        }
    }

    /**
     * Helper to sum two KioskData objects.
     * Adjust this based on your actual KioskData fields.
     */
    private fun sumKioskData(a: KioskData?, b: KioskData?): KioskData? {
        if (a == null) return b
        if (b == null) return a

        return KioskData(
            id = null,           // aggregated data doesn't have a single id
            date = null,         // aggregated date handled in DayEntry.date
            timestamp = null,
            username = null,
            shift = null,
            sales = (a.sales ?: 0.0) + (b.sales ?: 0.0),
            expenses = (a.expenses ?: 0.0) + (b.expenses ?: 0.0)
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
