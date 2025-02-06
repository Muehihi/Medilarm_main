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
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AlarmScreen : AppCompatActivity() {

    companion object {
        private const val SNOOZE_MINUTES = 5
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_screen)

        val medicineName = intent.getStringExtra("medicineName") ?: "Medicine"
        val dosage = intent.getStringExtra("dosage") ?: "Dosage"

        val txtAlarmMessage = findViewById<TextView>(R.id.alarmMessage)
        txtAlarmMessage.text = "Medicine: $medicineName, Dosage: $dosage"

        // Initialize alarm sound and vibrator
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        startVibration()

        // Cancel Button
        findViewById<Button>(R.id.confirmButton).setOnClickListener {
            stopAlarm()
            finish()
        }

        // Snooze Button
        findViewById<Button>(R.id.snoozeButton).setOnClickListener {
            snoozeAlarm()
            stopAlarm()
            finish()
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 500, 1000) // Delay, Vibrate, Pause, Repeat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, 0) // Repeat indefinitely
            )
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        vibrator.cancel()
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

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
