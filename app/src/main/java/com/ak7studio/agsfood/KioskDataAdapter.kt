/*
package com.ak7studio.agsfood

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KioskDataAdapter(
    private val dataList: List<KioskData>,
    private val userRole: String,
    private val onEditClick: (KioskData) -> Unit
) : RecyclerView.Adapter<KioskDataAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvShift: TextView = itemView.findViewById(R.id.tvShift)
        val tvSales: TextView = itemView.findViewById(R.id.tvSales)
        val tvExpenses: TextView = itemView.findViewById(R.id.tvExpenses)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kiosk_data, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]

        holder.tvTimestamp.text = data.timestamp
        holder.tvDate.text = data.date
        holder.tvShift.text = data.shift
        holder.tvSales.text = "Sales: ${data.sales}"
        holder.tvExpenses.text = "Expenses: ${data.expenses}"

        // Show edit button only for managers
        if (userRole == "manager") {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener {
                onEditClick(data)
            }
        } else {
            holder.btnEdit.visibility = View.GONE
        }

        holder.btnEdit.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditDataActivity::class.java).apply {
                putExtra("dataId", data.id)
                putExtra("sales", data.sales ?: 0.0)
                putExtra("expenses", data.expenses ?: 0.0)
                putExtra("shift", data.shift)
                putExtra("userRole", userRole)
            }
            context.startActivity(intent)
        }
    }

}
*/
