/*package com.ak7studio.agsfood

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

data class GroceryEntryOld(
    val rowId: Int = 0,
    val date: String = "",       // always in dd-MM-yy format
    val amount: Double = 0.0,
    val addAmount: Double = 0.0,
    val cash: Double = 0.0,
    val balance: Double = 0.0,
    val upiPayment: Double = 0.0,
    val comments: String = ""
)

object GrocerySheetsHelper {
    //AGSFoods-GroceryScriptV2
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbwr-ODC6ISDaYE_Esb91MjfGWHKkF1ySqlpZA1BHlC_Jy2ixfUjdKbDlhKhQhn_4vE/exec"
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())


    // Fetch all grocery entries
    fun fetchGroceryEntries(callback: (List<GroceryEntry>) -> Unit) {
        val url = "$BASE_URL?sheet=Grocery"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { handler.post { callback(emptyList()) } }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "[]"
                val entries = mutableListOf<GroceryEntry>()
                try {
                    val arr = JSONArray(json)
                    for (i in 1 until arr.length()) {  // Assuming first row is header, skip it
                        val row = arr.getJSONArray(i)
                        val rawDate = row.optString(0)
                        val formattedDate = parseDate(rawDate)
//                        val formattedDate = rawDate
                        entries.add(
                            GroceryEntryOld(
                                rowId = i + 1,
                                date = formattedDate,
                                amount = row.optDouble(1),
                                addAmount = row.optDouble(2),
                                cash = row.optDouble(3),
                                balance = row.optDouble(4),
                                upiPayment = row.optDouble(5),
                                comments = row.optString(6)
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("GrocerySheetsHelper", "Failed to parse grocery entries", e)
                }
                handler.post { callback(entries) }
            }
        })
    }

    private fun parseDate(rawDate: String): String {
        val outputFormat = SimpleDateFormat("MM-dd-yy", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()

        return when {
            isIsoFormat(rawDate) -> {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                try {
                    val date = isoFormat.parse(rawDate)
                    if (date != null) outputFormat.format(date) else rawDate
                } catch (e: ParseException) {
                    rawDate
                }
            }
            isDdMmYyFormat(rawDate) -> {
                // Already in desired format, but parse and reformat to be safe
                val simpleFormat = SimpleDateFormat("MM-dd-yy", Locale.getDefault())
                simpleFormat.timeZone = TimeZone.getDefault()
                try {
                    val date = simpleFormat.parse(rawDate)
                    if (date != null) outputFormat.format(date) else rawDate
                } catch (e: ParseException) {
                    rawDate
                }
            }
            else -> {
                Log.w("GrocerySheetsHelper", "Unrecognized date format: $rawDate")
                rawDate
            }
        }
    }

    fun isIsoFormat(dateStr: String): Boolean {
        val isoRegex = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z$""")
        return isoRegex.matches(dateStr)
    }

    fun isDdMmYyFormat(dateStr: String): Boolean {
        val ddMmYyRegex = Regex("""^\d{2}-\d{2}-\d{2}$""")
        return ddMmYyRegex.matches(dateStr)
    }

    // Add a new grocery entry (date formatted to dd-MM-yy)
    fun addGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?sheet=Grocery&action=add"
        val json = Gson().toJson(listOf(entry.date, entry.amount, entry.addAmount, entry.cash, entry.balance, entry.upiPayment, entry.comments))
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) { callback(response.isSuccessful) }
        })
    }

    // Update an existing grocery entry by rowId (date formatted to dd-MM-yy)
    fun updateGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?sheet=Grocery&action=update"
        val json = Gson().toJson(
            mapOf(
                "rowId" to entry.rowId,
                "date" to entry.date,
                "amount" to entry.amount,
                "addAmount" to entry.addAmount,
                "cash" to entry.cash,
                "balance" to entry.balance,
                "upiPayment" to entry.upiPayment,
                "comments" to entry.comments
            )
        )
        val jsonString = json.toString()
        Log.d("updateGroceryEntry", "Request URL: $url")
        Log.d("updateGroceryEntry", "Request JSON: $jsonString")

        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("updateGroceryEntry", "Request failed", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("updateGroceryEntry", "Response code: ${response.code}")
                val responseBody = response.body?.string()
                Log.d("updateGroceryEntry", "Response body: $responseBody")
                callback(response.isSuccessful)
            }
        })
    }

    // Delete a grocery entry by rowId
    fun deleteGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?sheet=Grocery&action=delete"
        val json = Gson().toJson(mapOf("rowId" to entry.rowId))
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build() // Use POST for delete if backend expects it
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) { callback(response.isSuccessful) }
        })
    }
}
*/