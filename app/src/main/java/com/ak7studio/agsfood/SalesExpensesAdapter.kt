package com.ak7studio.agsfood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SalesExpensesAdapter(
    private val dayEntries: List<DayEntry>,
    private val isSales: Boolean
) : RecyclerView.Adapter<SalesExpensesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.textDate)
        val morning: TextView = view.findViewById(R.id.textMorning)
        val evening: TextView = view.findViewById(R.id.textEvening)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sales_expenses, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = dayEntries.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = dayEntries[position]
        holder.date.text = entry.date
        if (isSales) {
            holder.morning.text = (entry.morning?.sales ?: 0.0).toString()
            holder.evening.text = (entry.evening?.sales ?: 0.0).toString()
        } else {
            holder.morning.text = (entry.morning?.expenses ?: 0.0).toString()
            holder.evening.text = (entry.evening?.expenses ?: 0.0).toString()
        }
    }
}
