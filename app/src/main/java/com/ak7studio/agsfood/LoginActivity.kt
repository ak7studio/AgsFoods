package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var phoneNumberEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val registerTextView = findViewById<TextView>(R.id.textViewRegister)
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        // Initialize UI elements
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber)
        sendOtpButton = findViewById(R.id.buttonSendOtp)

        sendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                PhoneAuthHelper.checkWhitelistAndStartVerification(
                    activity = this,
                    phoneNumber = phoneNumber,
                    isRegistration = false // This is login
                )
            } else {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }
}