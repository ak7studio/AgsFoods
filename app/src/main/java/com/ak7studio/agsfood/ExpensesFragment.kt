package com.ak7studio.agsfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ExpensesFragment(private val dayEntries: List<DayEntry>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sales_expenses, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val totalText = view.findViewById<TextView>(R.id.textTotal)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = SalesExpensesAdapter(dayEntries, isSales = false)

        // Calculate totals
        var totalMorning = 0.0
        var totalEvening = 0.0
        for (entry in dayEntries) {
            totalMorning += entry.morning?.expenses ?: 0.0
            totalEvening += entry.evening?.expenses ?: 0.0
        }
        totalText.text = "Total Morning: $totalMorning | Total Evening: $totalEvening"
        return view
    }
}
