package com.example.medilarm

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
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
    private lateinit var welcomeText: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        // Initialize the welcome TextView
        welcomeText = findViewById(R.id.alarm)

        // First handle the window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets  // Make sure to return the insets
        }

        // Then load user data and set up the UI
        loadUserName()

        recyclerView = findViewById(R.id.recyclerView)
        alarmAdapter = AlarmAdapter(
            alarmList,
            { position -> showDeleteConfirmationDialog(position) },
            { position, isEnabled -> updateAlarmStatusInFirebase(position, isEnabled) },
            { position -> editAlarm(position) }
        )
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
                    val type = document.getString("type") ?: ""

                    // Convert selected days to List<Int>
                    val selectedDays = (document.get("selectedDays") as? List<*>)?.mapNotNull {
                        when (it) {
                            is Long -> it.toInt()
                            is Int -> it
                            else -> null
                        }
                    } ?: listOf()

                    // Get start and end dates
                    val startDate = document.getLong("startDate")
                    val endDate = document.getLong("endDate")

                    val alarm = AlarmData(
                        medicineName = medicineName,
                        dosage = dosage,
                        hour = hour,
                        minute = minute,
                        isAlarmEnabled = isEnabled,
                        type = type,
                        selectedDays = selectedDays,
                        startDate = startDate,
                        endDate = endDate
                    )
                    alarmList.add(alarm)
                }
                alarmAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load alarms: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Update Firestore document with the new alarm state
        db.collection("users").document(userId).collection("alarms")
            .whereEqualTo("medicineName", alarmToUpdate.medicineName)
            .whereEqualTo("dosage", alarmToUpdate.dosage)
            .whereEqualTo("hour", alarmToUpdate.hour)
            .whereEqualTo("minute", alarmToUpdate.minute)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0) {  // Corrected check to ensure there are documents
                    val documentId = documents.documents.first().id
                    // Update the alarm's status in Firestore
                    db.collection("users").document(userId).collection("alarms")
                        .document(documentId)
                        .update("isAlarmEnabled", isEnabled)
                        .addOnSuccessListener {
                            // Update the local alarm list
                            alarmList[position].isAlarmEnabled = isEnabled

                            // Notify the adapter to refresh the alarm at the given position
                            alarmAdapter.notifyItemChanged(position)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update alarm status.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun editAlarm(position: Int) {
        val alarm = alarmList[position]
        val userId = currentUser?.uid ?: return

        // First get the full alarm data from Firestore
        db.collection("users").document(userId).collection("alarms")
            .whereEqualTo("medicineName", alarm.medicineName)
            .whereEqualTo("dosage", alarm.dosage)
            .whereEqualTo("hour", alarm.hour)
            .whereEqualTo("minute", alarm.minute)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val intent = Intent(this, Alarm::class.java).apply {
                        putExtra("isEditing", true)
                        putExtra("medicineName", document.getString("medicineName"))
                        putExtra("dosage", document.getString("dosage"))
                        putExtra("hour", document.getLong("hour")?.toInt())
                        putExtra("minute", document.getLong("minute")?.toInt())
                        putExtra("isAlarmEnabled", document.getBoolean("isAlarmEnabled") ?: true)
                        putExtra("type", document.getString("type"))

                        // Convert the selected days to ArrayList<Int>
                        val selectedDays = (document.get("selectedDays") as? List<*>)?.mapNotNull {
                            when (it) {
                                is Long -> it.toInt()
                                is Int -> it
                                else -> null
                            }
                        } ?: listOf()
                        putIntegerArrayListExtra("selectedDays", ArrayList(selectedDays))

                        putExtra("startDate", document.getLong("startDate"))
                        putExtra("endDate", document.getLong("endDate"))
                        putExtra("documentId", document.id)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Could not find alarm details", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading alarm details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserName() {
        val userId = currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val firstName = (document.getString("fname") ?: "User").capitalize()
                        welcomeText.text = "Welcome $firstName!"
                    }
                }
                .addOnFailureListener {
                    welcomeText.text = "Welcome!"
                }
        } else {
            welcomeText.text = "Welcome!"
        }
    }

}
