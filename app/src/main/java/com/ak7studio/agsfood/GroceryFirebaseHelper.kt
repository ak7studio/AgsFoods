package com.ak7studio.agsfood

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// The data class for a grocery entry.
// The 'id' field is now the date in "yyyy-MM-dd" format.
data class GroceryEntry(
    val id: String = "",
    val date: String = "", // Redundant, but kept for consistency within the object
    val amount: Double = 0.0,
    val addAmount: Double = 0.0,
    val cash: Double = 0.0,
    val balance: Double = 0.0,
    val upiPayment: Double = 0.0,
    val comments: String = ""
)

object GroceryFirebaseHelper {
    private const val SUPPLIES_PATH = "supplies"
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // Helper function to get the Firebase database path for a specific entry.
    private fun getGroceryPath(entryDate: String): DatabaseReference {
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(entryDate)
        val monthYear = monthFormat.format(date!!)
        return database.child(SUPPLIES_PATH)
            .child(monthYear)
            .child(entryDate)
            .child("grocery")
    }

    // Fetches all grocery entries for the current month and sets up a real-time listener.
    fun fetchGroceryEntries(callback: (List<GroceryEntry>) -> Unit) {
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthYear = monthFormat.format(Calendar.getInstance().time)

        database.child(SUPPLIES_PATH).child(currentMonthYear).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<GroceryEntry>()
                // The data is now a map of dates to grocery entries.
                for (dateSnapshot in snapshot.children) {
                    val entrySnapshot = dateSnapshot.child("grocery")
                    val entry = entrySnapshot.getValue(GroceryEntry::class.java)
                    if (entry != null) {
                        // The id is now the date key from the parent snapshot.
                        entries.add(entry.copy(id = dateSnapshot.key!!))
                    }
                }
                // Sort entries by date in descending order to show the latest first
                entries.sortByDescending { it.date }
                callback(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroceryFirebaseHelper", "Failed to load grocery entries: ${error.message}")
                callback(emptyList())
            }
        })
    }

    // Adds a new grocery entry to the database using the date as the key.
    fun addGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val entryRef = getGroceryPath(entry.date)
        entryRef.setValue(entry)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                Log.e("GroceryFirebaseHelper", "Failed to add new entry: ${it.message}")
                callback(false)
            }
    }

    // Updates an existing grocery entry in the database.
    fun updateGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        if (entry.id.isNotEmpty()) {
            val entryRef = getGroceryPath(entry.id)
            entryRef.setValue(entry)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener {
                    Log.e("GroceryFirebaseHelper", "Failed to update entry with ID ${entry.id}: ${it.message}")
                    callback(false)
                }
        } else {
            Log.e("GroceryFirebaseHelper", "Attempted to update entry with a blank ID.")
            callback(false)
        }
    }

    // Deletes a grocery entry from the database using its 'id' (date).
    fun deleteGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        if (entry.id.isNotEmpty()) {
            val entryRef = getGroceryPath(entry.id)
            entryRef.removeValue()
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener {
                    Log.e("GroceryFirebaseHelper", "Failed to delete entry with ID ${entry.id}: ${it.message}")
                    callback(false)
                }
        } else {
            Log.e("GroceryFirebaseHelper", "Attempted to delete entry with a blank ID.")
            callback(false)
        }
    }
}