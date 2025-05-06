package com.ak7studio.agsfood

import android.os.Handler
import android.os.Looper
import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.IOException

data class ExpenseItem(
    val rowId: Int = 0,
    val date: String = "",
    val shift: String = "",
    val itemName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)
data class SalesItem(
    val date: String = "",
    val shift: String = "",
    val category: String = "",
    val itemName: String = "",
    val count: Int = 0,
    val price: Double = 0.0,
    val totalCount: Int = 0,
    val totalAmount: Double = 0.0
)

object GoogleSheetsHelper {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private const val scriptUrl = "https://script.google.com/macros/s/AKfycbziuvBqycIqiLB3WrgTrHZmVRJ0Y_KKvEJQx07jAI6Y1x07PXJb6UMxgiqNu99P79Xfvg/exec" // Replace with your deployed Apps Script URL

    fun fetchExpenses(onResult: (List<ExpenseItem>) -> Unit) {
        val url = "$scriptUrl?sheet=Expenses"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { handler.post { onResult(emptyList()) } }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "[]"
                val items = mutableListOf<ExpenseItem>()
                val arr = JSONArray(json)
                for (i in 1 until arr.length()) { // skip header
                    val row = arr.getJSONArray(i)
                    items.add(
                        ExpenseItem(
                            rowId = i + 1,
                            date = row.optString(0),
                            shift = row.optString(1),
                            itemName = row.optString(2),
                            quantity = row.optInt(3),
                            price = row.optDouble(4)
                        )
                    )
                }
                handler.post { onResult(items) }
            }
        })
    }

    fun fetchSales(onResult: (List<SalesItem>) -> Unit) {
        val url = "$scriptUrl?sheet=Sales"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { handler.post { onResult(emptyList()) } }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "[]"
                val items = mutableListOf<SalesItem>()
                val arr = JSONArray(json)
                for (i in 1 until arr.length()) {
                    val row = arr.getJSONArray(i)
                    items.add(
                        SalesItem(
                            date = row.optString(0),
                            shift = row.optString(1),
                            category = row.optString(2),
                            itemName = row.optString(3),
                            count = row.optInt(4),
                            price = row.optDouble(5),
                            totalCount = row.optInt(6),
                            totalAmount = row.optDouble(7)
                        )
                    )
                }
                handler.post { onResult(items) }
            }
        })
    }

    fun addExpense(item: ExpenseItem, onResult: (Boolean) -> Unit) {
        val url = "$scriptUrl?sheet=Expenses&action=add"
        val json = Gson().toJson(listOf(item.date, item.shift, item.itemName, item.quantity, item.price))
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { handler.post { onResult(false) } }
            override fun onResponse(call: Call, response: Response) { handler.post { onResult(response.isSuccessful) } }
        })
    }

    fun addSales(item: SalesItem, onResult: (Boolean) -> Unit) {
        val url = "$scriptUrl?sheet=Sales&action=add"
        val json = Gson().toJson(listOf(item.date, item.shift, item.category, item.itemName, item.count, item.price, item.totalCount, item.totalAmount))
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { handler.post { onResult(false) } }
            override fun onResponse(call: Call, response: Response) { handler.post { onResult(response.isSuccessful) } }
        })
    }

    fun updateExpense(item: ExpenseItem, onResult: (Boolean) -> Unit) {
        val url = "$scriptUrl?sheet=Expenses&action=update"
        val json = Gson().toJson(
            mapOf(
                "rowId" to item.rowId,
                "date" to item.date,
                "shift" to item.shift,
                "itemName" to item.itemName,
                "quantity" to item.quantity,
                "price" to item.price
            )
        )
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { onResult(false) }
            override fun onResponse(call: Call, response: Response) { onResult(response.isSuccessful) }
        })
    }

    fun deleteExpense(item: ExpenseItem, onResult: (Boolean) -> Unit) {
        val url = "$scriptUrl?sheet=Expenses&action=delete"
        val json = Gson().toJson(mapOf("rowId" to item.rowId))
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { onResult(false) }
            override fun onResponse(call: Call, response: Response) { onResult(response.isSuccessful) }
        })
    }

}

