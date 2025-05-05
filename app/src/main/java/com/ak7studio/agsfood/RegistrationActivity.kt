package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegistrationActivity : AppCompatActivity() {

    private lateinit var phoneNumberEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber)
        registerButton = findViewById(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                PhoneAuthHelper.checkWhitelistAndStartVerification(
                    activity = this,
                    phoneNumber = phoneNumber,
                    isRegistration = true // This is registration
                )
            } else {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
