package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class CreateUsernameActivity : BaseActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var radioGroupRole: RadioGroup

    private lateinit var tvChangePassword: TextView
    private lateinit var tvForgotPassword: TextView

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    private var userId: String? = null
    private var userEmail: String? = null

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

        tvChangePassword = findViewById(R.id.tvChangePassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        tvChangePassword.setOnClickListener { showChangePasswordDialog() }
        tvForgotPassword.setOnClickListener { sendPasswordResetEmail() }



        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid
        userEmail = auth.currentUser?.email

        if (userId == null || userEmail == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadCurrentUserInfo()

        usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        radioGroupRole.visibility = if (UserPrefs.getRole() == UserPrefs.KEY_ADMIN) View.VISIBLE else View.INVISIBLE

        radioGroupRole.setOnCheckedChangeListener { _, _ -> checkForChanges() }

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

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = dialogView.findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitChangePassword)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSubmit.setOnClickListener {
            val oldPwd = etOldPassword.text.toString()
            val newPwd = etNewPassword.text.toString()
            val confirmPwd = etConfirmNewPassword.text.toString()

            if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPwd.length < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPwd != confirmPwd) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            val email = user?.email
            if (user != null && email != null) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPwd)
                user.reauthenticate(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.updatePassword(newPwd).addOnCompleteListener { updTask ->
                            if (updTask.isSuccessful) {
                                Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this, "Failed to change password: ${updTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
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

                submitButton.isEnabled = false
            }
        }
    }

    private fun getSelectedRole(): String {
        return when (radioGroupRole.checkedRadioButtonId) {
            R.id.radioCashier -> UserPrefs.KEY_CASHIER
            R.id.radioManager -> UserPrefs.KEY_MANAGER
            R.id.radioAdmin -> UserPrefs.KEY_ADMIN
            else -> UserPrefs.KEY_CASHIER
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
                        saveUsername(username, role)
                    } else {
                        Toast.makeText(this, "Username already taken, please choose another", Toast.LENGTH_SHORT).show()
                    }
                } else {
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
                // Update Firebase Auth profile display name
                updateFirebaseUserProfile(username)

                Toast.makeText(this, "Username and role saved successfully", Toast.LENGTH_SHORT).show()
                originalUsername = username
                originalRole = role
                submitButton.isEnabled = false
                SetUserNameToPref()
                navigateToDashboard()
            } else {
                Toast.makeText(this, "Failed to save username: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateFirebaseUserProfile(newDisplayName: String) {
        val user = auth.currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                .build()
            user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Failed to update profile name in auth", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendPasswordResetEmail() {
        if (userEmail == null) {
            Toast.makeText(this, "User email not available", Toast.LENGTH_SHORT).show()
            return
        }
        auth.sendPasswordResetEmail(userEmail!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $userEmail", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun SetUserNameToPref() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java)
                val role = snapshot.child("role").getValue(String::class.java)
                UserPrefs.setUsername(username.toString())
                UserPrefs.setRole(role.toString())
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
