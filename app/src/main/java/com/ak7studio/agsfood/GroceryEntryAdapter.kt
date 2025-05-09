package com.ak7studio.agsfood

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class GroceryEntry(
    val date: String,
    val amount: Double,
    val addAmount: Double,
    val cash: Double,
    val balance: Double,
    val upiPayment: Double,
    val comments: String
)

class GroceryEntryAdapter(
    private val entries: MutableList<GroceryEntry>,
    private val onEdit: (GroceryEntry, Int) -> Unit,
    private val onDelete: (GroceryEntry, Int) -> Unit
) : RecyclerView.Adapter<GroceryEntryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvAddAmount: TextView = view.findViewById(R.id.tvAddAmount)
        val tvCash: TextView = view.findViewById(R.id.tvCash)
        val tvBalance: TextView = view.findViewById(R.id.tvBalance)
        val tvUpiPayment: TextView = view.findViewById(R.id.tvUpiPayment)
        val tvComments: TextView = view.findViewById(R.id.tvComments)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grocery_entry, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = entries.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvDate.text = formatToSimpleDate(entry.date)
        holder.tvAmount.text = "Amount: ₹${entry.amount}"
        holder.tvAddAmount.text = "Extra Amt ₹${entry.addAmount}"
        holder.tvCash.text = "Cash: ₹${entry.cash}"
        holder.tvBalance.text = "Bal: ₹${entry.balance}"
        holder.tvUpiPayment.text = "UPI: " + (if (entry.upiPayment > 0) "₹${entry.upiPayment}" else "")
        holder.tvComments.text = entry.comments

        holder.btnEdit.setOnClickListener { onEdit(entry, position) }
        holder.btnDelete.setOnClickListener { onDelete(entry, position) }
    }

    fun formatToSimpleDate(dateString: String): String {
        // Try ISO 8601 first
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        return try {
            val parsedDate = isoFormat.parse(dateString)
            if (parsedDate != null) outputFormat.format(parsedDate) else dateString
        } catch (e: Exception) {
            // If parsing fails, return original string or try other formats if needed
            dateString
        }
    }

}
