package com.example.medilarm

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileSettings : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fullNameTextView: TextView
    private lateinit var emailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        fullNameTextView = findViewById(R.id.FullName)
        emailTextView = findViewById(R.id.Email)

        fetchUserProfile()

        val btnReturn: ImageView = findViewById(R.id.btn_return)
        btnReturn.setOnClickListener { finish() }
    }

    private fun fetchUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fname = document.getString("fname") ?: ""
                        val lname = document.getString("lname") ?: ""
                        val email = document.getString("email") ?: currentUser.email

                        fullNameTextView.text = "$fname $lname"
                        emailTextView.text = email
                    } else {
                        Log.d("Firestore", "Document not found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error fetching document", e)
                }
        }
    }
}
