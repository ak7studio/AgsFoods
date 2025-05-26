package com.ak7studio.agsfood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView

data class SaleForecast(
    val date: String,
    val shift: String, // "Morning" or "Evening"
    val sales: Int,
    val total: Int,
    val details: List<ForecastItem>
)

data class ForecastItem(
    var item: String,
    var qty: Int,
    var unit: String,
    var unitPrice: Int,
    var totalPrice: Int
)
class ForecastItemAdapter(
    private val items: MutableList<ForecastItem>,
    private val itemList: List<String>,
    private val unitList: List<String>,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<ForecastItemAdapter.ForecastItemViewHolder>() {

    inner class ForecastItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val spItem: Spinner = view.findViewById(R.id.spItem)
        val etQty: EditText = view.findViewById(R.id.etQty)
        val spUnit: Spinner = view.findViewById(R.id.spUnit)
//        val etUnitPrice: EditText = view.findViewById(R.id.etUnitPrice)
        val etTotalPrice: EditText = view.findViewById(R.id.etTotalPrice)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast_detail_row, parent, false)
        return ForecastItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastItemViewHolder, position: Int) {
        val currentItem = items[position]
        val itemAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, itemList)
        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spItem.adapter = itemAdapter
        val itemPosition = itemList.indexOf(currentItem.item)
        holder.spItem.setSelection(if (itemPosition >= 0) itemPosition else 0)
        holder.spItem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                currentItem.item = itemList[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        holder.etQty.setText(currentItem.qty.toString())
        holder.etQty.doOnTextChanged { text, _, _, _ ->
            currentItem.qty = text.toString().toIntOrNull() ?: 0
//            updateTotal(holder, currentItem)
            onDataChanged()
        }
        // Setup unit Spinner similarly
        val unitAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, unitList)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spUnit.adapter = unitAdapter
        val unitPosition = unitList.indexOf(currentItem.unit)
        holder.spUnit.setSelection(if (unitPosition >= 0) unitPosition else 0)
        holder.spUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                currentItem.unit = unitList[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        /*holder.etUnitPrice.setText(currentItem.unitPrice.toString())
        holder.etUnitPrice.doOnTextChanged { text, _, _, _ ->
            currentItem.unitPrice = text.toString().toIntOrNull() ?: 0
            updateTotal(holder, currentItem)
        }*/
        holder.etTotalPrice.setText(currentItem.totalPrice.toString())
        holder.etTotalPrice.doOnTextChanged { text, _, _, _ ->
            currentItem.totalPrice = text.toString().toIntOrNull() ?: 0
            onDataChanged()
        }
        holder.btnDelete.setOnClickListener {
            items.removeAt(position)
            notifyItemRemoved(position)
            onDataChanged()
        }
    }

    override fun getItemCount() = items.size

    private fun updateTotal(holder: ForecastItemViewHolder, item: ForecastItem) {
        item.totalPrice = item.qty * item.unitPrice
        holder.etTotalPrice.setText(item.totalPrice.toString())
        onDataChanged()
    }
}
