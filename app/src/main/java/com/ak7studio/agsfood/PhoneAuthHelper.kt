package com.ak7studio.agsfood

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

object PhoneAuthHelper {

    private val auth = FirebaseAuth.getInstance();
    private val whitelistRef = FirebaseDatabase.getInstance("https://agsfoods-d6f62.firebaseio.com/").getReference("whitelist")

     fun checkWhitelistAndStartVerification(
        activity: Activity,
        phoneNumber: String,
        isRegistration: Boolean = false
    ) {
         // Initialize Firebase Authentication
         FirebaseApp.initializeApp(activity)
        // phoneNumber should be in the same format as in your whitelist, e.g. "9876543210"
        whitelistRef.child(phoneNumber).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Number is whitelisted, proceed with OTP
                startPhoneNumberVerification(activity,phoneNumber, isRegistration)
            } else {
                // Not whitelisted
                Toast.makeText(activity, "Your number is not allowed to use this app.", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { error ->
            android.util.Log.e("PhoneAuthHelper", "Whitelist check failed", error)
            Toast.makeText(activity, "Failed to check whitelist. Try again.", Toast.LENGTH_LONG).show()
        }
    }
    private fun startPhoneNumberVerification(
        activity: Activity,
        phoneNumber: String,
        isRegistration: Boolean = false
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber") // Change country code if needed
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Optional: handle instant verification if needed
                }

                override fun onVerificationFailed(e: FirebaseException) {

                    Toast.makeText(activity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    // Navigate to OTP verification screen
                    val intent = Intent(activity, VerifyOtpActivity::class.java)
                    intent.putExtra("phoneNumber", phoneNumber)
                    intent.putExtra("verificationId", verificationId)
                    intent.putExtra("isRegistration", isRegistration)
                    activity.startActivity(intent)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
