package com.ak7studio.agsfood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DayEntryAdapter(
    private var dayList: List<DayEntry>
) : RecyclerView.Adapter<DayEntryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvMorningSales: TextView = itemView.findViewById(R.id.tvMorningSales)
        val tvMorningExpenses: TextView = itemView.findViewById(R.id.tvMorningExpenses)
        val tvEveningSales: TextView = itemView.findViewById(R.id.tvEveningSales)
        val tvEveningExpenses: TextView = itemView.findViewById(R.id.tvEveningExpenses)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kiosk_data, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = dayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = dayList[position]
        holder.tvDate.text = day.date

        // Morning
        holder.tvMorningSales.text = "Sales: ${day.morning?.sales ?: "-"}"
        holder.tvMorningExpenses.text = "Expenses: ${day.morning?.expenses ?: "-"}"

        // Evening
        holder.tvEveningSales.text = "Sales: ${day.evening?.sales ?: "-"}"
        holder.tvEveningExpenses.text = "Expenses: ${day.evening?.expenses ?: "-"}"
    }

    fun submitList(newList: List<DayEntry>) {
        dayList = newList
        notifyDataSetChanged()
    }
}
