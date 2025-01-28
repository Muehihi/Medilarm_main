package com.example.medilarm

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(private val alarmList: List<AlarmData>) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val medicineNameTextView: TextView = view.findViewById(R.id.medicineNameTextView)
        val dosageTextView: TextView = view.findViewById(R.id.dosageTextView)
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val currentItem = alarmList[position]
        holder.medicineNameTextView.text = currentItem.medicineName
        holder.dosageTextView.text = currentItem.dosage

        // Format the time
        val formattedTime = String.format("%02d:%02d", currentItem.hour, currentItem.minute)
        holder.timeTextView.text = formattedTime
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }
}
