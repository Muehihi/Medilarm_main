package com.example.medilarm

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AlarmAdapter(
    private val alarmList: List<AlarmData>,
    private val onDeleteClick: (Int) -> Unit,
    private val onToggleAlarm: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val medicineNameTextView: TextView = view.findViewById(R.id.medicineNameTextView)
        val dosageTextView: TextView = view.findViewById(R.id.dosageTextView)
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val amPmTextView: TextView = view.findViewById(R.id.amPmTextView)
        val alarmSwitch: SwitchCompat = view.findViewById(R.id.alarmSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val currentItem = alarmList[position]
        holder.medicineNameTextView.text = currentItem.medicineName
        holder.dosageTextView.text = currentItem.dosage

        val amPm = if (currentItem.hour >= 12) "PM" else "AM"
        val hour = if (currentItem.hour % 12 == 0) 12 else currentItem.hour % 12
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, currentItem.minute)

        holder.timeTextView.text = formattedTime
        holder.amPmTextView.text = amPm
        holder.amPmTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        // Handle switch state based on whether the alarm is enabled
        holder.alarmSwitch.isChecked = currentItem.isAlarmEnabled

        // Toggle switch to enable/disable the alarm
        holder.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentItem.isAlarmEnabled = isChecked
            onToggleAlarm(position, isChecked) // Notify the activity about the change
        }

        // Handle long press to delete
        holder.itemView.setOnLongClickListener {
            onDeleteClick(position)
            true
        }
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }
}
