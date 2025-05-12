package com.ak7studio.agsfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SalesFragment(private val dayEntries: List<DayEntry>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sales_expenses, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val totalText = view.findViewById<TextView>(R.id.textTotal)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = SalesExpensesAdapter(dayEntries, isSales = true)

        // Calculate totals
        var totalMorning = 0.0
        var totalEvening = 0.0
        for (entry in dayEntries) {
            totalMorning += entry.morning?.sales ?: 0.0
            totalEvening += entry.evening?.sales ?: 0.0
        }
        // Format numbers with commas and no decimals
        val formattedMorning = String.format("%,d", totalMorning.toInt())
        val formattedEvening = String.format("%,d", totalEvening.toInt())
        val formattedOverall = String.format("%,d", (totalMorning + totalEvening).toInt())

        totalText.text = "Morning Total: ₹$formattedMorning\n" +
                "Evening Total: ₹$formattedEvening\n" +
                "Overall Total: ₹$formattedOverall"

        return view
    }
}
