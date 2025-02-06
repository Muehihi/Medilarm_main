package com.example.medilarm

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomePage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private var alarmList: MutableList<AlarmData> = mutableListOf()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerView)
        alarmAdapter = AlarmAdapter(alarmList, { position -> showDeleteConfirmationDialog(position) }) { position, isEnabled ->
            updateAlarmStatusInFirebase(position, isEnabled)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = alarmAdapter

        loadUserAlarms()

        val btnAdd: ImageView = findViewById(R.id.btn_add)
        btnAdd.setOnClickListener {
            startActivity(Intent(this, Alarm::class.java))
        }

        val btnProfile: ImageView = findViewById(R.id.btn_Profile)
        btnProfile.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        val btnshelf: ImageView = findViewById(R.id.btn_shelf)
        btnshelf.setOnClickListener {
            startActivity(Intent(this, ShelfActivity::class.java))
        }
    }

    private fun loadUserAlarms() {
        val userId = currentUser?.uid ?: return

        db.collection("users").document(userId).collection("alarms")
            .get()
            .addOnSuccessListener { documents ->
                alarmList.clear()
                for (document in documents) {
                    val medicineName = document.getString("medicineName") ?: ""
                    val dosage = document.getString("dosage") ?: ""
                    val hour = document.getLong("hour")?.toInt() ?: 0
                    val minute = document.getLong("minute")?.toInt() ?: 0
                    val isEnabled = document.getBoolean("isAlarmEnabled") ?: true

                    val alarm = AlarmData(medicineName, dosage, hour, minute, isEnabled)
                    alarmList.add(alarm)
                }
                alarmAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load alarms.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to delete this alarm?")
            .setPositiveButton("Yes") { dialog, id ->
                deleteAlarmFromFirebase(position)
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun deleteAlarmFromFirebase(position: Int) {
        val userId = currentUser?.uid ?: return
        val alarmToDelete = alarmList[position]

        db.collection("users").document(userId).collection("alarms")
            .whereEqualTo("medicineName", alarmToDelete.medicineName)
            .whereEqualTo("dosage", alarmToDelete.dosage)
            .whereEqualTo("hour", alarmToDelete.hour)
            .whereEqualTo("minute", alarmToDelete.minute)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val documentId = documents.first().id
                    db.collection("users").document(userId).collection("alarms")
                        .document(documentId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Alarm deleted.", Toast.LENGTH_SHORT).show()
                            alarmList.removeAt(position)
                            alarmAdapter.notifyItemRemoved(position)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to delete alarm from Firebase.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "No alarm found to delete.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to find alarm in Firestore.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAlarmStatusInFirebase(position: Int, isEnabled: Boolean) {
        val userId = currentUser?.uid ?: return
        val alarmToUpdate = alarmList[position]

        db.collection("users").document(userId).collection("alarms")
            .whereEqualTo("medicineName", alarmToUpdate.medicineName)
            .whereEqualTo("dosage", alarmToUpdate.dosage)
            .whereEqualTo("hour", alarmToUpdate.hour)
            .whereEqualTo("minute", alarmToUpdate.minute)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val documentId = documents.first().id
                    db.collection("users").document(userId).collection("alarms")
                        .document(documentId)
                        .update("isAlarmEnabled", isEnabled)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Alarm status updated.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update alarm status.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
}
