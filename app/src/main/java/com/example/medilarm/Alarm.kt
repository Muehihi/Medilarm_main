package com.example.medilarm

import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TimePicker
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.TextView
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Alarm : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE = 100
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var dosageEditText: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var medicineSpinner: Spinner
    private lateinit var typeSpinner: Spinner
    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var calendarContainer: FrameLayout
    private lateinit var calendarOverlay: View
    private val selectedDays = mutableSetOf<Int>()
    private lateinit var dayButtons: List<Button>
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var isSelectingStartDate = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alarm)

        // Initialize Firestore and FirebaseAuth
        firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // Initialize views
        medicineSpinner = findViewById(R.id.medicineSpinner)
        dosageEditText = findViewById(R.id.dosage)
        timePicker = findViewById(R.id.timePicker)
        typeSpinner = findViewById(R.id.type)
        startDateText = findViewById(R.id.startDateText)
        endDateText = findViewById(R.id.endDateText)
        calendarView = findViewById(R.id.calendarView)
        calendarContainer = findViewById(R.id.calendarContainer)
        calendarOverlay = findViewById(R.id.calendarOverlay)

        // Setup medicine spinner with Firestore data
        setupMedicineSpinner()

        // Setup type spinner with static data
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf("Tablet", "Capsule", "Liquid", "Injection")
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        // Initialize day buttons
        dayButtons = listOf(
            findViewById(R.id.btn_sun),
            findViewById(R.id.btn_mon),
            findViewById(R.id.btn_tue),
            findViewById(R.id.btn_wed),
            findViewById(R.id.btn_thu),
            findViewById(R.id.btn_fri),
            findViewById(R.id.btn_sat)
        )

        // Set up day button listeners
        dayButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                toggleDaySelection(index, button)
            }
        }

        // Start date click listener
        startDateText.setOnClickListener {
            isSelectingStartDate = true
            showCalendar()
        }

        // End date click listener
        endDateText.setOnClickListener {
            if (startDate == null) {
                Toast.makeText(this, "Please select start date first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isSelectingStartDate = false
            showCalendar()
        }

        // Overlay click listener to dismiss calendar
        calendarOverlay.setOnClickListener {
            hideCalendar()
        }

        // Calendar selection listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            if (isSelectingStartDate) {
                startDate = calendar.timeInMillis
                startDateText.text = formatDate(calendar.timeInMillis)
                hideCalendar()
            } else {
                if (calendar.timeInMillis < startDate!!) {
                    Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                    return@setOnDateChangeListener
                }
                endDate = calendar.timeInMillis
                endDateText.text = formatDate(calendar.timeInMillis)
                hideCalendar()
            }
        }

        // Edge-to-edge setup
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button
        val btnBack: ImageView = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        // Save button
        val saveButton = findViewById<Button>(R.id.btn_save)
        saveButton.setOnClickListener {
            val medicineName = medicineSpinner.selectedItem?.toString() ?: ""
            val dosage = dosageEditText.text.toString()
            val type = typeSpinner.selectedItem?.toString() ?: ""
            val hour = timePicker.hour
            val minute = timePicker.minute

            if (medicineName.isNotEmpty() && dosage.isNotEmpty() && type.isNotEmpty()) {
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    val alarmData = hashMapOf(
                        "medicineName" to medicineName,
                        "dosage" to dosage,
                        "type" to type,
                        "hour" to hour,
                        "minute" to minute,
                        "selectedDays" to selectedDays.toList(),
                        "isRepeating" to selectedDays.isNotEmpty(),
                        "startDate" to startDate,
                        "endDate" to endDate
                    )

                    firestore.collection("users")
                        .document(userId)
                        .collection("alarms")
                        .add(alarmData)
                        .addOnSuccessListener { documentReference ->
                            if (selectedDays.isEmpty()) {
                                setOneTimeAlarm(hour, minute, medicineName, dosage)
                            } else {
                                setRepeatingAlarm(hour, minute, medicineName, dosage)
                            }
                            Toast.makeText(this, "Alarm set successfully!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, HomePage::class.java)
                            startActivity(intent)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error setting alarm: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "No user is signed in.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Request notification permission
        requestNotificationPermission()
    }

    private fun showCalendar() {
        calendarContainer.visibility = View.VISIBLE
    }

    private fun hideCalendar() {
        calendarContainer.visibility = View.GONE
    }

    private fun formatDate(timeInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        return android.text.format.DateFormat.format("MMMM dd, yyyy", calendar).toString()
    }

    private fun setupMedicineSpinner() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("medicines")
                .get()
                .addOnSuccessListener { documents ->
                    val medicineNames = mutableListOf<String>()
                    for (document in documents) {
                        val medicineName = document.getString("name")
                        if (medicineName != null) {
                            medicineNames.add(medicineName)
                        }
                    }

                    if (medicineNames.isEmpty()) {
                        Toast.makeText(
                            this,
                            "No medicines found. Please add medicines first.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        medicineNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    medicineSpinner.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error loading medicines: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(
                this,
                "Please sign in to view medicines",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleDaySelection(dayIndex: Int, button: Button) {
        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex)
            button.setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
        } else {
            selectedDays.add(dayIndex)
            button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, theme))
        }
    }

    private fun setOneTimeAlarm(hour: Int, minute: Int, medicineName: String, dosage: String) {
        val alarmManager = getSystemService(AlarmManager::class.java) as AlarmManager

        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmIntent.putExtra("medicineName", medicineName)
        alarmIntent.putExtra("dosage", dosage)
        if (endDate != null) {
            alarmIntent.putExtra("endDate", endDate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        // If start date is set, use it
        if (startDate != null) {
            val startCalendar = Calendar.getInstance()
            startCalendar.timeInMillis = startDate!!
            startCalendar.set(Calendar.HOUR_OF_DAY, hour)
            startCalendar.set(Calendar.MINUTE, minute)
            startCalendar.set(Calendar.SECOND, 0)

            if (startCalendar.timeInMillis > System.currentTimeMillis()) {
                calendar.timeInMillis = startCalendar.timeInMillis
            }
        } else if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun setRepeatingAlarm(hour: Int, minute: Int, medicineName: String, dosage: String) {
        val alarmManager = getSystemService(AlarmManager::class.java) as AlarmManager

        selectedDays.forEach { dayOfWeek ->
            val alarmIntent = Intent(this, AlarmReceiver::class.java)
            alarmIntent.putExtra("medicineName", medicineName)
            alarmIntent.putExtra("dosage", dosage)
            if (endDate != null) {
                alarmIntent.putExtra("endDate", endDate)
            }

            val requestCode = REQUEST_CODE + dayOfWeek
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance()

            // If start date is set, use it
            if (startDate != null) {
                calendar.timeInMillis = startDate!!
            }

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            // Set to the next occurrence of this day
            while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek + 1 ||
                calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_WEEK, 1)
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                pendingIntent
            )
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}