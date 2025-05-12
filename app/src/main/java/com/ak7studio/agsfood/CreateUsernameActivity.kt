package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CreateUsernameActivity : BaseActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var radioGroupRole: RadioGroup

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    private var userId: String? = null
    private var userEmail: String? = null

    override fun getCurrentNavItemId(): Int = R.id.nav_updateProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_create_username)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_create_username, contentFrameLayout, true)
        auth = FirebaseAuth.getInstance()

        usernameEditText = findViewById(R.id.editTextUsername)
        submitButton = findViewById(R.id.buttonSubmitUsername)
        radioGroupRole = findViewById(R.id.radioGroupRole)

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid
        userEmail = auth.currentUser?.email

        if (userId == null || userEmail == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Optionally, load current username and role to prefill UI
        loadCurrentUserInfo()

        submitButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val role = getSelectedRole()
            checkUsernameAndSave(username, role)
        }
    }

    private fun loadCurrentUserInfo() {
        database.child("users").child(userId!!).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java)
                val role = snapshot.child("role").getValue(String::class.java)
                usernameEditText.setText(username)
                // Set radio button based on role
                when (role) {
                    "cashier" -> radioGroupRole.check(R.id.radioCashier)
                    "manager" -> radioGroupRole.check(R.id.radioManager)
                    "admin" -> radioGroupRole.check(R.id.radioAdmin)
                }
            }
        }
    }

    private fun getSelectedRole(): String {
        return when (radioGroupRole.checkedRadioButtonId) {
            R.id.radioCashier -> "cashier"
            R.id.radioManager -> "manager"
            R.id.radioAdmin -> "admin"
            else -> "cashier" // default
        }
    }

    private fun checkUsernameAndSave(username: String, role: String) {
        val usernamesRef = database.child("usernames").child(username)
        usernamesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot.exists()) {
                    val existingEmail = snapshot.getValue(String::class.java)
                    if (existingEmail == userEmail) {
                        // Username belongs to current user, allow update
                        saveUsername(username, role)
                    } else {
                        Toast.makeText(this, "Username already taken, please choose another", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Username not taken, proceed to save
                    saveUsername(username, role)
                }
            } else {
                Toast.makeText(this, "Failed to check username: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUsername(username: String, role: String) {
        if (userId == null || userEmail == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userMap = mapOf(
            "username" to username,
            "email" to userEmail,
            "role" to role
        )

        val updates = hashMapOf<String, Any>(
            "/users/$userId" to userMap,
            "/usernames/$username" to userEmail!!
        )

        database.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Username and role saved successfully", Toast.LENGTH_SHORT).show()
                // Stay on the same activity for update
            } else {
                Toast.makeText(this, "Failed to save username: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

