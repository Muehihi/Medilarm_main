package com.example.medilarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val medicineName = intent?.getStringExtra("medicineName") ?: "Medicine"
            val dosage = intent?.getStringExtra("dosage") ?: "Dosage"

            // Start the alarm activity when the alarm goes off
            val alarmScreenIntent = Intent(context, AlarmScreen::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("medicineName", medicineName)
                putExtra("dosage", dosage)
            }
            context.startActivity(alarmScreenIntent)


            // Show Notification as well
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "MedicineReminderChannel"
                val description = "Channel for medicine reminders"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel("medicineReminderChannel", name, importance)
                channel.description = description

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, "medicineReminderChannel")
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle("Medicine Reminder")
                .setContentText("Time to take $medicineName ($dosage)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(longArrayOf(0, 1000, 500, 1000))

            with(NotificationManagerCompat.from(context)) {
                val notificationId = medicineName.hashCode()
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(notificationId, builder.build())
                }
            }

            Toast.makeText(context, "Time to take $medicineName ($dosage)", Toast.LENGTH_LONG).show()
        }
    }
}
