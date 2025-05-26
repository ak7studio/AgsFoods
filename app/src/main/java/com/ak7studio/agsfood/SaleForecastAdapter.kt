package com.ak7studio.agsfood

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SaleForecastAdapter(
    private val items: List<SaleForecastEntry>,
    private val onItemClick: (SaleForecastEntry) -> Unit
) : RecyclerView.Adapter<SaleForecastAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvShift: TextView = view.findViewById(R.id.tvShift)
        val tvSales: TextView = view.findViewById(R.id.tvSales)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)

        init {
            view.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sale_forecast_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = items[position]
        Log.d("SaleForecastAdapter", "Binding: $entry")
        holder.tvDate.text = entry.date
        holder.tvShift.text = entry.shift
        holder.tvSales.text = entry.sales.toString()
        holder.tvTotal.text = entry.total.toString()
    }

    override fun getItemCount(): Int = items.size
}
