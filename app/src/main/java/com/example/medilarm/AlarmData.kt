package com.example.medilarm

data class AlarmData(
    val medicineName: String,
    val dosage: String,
    val hour: Int,
    val minute: Int,
    var isAlarmEnabled: Boolean = true
)



