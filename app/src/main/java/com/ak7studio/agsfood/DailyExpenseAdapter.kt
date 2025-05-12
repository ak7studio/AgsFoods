package com.ak7studio.agsfood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DailyExpenseAdapter(
    private val items: MutableList<ExpenseItem>,
    private val onEdit: (ExpenseItem, Int) -> Unit,
    private val onDelete: (ExpenseItem, Int) -> Unit
) : RecyclerView.Adapter<DailyExpenseAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvShift: TextView = view.findViewById(R.id.tvShift)
        val tvItem: TextView = view.findViewById(R.id.tvItem)
        val tvQty: TextView = view.findViewById(R.id.tvQty)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(item: ExpenseItem, position: Int) {
            tvShift.text = item.shift ?: ""
            tvItem.text = item.itemName
            tvQty.text = "${item.quantity}"
            tvPrice.text = "₹${item.price}"

            btnEdit.setOnClickListener { onEdit(item, position) }
            btnDelete.setOnClickListener { onDelete(item, position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvShift.text = item.shift ?: ""
        holder.tvItem.text = item.itemName
        holder.tvQty.text = "${item.quantity}"
        holder.tvPrice.text = "₹${item.price}"
        holder.bind(item, position)
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateItem(position: Int, newItem: ExpenseItem) {
        items[position] = newItem
        notifyItemChanged(position)
    }
}