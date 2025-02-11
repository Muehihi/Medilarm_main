package com.example.medilarm

import androidx.core.app.ActivityCompat
import android.Manifest
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicineAdapter(
    private val medicineList: MutableList<ShelfActivity.Medicine>,
    private val onDeleteClick: (Int) -> Unit,
    private val onEditClick: (ShelfActivity.Medicine, Int) -> Unit // Edit callback
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.medicineName)
        val countTextView: TextView = itemView.findViewById(R.id.medicineCount)
        val expirationTextView: TextView = itemView.findViewById(R.id.medicineExpiration)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        val editTextButton: TextView = itemView.findViewById(R.id.editTextButton)  // The TextView for Edit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.medicine_item, parent, false)
        return MedicineViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicineList[position]
        holder.nameTextView.text = medicine.name
        holder.countTextView.text = "Count: ${medicine.count}"
        holder.expirationTextView.text = "Expires: ${medicine.expirationDate}"

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }

        // Handle edit button click (now it's a TextView)
        holder.editTextButton.setOnClickListener {
            onEditClick(medicine, position) // Trigger the callback for editing
        }
    }

    override fun getItemCount(): Int {
        return medicineList.size
    }

    // Method to remove an item from the list
    fun removeItem(position: Int) {
        medicineList.removeAt(position) // Remove item from the list
        notifyItemRemoved(position)      // Notify adapter about the item removal
    }
}
