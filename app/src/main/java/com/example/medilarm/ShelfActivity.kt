package com.example.medilarm

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.textfield.TextInputEditText
import android.app.DatePickerDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class ShelfActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var medicineAdapter: MedicineAdapter
    private val medicineList: MutableList<Medicine> = mutableListOf()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shelf)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with the mutable list and the delete & edit callbacks
        medicineAdapter = MedicineAdapter(
            medicineList,
            { position ->
                // Handle delete button click (remove item from list)
                deleteMedicineFromFirebase(medicineList[position].name)
            },
            { medicine, position ->
                // Handle edit button click (show edit dialog)
                showEditMedicineDialog(medicine, position)
            }
        )
        recyclerView.adapter = medicineAdapter

        // Get the current userId (assumes Firebase Authentication is set up)
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            userId = user.uid
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch medicines from Firebase Firestore
        fetchMedicinesFromFirebase()

        // Add button to open dialog
        val addButton = findViewById<ImageView>(R.id.medicineAddButton)
        addButton.setOnClickListener {
            // Open dialog to add new medicine
            showAddMedicineDialog()
        }

        val btnAdd: ImageView = findViewById(R.id.btn_Clock)
        btnAdd.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        val btnProfile: ImageView = findViewById(R.id.btn_Profile)
        btnProfile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }
    }

    // Fetch medicines from Firebase Firestore
    private fun fetchMedicinesFromFirebase() {
        firestore.collection("users")
            .document(userId)
            .collection("medicines")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result: QuerySnapshot = task.result!!
                    medicineList.clear() // Clear previous list
                    for (document in result) {
                        val name = document.getString("name") ?: ""
                        val count = document.getLong("count")?.toInt() ?: 0
                        val expirationDate = document.getString("expirationDate") ?: ""
                        val medicine = Medicine(name, count, expirationDate)
                        medicineList.add(medicine)
                    }
                    medicineAdapter.notifyDataSetChanged() // Notify the adapter that the data has been updated
                } else {
                    Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to show the add medicine dialog
    private fun showAddMedicineDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null)

        // Get reference to expiration input field
        val expirationInput = dialogView.findViewById<TextInputEditText>(R.id.medicineExpirationInput)

        // Make the expiration input field read-only
        expirationInput.isFocusable = false
        expirationInput.isClickable = true

        // Show DatePicker when clicking the expiration input
        expirationInput.setOnClickListener {
            showDatePicker(expirationInput)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Medicine")
            .setView(dialogView)
            .setPositiveButton("Add") { dialogInterface: DialogInterface, _ ->
                val name = dialogView.findViewById<TextInputEditText>(R.id.medicineNameInput).text.toString()
                val countString = dialogView.findViewById<TextInputEditText>(R.id.medicineCountInput).text.toString()
                val expirationDate = expirationInput.text.toString()

                if (name.isNotEmpty() && countString.isNotEmpty() && expirationDate.isNotEmpty()) {
                    val count = countString.toIntOrNull()
                    if (count != null) {
                        addMedicineToFirebase(name, count, expirationDate)
                    } else {
                        Toast.makeText(this, "Please enter a valid number for count", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    // Add medicine to Firebase Firestore
    private fun addMedicineToFirebase(name: String, count: Int, expirationDate: String) {
        val medicine = hashMapOf(
            "name" to name,
            "count" to count,
            "expirationDate" to expirationDate
        )
        firestore.collection("users")
            .document(userId)
            .collection("medicines")
            .add(medicine)
            .addOnSuccessListener {
                Toast.makeText(this, "Medicine added", Toast.LENGTH_SHORT).show()
                fetchMedicinesFromFirebase() // Refresh the data
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to show the edit medicine dialog
    private fun showEditMedicineDialog(medicine: Medicine, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null)

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.medicineNameInput)
        val countInput = dialogView.findViewById<TextInputEditText>(R.id.medicineCountInput)
        val expirationInput = dialogView.findViewById<TextInputEditText>(R.id.medicineExpirationInput)

        // Make the expiration input field read-only
        expirationInput.isFocusable = false
        expirationInput.isClickable = true

        // Show DatePicker when clicking the expiration input
        expirationInput.setOnClickListener {
            showDatePicker(expirationInput)
        }

        nameInput.setText(medicine.name)
        countInput.setText(medicine.count.toString())
        expirationInput.setText(medicine.expirationDate)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Medicine")
            .setView(dialogView)
            .setPositiveButton("Save") { dialogInterface: DialogInterface, _ ->
                val newName = nameInput.text.toString()
                val newCountString = countInput.text.toString()

                if (newName.isNotEmpty() && newCountString.isNotEmpty()) {
                    val newCount = newCountString.toIntOrNull()
                    if (newCount != null) {
                        updateMedicineInFirebase(medicine.name, newName, newCount)
                    } else {
                        Toast.makeText(this, "Please enter a valid number for count", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    // Update medicine in Firebase Firestore
    private fun updateMedicineInFirebase(oldName: String, newName: String, newCount: Int) {
        val updatedMedicine = hashMapOf(
            "name" to newName,
            "count" to newCount
        )
        firestore.collection("users")
            .document(userId)
            .collection("medicines")
            .whereEqualTo("name", oldName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val document = querySnapshot.documents.firstOrNull()
                if (document != null) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("medicines")
                        .document(document.id)
                        .set(updatedMedicine)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Medicine updated", Toast.LENGTH_SHORT).show()
                            fetchMedicinesFromFirebase() // Refresh the data
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Delete medicine from Firebase Firestore
    private fun deleteMedicineFromFirebase(medicineName: String) {
        firestore.collection("users")
            .document(userId)
            .collection("medicines")
            .whereEqualTo("name", medicineName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val document = querySnapshot.documents.firstOrNull()
                if (document != null) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("medicines")
                        .document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show()
                            fetchMedicinesFromFirebase() // Refresh the data
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker(dateInput: TextInputEditText) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateInput.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    data class Medicine(val name: String, val count: Int, val expirationDate: String)
}
