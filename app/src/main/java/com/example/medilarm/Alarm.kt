package com.example.medilarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class Alarm : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE = 100
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var medicineSpinner: Spinner
    private lateinit var dosageEditText: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var typeSpinner: Spinner  // Spinner for dosage type

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

        // Fetch medicines from Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("medicines")
                .get()
                .addOnSuccessListener { result ->
                    val medicineList = mutableListOf<String>()
                    for (document in result) {
                        val medicineName = document.getString("name")
                        if (medicineName != null) {
                            medicineList.add(medicineName)
                        }
                    }
                    if (medicineList.isEmpty()) {
                        Toast.makeText(this, "No medicines found in shelf. Please add medicine.", Toast.LENGTH_LONG).show()
                    } else {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, medicineList)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        medicineSpinner.adapter = adapter
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching medicines: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Set up the Spinner for dosage types
        val dosageTypes = arrayOf("Pill", "Liquid", "Injection")  // Example dosage types
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dosageTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        // Back button
        val btnBack: ImageView = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        // Save button
        val saveButton = findViewById<Button>(R.id.btn_save)
        saveButton.setOnClickListener {
            val selectedMedicine = medicineSpinner.selectedItem.toString()
            val dosage = dosageEditText.text.toString()
            val selectedType = typeSpinner.selectedItem.toString()
            val hour = timePicker.hour
            val minute = timePicker.minute

            if (selectedMedicine.isNotEmpty() && dosage.isNotEmpty()) {
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    // Prepare the alarm data
                    val alarmData = hashMapOf(
                        "medicineName" to selectedMedicine,
                        "dosage" to dosage,
                        "type" to selectedType,
                        "hour" to hour,
                        "minute" to minute
                    )

                    // Save the alarm data under the user's document in Firestore
                    firestore.collection("users")
                        .document(userId)
                        .collection("alarms")
                        .add(alarmData)
                        .addOnSuccessListener {
                            setAlarm(hour, minute, selectedMedicine, dosage)
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
                Toast.makeText(this, "Please select medicine and enter dosage.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setAlarm(hour: Int, minute: Int, medicineName: String, dosage: String) {
        val alarmManager = getSystemService(AlarmManager::class.java) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmIntent.putExtra("medicineName", medicineName)
        alarmIntent.putExtra("dosage", dosage)

        // Generate a unique request code based on medicineName, hour, and minute
        val requestCode = "$medicineName$hour$minute".hashCode()

        // Use the unique requestCode for the PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If the alarm time is in the past, set it for the next day
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Set the alarm
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }


    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
