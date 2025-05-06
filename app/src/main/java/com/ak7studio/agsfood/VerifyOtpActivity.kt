package com.ak7studio.agsfood

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var otpEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendButton: Button
    private lateinit var auth: FirebaseAuth

    private var verificationId: String? = null
    private var phoneNumber: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isRegistration: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        otpEditText = findViewById(R.id.editTextOtp)
        verifyButton = findViewById(R.id.buttonVerifyOtp)
        resendButton = findViewById(R.id.buttonResendOtp)

        auth = FirebaseAuth.getInstance()

        // Get intent extras
        verificationId = intent.getStringExtra("verificationId")
        phoneNumber = intent.getStringExtra("phoneNumber")
        isRegistration = intent.getBooleanExtra("isRegistration", false)
        resendToken = intent.getParcelableExtra("resendToken")

        verifyButton.setOnClickListener {
            val code = otpEditText.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verificationId?.let { vid ->
                verifyCode(vid, code)
            } ?: Toast.makeText(this, "Verification ID missing", Toast.LENGTH_SHORT).show()
        }

        resendButton.setOnClickListener {
            if (phoneNumber != null && resendToken != null) {
                resendVerificationCode(phoneNumber!!, resendToken!!)
            } else {
                Toast.makeText(this, "Cannot resend OTP now. Try again later.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        if (isRegistration) {
                            createUserProfileIfNotExists(phoneNumber.toString(), user.uid)
                        } else {
//                            navigateToShiftSelection(user.phoneNumber)
                            navigateToDashboardScreen()
                        }
                    } else {
                        Toast.makeText(this, "User not found after sign-in", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Verification failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun createUserProfileIfNotExists(phoneNumber: String, name: String = "User", role: String = "cashier") {
        val usersRef = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/")
            .getReference("users")
        val userRef = usersRef.child(phoneNumber)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Only create if not exists
                val userProfile = mapOf(
                    "name" to name,
                    "role" to role
                )
                userRef.setValue(userProfile)
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                prefs.edit().putString("phoneNumber", phoneNumber).apply()
                prefs.edit().putString("name", name).apply()
                prefs.edit().putString("role", role).apply()
            }
        }
    }

    private fun resendVerificationCode(phone: String, token: PhoneAuthProvider.ForceResendingToken) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone") // Adjust country code if needed
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@VerifyOtpActivity, "Resend failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Toast.makeText(this@VerifyOtpActivity, "OTP resent", Toast.LENGTH_SHORT).show()
                    this@VerifyOtpActivity.verificationId = verificationId
                    this@VerifyOtpActivity.resendToken = token
                }
            })
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun navigateToDashboardScreen() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
