package com.ak7studio.agsfood

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.IOException

data class SaleForecastEntry(
    val rowId: Int = 0,
    val date: String = "",       // format: dd-MM-yy
    val shift: String = "",      // "Morning" or "Evening"
    val sales: Int = 0,
    val total: Int = 0
    // Add more fields if needed
)

object SaleForecastSheetsHelper {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbyNw2sOAwQawo20kDHuXMtm4ol4jyVgdE_XXxukplca77CF7uMM-mhrSF6M3j2pLnZ2rw/exec" // Replace with your script URL
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())

    // Fetch all sale forecast entries
    fun fetchSaleForecastEntries(callback: (List<SaleForecastEntry>) -> Unit) {
        val url = "$BASE_URL?sheet=SaleForecast"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaleForecastSheetsHelper", "Fetch failed", e)
                handler.post { callback(emptyList()) }
            }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "[]"
                val entries = mutableListOf<SaleForecastEntry>()
                try {
                    val arr = JSONArray(json)
                    for (i in 1 until arr.length()) {  // Assuming first row is header
                        val row = arr.getJSONArray(i)
                        entries.add(
                            SaleForecastEntry(
                                rowId = i + 1,
                                date = row.optString(0),
                                shift = row.optString(1),
                                sales = row.optInt(2),
                                total = row.optInt(3)
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SaleForecastSheetsHelper", "Failed to parse entries", e)
                }
                handler.post { callback(entries) }
            }
        })
    }

    // Add a new sale forecast entry
    fun addSaleForecastEntry(entry: SaleForecastEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?sheet=SaleForecast&action=add"
        val json = Gson().toJson(listOf(entry.date, entry.shift, entry.sales, entry.total))
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaleForecastSheetsHelper", "Add failed", e)
                handler.post { callback(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                handler.post { callback(response.isSuccessful) }
            }
        })
    }

    // Update an existing sale forecast entry by rowId
    fun updateSaleForecastEntry(entry: SaleForecastEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?sheet=SaleForecast&action=update"
        val json = Gson().toJson(
            mapOf(
                "rowId" to entry.rowId,
                "date" to entry.date,
                "shift" to entry.shift,
                "sales" to entry.sales,
                "total" to entry.total
            )
        )
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaleForecastSheetsHelper", "Update failed", e)
                handler.post { callback(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                handler.post { callback(response.isSuccessful) }
            }
        })
    }

    // Delete a sale forecast entry by rowId
    fun deleteSaleForecastEntry(entry: SaleForecastEntry, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL?sheet=SaleForecast&action=delete"
        val json = Gson().toJson(mapOf("rowId" to entry.rowId))
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaleForecastSheetsHelper", "Delete failed", e)
                handler.post { callback(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                handler.post { callback(response.isSuccessful) }
            }
        })
    }

    fun saveForecastDetails(
        date: String,
        shift: String,
        details: List<ForecastItem>,
        callback: (Boolean) -> Unit
    ) {
        val url = "$BASE_URL?sheet=SaleForecastDetails&action=saveDetails"
        // Prepare JSON array of arrays: each row = [date, shift, item, qty, unit, unitPrice, totalPrice]
        val rows = details.map {
            listOf(date, shift, it.item, it.qty, it.unit, it.unitPrice, it.totalPrice)
        }
        val json = Gson().toJson(rows)
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaleForecastSheetsHelper", "Save details failed", e)
                handler.post { callback(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                handler.post { callback(response.isSuccessful) }
            }
        })
    }

    fun fetchConfig(callback: (items: List<String>, units: List<String>) -> Unit) {
        val url = "$BASE_URL?sheet=Config"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaleForecastSheetsHelper", "Fetch config failed", e)
                handler.post { callback(emptyList(), emptyList()) }
            }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "[]"
                val items = mutableListOf<String>()
                val units = mutableListOf<String>()
                try {
                    val arr = JSONArray(json)
                    // Skip header row (index 0)
                    for (i in 1 until arr.length()) {
                        val row = arr.getJSONArray(i)
                        val item = row.optString(0).trim()
                        val unit = row.optString(1).trim()
                        if (item.isNotEmpty()) items.add(item)
                        if (unit.isNotEmpty()) units.add(unit)
                    }
                } catch (e: Exception) {
                    Log.e("SaleForecastSheetsHelper", "Failed to parse config", e)
                }
                handler.post { callback(items, units) }
            }
        })
    }

}
