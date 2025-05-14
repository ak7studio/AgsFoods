package com.ak7studio.agsfood

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged

class CreateUsernameActivity : BaseActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var radioGroupRole: RadioGroup

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    private var userId: String? = null
    private var userEmail: String? = null

    // Track original username and role
    private var originalUsername: String? = null
    private var originalRole: String? = null

    override fun getCurrentNavItemId(): Int = R.id.nav_updateProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate layout into content frame
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

        // Load current username and role, disable submit initially
        loadCurrentUserInfo()

        // Add listeners to detect changes and enable/disable submit button
        usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        radioGroupRole.visibility = if(UserPrefs.getRole() == UserPrefs.KEY_ADMIN) View.VISIBLE else View.INVISIBLE

        radioGroupRole.setOnCheckedChangeListener { _, _ ->
            checkForChanges()
        }

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
                originalUsername = snapshot.child("username").getValue(String::class.java)
                originalRole = snapshot.child("role").getValue(String::class.java)

                usernameEditText.setText(originalUsername)

                when (originalRole) {
                    "cashier" -> radioGroupRole.check(R.id.radioCashier)
                    "manager" -> radioGroupRole.check(R.id.radioManager)
                    "admin" -> radioGroupRole.check(R.id.radioAdmin)
                }

                submitButton.isEnabled = false // disable initially
            }
        }
    }

    private fun getSelectedRole(): String {
        return when (radioGroupRole.checkedRadioButtonId) {
            R.id.radioCashier -> UserPrefs.KEY_CASHIER
            R.id.radioManager -> UserPrefs.KEY_MANAGER
            R.id.radioAdmin -> UserPrefs.KEY_ADMIN
            else -> UserPrefs.KEY_CASHIER // default
        }
    }

    private fun checkForChanges() {
        val currentUsername = usernameEditText.text.toString().trim()
        val currentRole = getSelectedRole()

        submitButton.isEnabled = (currentUsername != originalUsername) || (currentRole != originalRole)
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
                // Update original values and disable submit button again
                originalUsername = username
                originalRole = role
                submitButton.isEnabled = false
                SetUserNameToPref()
            } else {
                Toast.makeText(this, "Failed to save username: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun SetUserNameToPref() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java)
                val role = snapshot.child("role").getValue(String::class.java)
                UserPrefs.setUsername(username.toString())
                UserPrefs.setRole(role.toString())
            }
        }
    }
}
