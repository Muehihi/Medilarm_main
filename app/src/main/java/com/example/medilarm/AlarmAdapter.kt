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
    private val onToggleAlarm: (Int, Boolean) -> Unit,
    private val onEditClick: (Int) -> Unit

) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val medicineNameTextView: TextView = view.findViewById(R.id.medicineNameTextView)
        val dosageTextView: TextView = view.findViewById(R.id.dosageTextView)
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val amPmTextView: TextView = view.findViewById(R.id.amPmTextView)
        val alarmSwitch: SwitchCompat = view.findViewById(R.id.alarmSwitch)
        val repeatDaysText: TextView = view.findViewById(R.id.repeatDaysText)
        val dateRangeText: TextView = view.findViewById(R.id.dateRangeText)
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

        // Set switch state from the alarm's current state
        holder.alarmSwitch.isChecked = currentItem.isAlarmEnabled

        // Handle switch toggle
        holder.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentItem.isAlarmEnabled = isChecked
            onToggleAlarm(position, isChecked)
        }

        // Handle click for editing
        holder.itemView.setOnClickListener {
            onEditClick(position)
        }

        // Handle long press to delete
        holder.itemView.setOnLongClickListener {
            onDeleteClick(position)
            true
        }

        // Set repeat days text
        if (currentItem.selectedDays.isNotEmpty()) {
            val dayNames = currentItem.selectedDays.map { dayIndex ->
                when (dayIndex) {
                    0 -> "Sun"
                    1 -> "Mon"
                    2 -> "Tue"
                    3 -> "Wed"
                    4 -> "Thu"
                    5 -> "Fri"
                    6 -> "Sat"
                    else -> ""
                }
            }
            holder.repeatDaysText.text = "Repeats on: ${dayNames.joinToString(", ")}"
            holder.repeatDaysText.visibility = View.VISIBLE
        } else {
            holder.repeatDaysText.visibility = View.GONE
        }

        // Set date range text
        if (currentItem.startDate != null || currentItem.endDate != null) {
            val dateFormat = android.text.format.DateFormat.getMediumDateFormat(holder.itemView.context)
            val startDateStr = currentItem.startDate?.let { dateFormat.format(it) } ?: "Not set"
            val endDateStr = currentItem.endDate?.let { dateFormat.format(it) } ?: "Not set"
            holder.dateRangeText.text = "From: $startDateStr To: $endDateStr"
            holder.dateRangeText.visibility = View.VISIBLE
        } else {
            holder.dateRangeText.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }
}
