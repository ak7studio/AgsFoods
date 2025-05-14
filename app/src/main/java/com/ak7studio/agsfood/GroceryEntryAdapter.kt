package com.ak7studio.agsfood

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.core.utilities.Utilities
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone



class GroceryEntryAdapter(
    private val context: Context,
    private val entries: MutableList<GroceryEntry>,
    private val onEdit: (GroceryEntry, Int) -> Unit,
    private val onDelete: (GroceryEntry, Int) -> Unit
) : RecyclerView.Adapter<GroceryEntryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvSettlement: TextView = view.findViewById(R.id.tvSettlement)

        init {
            view.setOnClickListener {
                showDetailDialog(adapterPosition)
            }
        }
    }

    private fun showDetailDialog(startIndex: Int) {
        if (startIndex !in entries.indices) return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_grocery_detail, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        var currentIndex = startIndex

        val btnPrev = dialogView.findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = dialogView.findViewById<ImageButton>(R.id.btnNext)
        val btnEdit = dialogView.findViewById<ImageButton>(R.id.btnEdit)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btnDelete)

        fun updateDialog(index: Int) {
            val entry = entries[index]
//            dialogView.findViewById<TextView>(R.id.tvDetailDate).text = isoToLocalDateString(entry.date)
            dialogView.findViewById<TextView>(R.id.tvDetailDate).text = entry.date
            dialogView.findViewById<TextView>(R.id.tvDetailAmount).text = "₹${entry.amount.toInt()}"
            dialogView.findViewById<TextView>(R.id.tvDetailAddAmount).text = "₹${entry.addAmount.toInt()}"
            dialogView.findViewById<TextView>(R.id.tvDetailCash).text = "₹${entry.cash.toInt()}"
            dialogView.findViewById<TextView>(R.id.tvDetailBalance).text = "₹${entry.balance.toInt()}"
            dialogView.findViewById<TextView>(R.id.tvDetailUpiPayment).text = "₹${entry.upiPayment.toInt()}"
            dialogView.findViewById<TextView>(R.id.tvDetailComments).text = entry.comments

            // Hide Prev button if at first item
            btnPrev.visibility = if (index > 0) View.VISIBLE else View.INVISIBLE

            // Hide Next button if at last item
            btnNext.visibility = if (index < entries.size - 1) View.VISIBLE else View.INVISIBLE

            btnDelete.visibility = if(UserPrefs.getRole() == UserPrefs.KEY_MANAGER) View.VISIBLE else View.INVISIBLE

        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateDialog(currentIndex)
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < entries.size - 1) {
                currentIndex++
                updateDialog(currentIndex)
            }
        }

        // Edit and Delete button handlers remain unchanged
        btnEdit.setOnClickListener {
            val entry = entries[currentIndex]
            onEdit(entry, currentIndex)
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val entry = entries[currentIndex]
            onDelete(entry, currentIndex)
            dialog.dismiss()
        }

        updateDialog(currentIndex)
        dialog.show()
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
        holder.tvDate.text = entry.date
        holder.tvAmount.text = "${entry.amount.toInt()}"
        if (entry.balance == 0.0) {
            holder.tvSettlement.text = "Settled"
            holder.tvSettlement.setTextColor(ContextCompat.getColor(context, R.color.settled_green))
        } else {
            holder.tvSettlement.text = "Unpaid"
            holder.tvSettlement.setTextColor(ContextCompat.getColor(context, R.color.unpaid_red))
        }
    }
}
