package com.example.medilarm

data class AlarmData(
    val medicineName: String,
    val dosage: String,
    var hour: Int,
    var minute: Int,
    var isAlarmEnabled: Boolean = true,
    val type: String = "",
    val selectedDays: List<Int> = listOf(),
    val startDate: Long? = null,
    val endDate: Long? = null
)



