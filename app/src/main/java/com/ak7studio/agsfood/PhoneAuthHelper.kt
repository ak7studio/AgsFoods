package com.ak7studio.agsfood

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

object PhoneAuthHelper {
    fun startPhoneNumberVerification(
        activity: Activity,
        auth: FirebaseAuth,
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
