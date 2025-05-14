package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var loginOrSignupButton: Button
    private lateinit var loginTab: Button
    private lateinit var signupTab: Button

    private var isLoginMode = true
    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Find views
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber)
        loginOrSignupButton = findViewById(R.id.buttonLoginOrSignup)
        loginTab = findViewById(R.id.btnLoginTab)
        signupTab = findViewById(R.id.btnSignupTab)
        loginTab.isSelected = true
        signupTab.isSelected = false

        //Init User Prefs
        UserPrefs.init(applicationContext)

        // Tab switching logic
        loginTab.setOnClickListener {
            isLoginMode = true
            loginTab.setBackgroundResource(R.drawable.bg_tab_left_selected)
            signupTab.setBackgroundResource(R.drawable.bg_tab_right_unselected)
            loginOrSignupButton.text = "Log In"
            confirmPasswordEditText.visibility = View.GONE
            loginTab.isSelected = true
            signupTab.isSelected = false
        }
        signupTab.setOnClickListener {
            isLoginMode = false
            signupTab.setBackgroundResource(R.drawable.bg_tab_right_selected)
            loginTab.setBackgroundResource(R.drawable.bg_tab_left_unselected)
            loginOrSignupButton.text = "Sign Up"
            confirmPasswordEditText.visibility = View.VISIBLE
            loginTab.isSelected = false
            signupTab.isSelected = true
        }

        loginOrSignupButton.setOnClickListener {
            val userName = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (isLoginMode) {
                if (userName.isNotEmpty() && password.isNotEmpty()) {
                    loginWithUsername(userName, password)
                } else {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (userName.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    if (password == confirmPassword) {
                        signUpWithEmail(userName, password)
                    } else {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login success
                    navigateToDashboardScreen()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginWithUsername(username: String, password: String) {
        database.child("usernames").child(username).get().addOnSuccessListener { snapshot ->
            val email = snapshot.getValue(String::class.java)
            if (email != null) {
                val normalizedEmailKey = email.replace(".", ",")
                database.child("whitelist_emails").child(normalizedEmailKey).get().addOnSuccessListener { wlSnapshot ->
                    if (wlSnapshot.exists()) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    SetUserNameToPref()
                                    navigateToDashboardScreen()
                                } else {
                                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "User not authorized to log in", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to verify whitelist: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch username", Toast.LENGTH_SHORT).show()
        }
    }


    private fun signUpWithEmail(email: String, password: String) {
        val normalizedEmailKey = email.replace(".", ",")
        database.child("whitelist_emails").child(normalizedEmailKey).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Email is whitelisted, proceed with sign up
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            navigateToCreateUserNamecreen()
                        } else {
                            Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Email not authorized to sign up", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to verify whitelist: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun navigateToDashboardScreen() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToCreateUserNamecreen() {
        val firebaseUser = auth.currentUser
        firebaseUser?.let { user ->
            val intent = Intent(this, CreateUsernameActivity::class.java)
            intent.putExtra("USER_ID", user.uid)  // Pass the UID here
            startActivity(intent)
            finish()
        }
    }


    private fun SetUserNameToPref() {
        var userId = auth.currentUser?.uid
        database.child("users").child(userId!!).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java)
                val role = snapshot.child("role").getValue(String::class.java)
                UserPrefs.setUsername(username.toString())
                UserPrefs.setRole(role.toString())
            }
        }
    }
}
