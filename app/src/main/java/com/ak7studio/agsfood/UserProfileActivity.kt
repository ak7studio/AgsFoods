package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UserProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var cRole: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: Button
    private var phoneNumber: String? = null
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        etName = findViewById(R.id.etName)
        cRole = findViewById(R.id.cRole)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnSave = findViewById(R.id.btnSaveProfile)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        phoneNumber = prefs.getString("phoneNumber", null)
        userRole = prefs.getString("userRole", "cashier")

        loadProfile()

        val backButton = findViewById<Button>(R.id.buttonBack)
        backButton.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Only admin can edit name/role
        val isAdmin = userRole == "admin"
        etName.isEnabled = true
        cRole.isEnabled = isAdmin;
        spinnerRole.isEnabled = isAdmin
        btnSave.visibility = View.VISIBLE; //if (isAdmin) View.VISIBLE else View.GONE

        btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadProfile() {
        if (phoneNumber == null) return
        val userRef = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/")
            .getReference("users").child(phoneNumber!!)
        userRef.get().addOnSuccessListener { snapshot ->
            etName.setText(snapshot.child("name").getValue(String::class.java) ?: "")
            val role = snapshot.child("role").getValue(String::class.java) ?: "cashier"
            cRole.setText(role)
            val roles = resources.getStringArray(R.array.user_roles)
            spinnerRole.setSelection(roles.indexOf(role))
        }
    }

    private fun saveProfile() {
        if (phoneNumber == null) return
        val name = etName.text.toString().trim()
        val isAdmin = userRole == "admin"
        val role = if (isAdmin) spinnerRole.selectedItem.toString() else cRole.text.toString()
        val userRef = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/")
            .getReference("users").child(phoneNumber!!)
        userRef.child("name").setValue(name)
        userRef.child("role").setValue(role)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().putString("phoneNumber", phoneNumber).apply()
        prefs.edit().putString("name", name).apply()
        prefs.edit().putString("role", role).apply()

        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

    }
}
