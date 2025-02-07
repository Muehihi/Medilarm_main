package com.example.medilarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AlarmScreen : AppCompatActivity() {

    companion object {
        private const val SNOOZE_MINUTES = 5
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var timeTextView: TextView
    private lateinit var timer: Timer
    private lateinit var database: DatabaseReference
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_screen)

        val medicineName = intent.getStringExtra("medicineName") ?: "Medicine"
        val dosage = intent.getStringExtra("dosage") ?: "Dosage"

        val txtAlarmMessage = findViewById<TextView>(R.id.alarmMessage)
        txtAlarmMessage.text = "Medicine: $medicineName, Dosage: $dosage"

        // Initialize the TextView for displaying the time
        timeTextView = findViewById(R.id.currentTime)
        updateTime() // Initial time update
        startTimer() // Start updating time every second

        // Initialize alarm sound and vibrator
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        startVibration()

        // Firebase reference to users
        database = FirebaseDatabase.getInstance().getReference("users")

        // Get current user ID
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Cancel Button
        findViewById<Button>(R.id.confirmButton).setOnClickListener {
            showConfirmationDialog(medicineName, dosage)

        }

        // Snooze Button
        findViewById<Button>(R.id.snoozeButton).setOnClickListener {
            snoozeAlarm()
            stopAlarm()
            finish()
        }
    }

    private fun updateTime() {
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        timeTextView.text = currentTime
    }

    private fun startTimer() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread { updateTime() }
            }
        }, 0, 1000) // Update every second
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 500, 1000) // Delay, Vibrate, Pause, Repeat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // Repeat indefinitely
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        vibrator.cancel()
        timer.cancel() // Stop the timer when the alarm is stopped
    }

    private fun snoozeAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("medicineName", intent.getStringExtra("medicineName"))
            putExtra("dosage", intent.getStringExtra("dosage"))
        }

        val pendingIntent = PendingIntent.getBroadcast(this, Alarm.REQUEST_CODE, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, SNOOZE_MINUTES)
        }

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun showConfirmationDialog(medicineName: String, dosage: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Medicine Intake")
        builder.setMessage("Have you taken $medicineName?")

        builder.setPositiveButton("Yes") { _, _ ->
            updateMedicineCount(medicineName, dosage)
            updateAlarmStatusInFirebase(medicineName, dosage, false)
            stopAlarm()
            finish()
        }

        builder.setNegativeButton("No") { _, _ ->
            stopAlarm()
            finish()
        }

        builder.show()
    }

    private fun updateMedicineCount(medicineName: String, dosage: String) {
        val dosageInt = dosage.toIntOrNull() ?: return
        val userMedicineRef = FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("medicines").whereEqualTo("name", medicineName)

        userMedicineRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val medicineDoc = snapshot.documents[0]
                val currentCount = medicineDoc.getLong("count")?.toInt() ?: 0
                val updatedCount = currentCount - dosageInt

                if (updatedCount >= 0) {
                    medicineDoc.reference.update("count", updatedCount)
                    // Notify user if the count is below 5
                    if (updatedCount < 5) {
                        notifyLowMedicineCount()
                    }
                } else {
                    // Handle case where count is negative (possibly notify user)
                    Toast.makeText(this, "Not enough medicine left!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Medicine not found in your records!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error updating medicine count: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAlarmStatusInFirebase(medicineName: String, dosage: String, isEnabled: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Find the alarm in the Firebase collection
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("alarms")
            .whereEqualTo("medicineName", medicineName)
            .whereEqualTo("dosage", dosage)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) { // Use isEmpty instead of isNotEmpty
                    val documentId = documents.documents.first().id // Get the document ID
                    FirebaseFirestore.getInstance().collection("users").document(userId).collection("alarms")
                        .document(documentId)
                        .update("isAlarmEnabled", isEnabled)
                        .addOnSuccessListener {
                            // Optionally, notify the user or update the UI
                        }
                }
            }

    }


    private fun notifyLowMedicineCount() {
        // Show a Toast message to notify the user
        Toast.makeText(this, "Medicine count is below 5. Please restock!", Toast.LENGTH_LONG).show()

        // Optionally, show a dialog
        AlertDialog.Builder(this)
            .setTitle("Low Medicine Alert")
            .setMessage("Your remaining medicine count is below 5. Please restock soon.")
            .setPositiveButton("Okay", null)
            .show()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
