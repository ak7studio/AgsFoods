package com.ak7studio.agsfood

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var phoneNumberEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val registerTextView = findViewById<TextView>(R.id.textViewRegister)
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        // Initialize Firebase Authentication
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber)
        sendOtpButton = findViewById(R.id.buttonSendOtp)

        sendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                PhoneAuthHelper.startPhoneNumberVerification(
                    activity = this,
                    auth = auth,
                    phoneNumber = phoneNumber,
                    isRegistration = false // This is login
                )
            } else {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber") // Replace +91 with your country code if needed
            .setTimeout(60L, TimeUnit.SECONDS) // Set a timeout for the verification process
            .setActivity(this) // For activity result callbacks
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This callback will be invoked in instances where the phone number can be instantly
                    // verified and an auto-generated Firebase credential can be retrieved.
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(firebaseException: FirebaseException) {
                    // This callback is invoked when the verification attempts fail.
                    Toast.makeText(this@LoginActivity, "Verification failed: ${firebaseException.message}", Toast.LENGTH_LONG).show()
                    // Log the error for debugging
                    android.util.Log.e("Firebase Auth", "Phone number verification failed", firebaseException)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    // The SMS verification code has been sent to the user's phone number.
                    this@LoginActivity.verificationId = verificationId
                    // Navigate to the OTP verification screen
                    navigateToOtpVerificationScreen(phoneNumber, verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun navigateToOtpVerificationScreen(phoneNumber: String, verificationId: String) {
        // We'll create this activity next
        val intent = Intent(this, VerifyOtpActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        intent.putExtra("verificationId", verificationId)
        startActivity(intent)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = task.result?.user
                    Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                    // Navigate to the shift selection screen
//                    navigateToShiftSelectionScreen(user?.phoneNumber)
                    //navigate to dashboard
                    navigateToDashboardScreen()
                } else {
                    // Sign in failed, display a message to the user
                    Toast.makeText(this@LoginActivity, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("Firebase Auth", "Phone sign-in failed", task.exception)
                }
            }
    }

    private fun navigateToShiftSelectionScreen(phoneNumber: String?) {
        val intent = Intent(this, ShiftSelectionActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        startActivity(intent)
        finish() // Prevent going back to the login screen
    }
    private fun navigateToDashboardScreen() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}