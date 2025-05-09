package com.ak7studio.agsfood

import android.content.Context
import org.json.JSONArray
import java.io.File

class ItemNameStorageHelper(private val context: Context) {

    private val fileName = "items.json"

    private fun getItemsFile(): File = File(context.filesDir, fileName)

    fun loadItemNames(): MutableList<String> {
        val file = getItemsFile()
        val items = mutableListOf<String>()
        if (file.exists()) {
            val json = JSONArray(file.readText())
            for (i in 0 until json.length()) items.add(json.getString(i))
        } else {
            items.addAll(listOf("Leaf", "Milk", "Watercan")) // default
            saveItemNames(items)
        }
        return items
    }

    fun saveItemNames(items: List<String>) {
        val file = getItemsFile()
        val json = JSONArray()
        items.forEach { json.put(it) }
        file.writeText(json.toString())
    }
}
