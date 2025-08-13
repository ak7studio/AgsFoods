package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView
    protected lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    protected var showToolbarAndDrawer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // Initialize FirebaseApp and perform a null-safe check
        val firebaseApp = FirebaseApp.initializeApp(this)

        // Use a 'let' block to ensure firebaseApp is not null before proceeding
        firebaseApp?.let { app ->
            val firebaseAppCheck = FirebaseAppCheck.getInstance(app)
            try {
                // Attempt to install the Play Integrity provider
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            } catch (e: Exception) {
                // Log the error and allow the app to continue without App Check
                // This handles cases where the Play Integrity API fails on a device
                Log.e("AppCheck", "Failed to install Play Integrity provider: ${e.message}")
            }
        }


        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        if (showToolbarAndDrawer) {
            toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
            navigationView.setNavigationItemSelectedListener(this)
            supportActionBar?.show()
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.isDrawerIndicatorEnabled = false
            navigationView.setNavigationItemSelectedListener(this)
            supportActionBar?.show()
            highlightCurrentMenuItem()
        }
    }

    private fun highlightCurrentMenuItem() {
        val menu = navigationView.menu
        val currentItemId = getCurrentNavItemId()
        if (currentItemId != 0) {
            menu.findItem(currentItemId)?.isChecked = true
        }
    }

    abstract fun getCurrentNavItemId(): Int

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_admin -> {
                if (UserPrefs.getRole() == "admin") {
                    startActivity(Intent(this, AdminActivity::class.java))
                    item.isVisible = UserPrefs.getRole() == "admin"
                    return true
                }
            }
        }

        if (item.itemId == getCurrentNavItemId()) {
            // Already on this screen
            return true
        }

        when (item.itemId) {
            R.id.nav_dashboard -> startActivity(Intent(this, DashboardActivity::class.java))
            R.id.nav_fetchSalesExpense -> startActivity(Intent(this, FetchSalesData::class.java))
            R.id.nav_updateSalesExpense -> startActivity(Intent(this, SalesExpensesActivity::class.java))
            R.id.nav_updateGroceryExpense -> startActivity(Intent(this, GroceryMerchantActivity::class.java))
            R.id.nav_updateProfile -> startActivity(Intent(this, CreateUsernameActivity::class.java))
            R.id.nav_admin -> startActivity(Intent(this, AdminActivity::class.java))
            R.id.nav_forecast -> startActivity(Intent(this, SaleForecastActivity::class.java))
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
        }
        return true
    }

    private fun getUserNameFromPrefs(isrole: Boolean = false): String {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return if (isrole) {
            prefs.getString("role", "Cashier") ?: "Cashier"
        } else {
            prefs.getString("name", "User") ?: "User"
        }
    }

}

