package com.example.medilarm

import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TimePicker
import android.widget.Toast
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
    private lateinit var medicineNameEditText: EditText
    private lateinit var dosageEditText: EditText
    private lateinit var timePicker: TimePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alarm)

        // Initialize Firestore and FirebaseAuth
        firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // Initialize views
        medicineNameEditText = findViewById(R.id.medicineName)
        dosageEditText = findViewById(R.id.dosage)
        timePicker = findViewById(R.id.timePicker)

        // Edge-to-edge setup (if needed)
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
            val medicineName = medicineNameEditText.text.toString()
            val dosage = dosageEditText.text.toString()
            val hour = timePicker.hour
            val minute = timePicker.minute

            if (medicineName.isNotEmpty() && dosage.isNotEmpty()) {
                // Get the currently authenticated user's ID
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    // Prepare the alarm data
                    val alarmData = hashMapOf(
                        "medicineName" to medicineName,
                        "dosage" to dosage,
                        "hour" to hour,
                        "minute" to minute
                    )

                    // Save the alarm data under the user's document in Firestore
                    firestore.collection("users")
                        .document(userId)
                        .collection("alarms")
                        .add(alarmData)
                        .addOnSuccessListener { documentReference ->
                            // Set the alarm
                            setAlarm(hour, minute, medicineName, dosage)
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
                Toast.makeText(this, "Please enter medicine name and dosage.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setAlarm(hour: Int, minute: Int, medicineName: String, dosage: String) {
        val alarmManager = getSystemService(AlarmManager::class.java) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmIntent.putExtra("medicineName", medicineName)
        alarmIntent.putExtra("dosage", dosage)

        val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE, alarmIntent, PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

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