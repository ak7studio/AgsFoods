package com.ak7studio.agsfood

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object GrocerySheetsHelper {
    // Replace with your actual Google Apps Script endpoint
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbw2gEjxy045wPwnyDxoHV1NTg0CpvuiJAHC2t_JbHJXKeXOTBUgniSqCmDezbhxYIFzAw/exec"
    private val client = OkHttpClient()

    // Fetch all grocery entries
    fun fetchGroceryEntries(callback: (List<GroceryEntry>) -> Unit) {
        val url = "$BASE_URL?action=fetch"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }
            override fun onResponse(call: Call, response: Response) {
                val entries = mutableListOf<GroceryEntry>()
                response.body?.string()?.let { body ->
                    val json = JSONArray(body)
                    for (i in 0 until json.length()) {
                        val obj = json.getJSONObject(i)
                        entries.add(
                            GroceryEntry(
                                date = obj.optString("date"),
                                amount = obj.optDouble("amount", 0.0),
                                addAmount = obj.optDouble("addAmount", 0.0),
                                cash = obj.optDouble("cash", 0.0),
                                balance = obj.optDouble("balance", 0.0),
                                upiPayment = obj.optDouble("upiPayment", 0.0),
                                comments = obj.optString("comments")
                            )
                        )
                    }
                }
                callback(entries)
            }
        })
    }

    // Add a new grocery entry
    fun addGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?action=add"
        val json = JSONObject().apply {
            put("date", entry.date)
            put("amount", entry.amount)
            put("addAmount", entry.addAmount)
            put("cash", entry.cash)
            put("balance", entry.balance)
            put("upiPayment", entry.upiPayment)
            put("comments", entry.comments)
        }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    // Update an existing grocery entry (by date or unique id)
    fun updateGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?action=update"
        val json = JSONObject().apply {
            put("date", entry.date)
            put("amount", entry.amount)
            put("addAmount", entry.addAmount)
            put("cash", entry.cash)
            put("balance", entry.balance)
            put("upiPayment", entry.upiPayment)
            put("comments", entry.comments)
        }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).put(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    // Delete a grocery entry (by date or unique id)
    fun deleteGroceryEntry(entry: GroceryEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?action=delete"
        val json = JSONObject().apply {
            put("date", entry.date)
        }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).delete(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

}
