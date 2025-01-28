package com.example.medilarm


import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText


class ShelfActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var medicineAdapter: MedicineAdapter
    private val medicineList: MutableList<Medicine> = mutableListOf()

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
                medicineAdapter.removeItem(position)
            },
            { medicine, position ->
                // Handle edit button click (show edit dialog)
                showEditMedicineDialog(medicine, position)
            }
        )
        recyclerView.adapter = medicineAdapter

        // Setup Add Button click listener
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

    // Function to show the add medicine dialog
    private fun showAddMedicineDialog() {
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null)

        // Initialize the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Medicine")
            .setView(dialogView)
            .setPositiveButton("Add") { dialogInterface: DialogInterface, _ ->
                // Get input values from dialog
                val name = dialogView.findViewById<TextInputEditText>(R.id.medicineNameInput).text.toString()
                val countString = dialogView.findViewById<TextInputEditText>(R.id.medicineCountInput).text.toString()

                // Validate input
                if (name.isNotEmpty() && countString.isNotEmpty()) {
                    val count = countString.toIntOrNull()
                    if (count != null) {
                        val newMedicine = Medicine(name, count)
                        medicineList.add(newMedicine)
                        medicineAdapter.notifyItemInserted(medicineList.size - 1)
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

    // Function to show the edit medicine dialog
    private fun showEditMedicineDialog(medicine: Medicine, position: Int) {
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null)

        // Pre-fill the dialog with current medicine data
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.medicineNameInput)
        val countInput = dialogView.findViewById<TextInputEditText>(R.id.medicineCountInput)
        nameInput.setText(medicine.name)
        countInput.setText(medicine.count.toString())

        // Initialize the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Medicine")
            .setView(dialogView)
            .setPositiveButton("Save") { dialogInterface: DialogInterface, _ ->
                // Get updated values from dialog
                val newName = nameInput.text.toString()
                val newCountString = countInput.text.toString()

                // Validate input
                if (newName.isNotEmpty() && newCountString.isNotEmpty()) {
                    val newCount = newCountString.toIntOrNull()
                    if (newCount != null) {
                        medicineList[position] = Medicine(newName, newCount)
                        medicineAdapter.notifyItemChanged(position)
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

    data class Medicine(val name: String, val count: Int)
}
